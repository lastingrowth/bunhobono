package api.feerule_p;

import jakarta.annotation.Resource;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class FeeRuleService {

    @Resource
    private FeeRuleMapper feeRuleMapper;

    // 전체 요금 규칙 목록 조회
    public List<FeeRuleDTO> list() {
        return feeRuleMapper.list(null, null);
    }

    // 요금 규칙 상세 조회
    public FeeRuleDTO detail(int feeRuleNo) {
        if (feeRuleNo <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
        }

        FeeRuleDTO dto = feeRuleMapper.detail(feeRuleNo);

        if (dto == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }

        return dto;
    }

    // 해당 시각에 자동 적용할 기본 요금 규칙 조회
    public FeeRuleDTO findDefaultAt(LocalDateTime activeAt) {
        if (activeAt == null) {
            activeAt = LocalDateTime.now();
        }

        List<FeeRuleDTO> list = feeRuleMapper.list(true, activeAt);

        if (list.size() != 1) {
            throw new ResponseStatusException(HttpStatus.CONFLICT);
        }

        return list.get(0);
    }

    // 새로운 규칙을 등록하거나 같은 이름의 기존 규칙을 상태에 맞게 저장
    @Transactional
    public int insert(FeeRuleDTO dto) {
        validateRuleValues(dto);

        if (dto.getIsDefault() == null) {
            dto.setIsDefault(false);
        }

        FeeRuleDTO feeRule = findLatestRule(dto.getRuleName());

        // 같은 이름이 없으면 새로운 규칙 등록
        if (feeRule == null) {
            validateDefaultRulePeriod(dto, null);

            try {
                return feeRuleMapper.insert(dto);
            } catch (DuplicateKeyException e) {
                throw new ResponseStatusException(HttpStatus.CONFLICT);
            }
        }

        LocalDateTime now = LocalDateTime.now();

        // 종료된 규칙은 변경하지 않고 같은 규칙의 다음 버전 등록
        if (feeRule.getEffectiveTo() != null
                && !feeRule.getEffectiveTo().isAfter(now)) {
            if (dto.getEffectiveFrom().isBefore(
                    feeRule.getEffectiveTo()
            )) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
            }

            dto.setRuleName(generateNextRuleName(feeRule));
            dto.setIsDefault(feeRule.getIsDefault());
            dto.setFeeRuleNo(null);

            validateDefaultRulePeriod(dto, null);

            try {
                return feeRuleMapper.insert(dto);
            } catch (DuplicateKeyException e) {
                throw new ResponseStatusException(HttpStatus.CONFLICT);
            }
        }

        // 같은 이름의 예약 규칙은 새 행을 만들지 않고 수정
        if (feeRule.getEffectiveFrom().isAfter(now)) {
            if (!dto.getEffectiveFrom().isAfter(now)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
            }

            dto.setFeeRuleNo(feeRule.getFeeRuleNo());
            dto.setRuleName(feeRule.getRuleName());

            validateDefaultRulePeriod(
                    dto,
                    feeRule.getFeeRuleNo()
            );

            try {
                return feeRuleMapper.update(dto);
            } catch (DuplicateKeyException e) {
                throw new ResponseStatusException(HttpStatus.CONFLICT);
            }
        }

        // 같은 이름의 활성 규칙은 입력한 시작시각부터 다음 버전 적용
        if (!dto.getEffectiveFrom().isAfter(now)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
        }

        return insertNextVersion(feeRule, dto);
    }

    // 상태에 따라 예약 규칙을 수정하거나 활성 규칙의 새 버전 등록
    @Transactional
    public int update(int feeRuleNo, FeeRuleDTO dto) {
        if (feeRuleNo <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
        }

        FeeRuleDTO feeRule = feeRuleMapper.detail(feeRuleNo);

        if (feeRule == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }

        validateRuleValues(dto);

        LocalDateTime now = LocalDateTime.now();

        if (feeRule.getEffectiveTo() != null && !feeRule.getEffectiveTo().isAfter(now)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT);
        }

        if (!dto.getEffectiveFrom().isAfter(now)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
        }

        if (dto.getIsDefault() == null) {
            dto.setIsDefault(feeRule.getIsDefault());
        }

        // 예약 규칙은 기존 행 수정
        if (feeRule.getEffectiveFrom().isAfter(now)) {
            dto.setFeeRuleNo(feeRuleNo);

            validateDefaultRulePeriod(dto, feeRuleNo);

            try {
                return feeRuleMapper.update(dto);
            } catch (DuplicateKeyException e) {
                throw new ResponseStatusException(HttpStatus.CONFLICT);
            }
        }

        // 활성 규칙은 변경값을 다음 버전으로 등록
        return insertNextVersion(feeRule, dto);
    }

    // 요금 계산과 적용 기간에 필요한 값 검증
    private void validateRuleValues(FeeRuleDTO dto) {
        if (dto == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
        }

        if (dto.getRuleName() == null
                || dto.getRuleName().isBlank()
                || dto.getUnitMinutes() <= 0
                || dto.getUnitFee() == null
                || dto.getUnitFee().compareTo(BigDecimal.ZERO) < 0
                || dto.getDailyMaxFee() != null
                && dto.getDailyMaxFee().compareTo(BigDecimal.ZERO) < 0
                || dto.getExitGraceMinutes() == null
                || dto.getExitGraceMinutes() < 0
                || dto.getEffectiveFrom() == null
                || dto.getEffectiveTo() != null
                && !dto.getEffectiveTo().isAfter(dto.getEffectiveFrom())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
        }

        dto.setRuleName(dto.getRuleName().trim());
    }

    // 두 요금 규칙의 적용 기간 중복 여부 확인
    private boolean isPeriodOverlapping(FeeRuleDTO dto, FeeRuleDTO feeRule) {
        return (feeRule.getEffectiveTo() == null
                || dto.getEffectiveFrom().isBefore(feeRule.getEffectiveTo()))
                && (dto.getEffectiveTo() == null
                || feeRule.getEffectiveFrom().isBefore(dto.getEffectiveTo()));
    }

    // 다른 기본 규칙과 적용 기간이 겹치는지 검증
    private void validateDefaultRulePeriod(
            FeeRuleDTO dto,
            Integer excludedFeeRuleNo
    ) {
        if (!Boolean.TRUE.equals(dto.getIsDefault())) {
            return;
        }

        List<FeeRuleDTO> list = feeRuleMapper.list(true, null);

        boolean overlapping = list.stream()
                .filter(feeRule -> excludedFeeRuleNo == null
                        || !excludedFeeRuleNo.equals(feeRule.getFeeRuleNo()))
                .anyMatch(feeRule -> isPeriodOverlapping(dto, feeRule));

        if (overlapping) {
            throw new ResponseStatusException(HttpStatus.CONFLICT);
        }
    }

    // 버전 번호를 제외한 요금 규칙의 기본 이름을 반환
    private String getBaseRuleName(String ruleName) {
        String versionText = " v.";
        int separator = ruleName.lastIndexOf(versionText);

        if (separator <= 0
                || separator + versionText.length() >= ruleName.length()) {
            return ruleName;
        }

        String version = ruleName.substring(
                separator + versionText.length()
        );

        if (!version.chars().allMatch(Character::isDigit)) {
            return ruleName;
        }

        return ruleName.substring(0, separator);
    }

    // 같은 기본 이름을 가진 규칙 중 가장 최근 버전 조회
    private FeeRuleDTO findLatestRule(String ruleName) {
        String baseName = getBaseRuleName(ruleName);
        List<FeeRuleDTO> list = feeRuleMapper.list(null, null);

        // 목록이 적용 시작일시 역순이므로 첫 번째 일치 항목이 최신 버전
        for (FeeRuleDTO feeRule : list) {
            if (baseName.equals(
                    getBaseRuleName(feeRule.getRuleName())
            )) {
                return feeRule;
            }
        }

        return null;
    }

    // UNIQUE 조건과 충돌하지 않는 다음 버전 이름 생성
    private String generateNextRuleName(FeeRuleDTO feeRule) {
        String baseName = getBaseRuleName(feeRule.getRuleName());
        List<FeeRuleDTO> list = feeRuleMapper.list(null, null);

        int version = 1;

        while (true) {
            String suffix = " v." + version;
            int maxLength = 100 - suffix.length();

            String name = baseName.length() > maxLength
                    ? baseName.substring(0, maxLength) + suffix
                    : baseName + suffix;

            boolean isExisting = false;

            for (FeeRuleDTO dto : list) {
                if (name.equals(dto.getRuleName())) {
                    isExisting = true;
                    break;
                }
            }

            if (!isExisting) {
                return name;
            }

            version++;
        }
    }

    // 활성 규칙을 종료하고 변경값을 다음 버전으로 등록
    private int insertNextVersion(FeeRuleDTO feeRule, FeeRuleDTO dto) {
        Integer feeRuleNo = feeRule.getFeeRuleNo();

        // 기본 이름이 같으면 같은 규칙의 다음 버전 이름을 자동 적용
        if (getBaseRuleName(dto.getRuleName()).equals(getBaseRuleName(feeRule.getRuleName()))) {
            dto.setRuleName(generateNextRuleName(feeRule));
        }

        // 활성 규칙의 기본 적용 여부를 다음 버전이 그대로 이어받음
        dto.setIsDefault(feeRule.getIsDefault());
        dto.setFeeRuleNo(null);

        // 기존 규칙 종료시각과 새 규칙 시작시각을 동일하게 연결
        feeRule.setEffectiveTo(dto.getEffectiveFrom());

        validateDefaultRulePeriod(feeRule, feeRuleNo);
        validateDefaultRulePeriod(dto, feeRuleNo);

        try {
            if (feeRuleMapper.update(feeRule) != 1 || feeRuleMapper.insert(dto) != 1) {
                throw new ResponseStatusException(HttpStatus.CONFLICT);
            }

            return 1;
        } catch (DuplicateKeyException e) {
            throw new ResponseStatusException(HttpStatus.CONFLICT);
        }
    }
}
