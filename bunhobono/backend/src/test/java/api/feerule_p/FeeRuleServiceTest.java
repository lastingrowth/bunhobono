package api.feerule_p;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FeeRuleServiceTest {
    @Mock FeeRuleMapper mapper;
    @InjectMocks FeeRuleService service;

    @Test @DisplayName("UT-BE-FEERULE-001 전체 요금 규칙 목록을 조회한다")
    void list() { when(mapper.list(null, null)).thenReturn(List.of(new FeeRuleDTO())); assertThat(service.list()).hasSize(1); }

    @Test @DisplayName("UT-BE-FEERULE-002 유효한 번호의 요금 규칙 상세를 조회한다")
    void detail() { FeeRuleDTO dto = new FeeRuleDTO(); when(mapper.detail(1)).thenReturn(dto); assertThat(service.detail(1)).isSameAs(dto); }

    @Test @DisplayName("UT-BE-FEERULE-003 요금 규칙 상세 번호가 0 이하면 거부한다")
    void detail_rejectsInvalidNumber() { assertStatus(org.springframework.http.HttpStatus.BAD_REQUEST,() -> service.detail(0)); verifyNoInteractions(mapper); }

    @Test @DisplayName("UT-BE-FEERULE-004 지정 시각의 기본 규칙이 정확히 한 건이면 반환한다")
    void defaultAt() { LocalDateTime at = LocalDateTime.now(); FeeRuleDTO dto = new FeeRuleDTO(); when(mapper.list(true, at)).thenReturn(List.of(dto)); assertThat(service.findDefaultAt(at)).isSameAs(dto); }

    @Test @DisplayName("UT-BE-FEERULE-005 기본 규칙이 없으면 충돌로 처리한다")
    void rejectAmbiguousDefault() { when(mapper.list(eq(true), any())).thenReturn(List.of()); assertThatThrownBy(() -> service.findDefaultAt(LocalDateTime.now())).isInstanceOf(ResponseStatusException.class); }

    @Test @DisplayName("UT-BE-FEERULE-006 새 이름의 유효한 요금 규칙을 등록한다")
    void insertNewRule() { FeeRuleDTO dto=validRule(); when(mapper.list(null,null)).thenReturn(List.of()); when(mapper.insert(dto)).thenReturn(1); assertThat(service.insert(dto)).isEqualTo(1); assertThat(dto.getRuleName()).isEqualTo("기본 요금"); assertThat(dto.getIsDefault()).isFalse(); }

    @Test @DisplayName("UT-BE-FEERULE-007 예약 요금 규칙은 기존 행을 수정한다")
    void updateScheduledRule() { FeeRuleDTO old=validRule(); old.setFeeRuleNo(3); old.setEffectiveFrom(LocalDateTime.now().plusDays(2)); FeeRuleDTO changed=validRule(); changed.setEffectiveFrom(LocalDateTime.now().plusDays(3)); when(mapper.detail(3)).thenReturn(old); when(mapper.update(changed)).thenReturn(1); assertThat(service.update(3,changed)).isEqualTo(1); assertThat(changed.getFeeRuleNo()).isEqualTo(3); }

    @Test @DisplayName("UT-BE-FEERULE-008 없는 요금 규칙 상세를 404로 처리한다") void detail_rejectsMissing(){ when(mapper.detail(2)).thenReturn(null); assertStatus(org.springframework.http.HttpStatus.NOT_FOUND,() -> service.detail(2)); }
    @Test @DisplayName("UT-BE-FEERULE-009 기본 규칙이 여러 건이면 충돌로 처리한다") void findDefaultAt_rejectsMultiple(){ when(mapper.list(true,LocalDateTime.MIN)).thenReturn(List.of(new FeeRuleDTO(),new FeeRuleDTO())); assertStatus(org.springframework.http.HttpStatus.CONFLICT,() -> service.findDefaultAt(LocalDateTime.MIN)); }
    @Test @DisplayName("UT-BE-FEERULE-010 조회시각이 null이면 현재 시각으로 기본 규칙을 조회한다") void findDefaultAt_usesNow(){ FeeRuleDTO rule=new FeeRuleDTO(); when(mapper.list(eq(true),any(LocalDateTime.class))).thenReturn(List.of(rule)); assertThat(service.findDefaultAt(null)).isSameAs(rule); }
    @Test @DisplayName("UT-BE-FEERULE-011 요금 규칙 필수값과 적용기간을 검증한다") void insert_rejectsInvalidValues(){ assertStatus(org.springframework.http.HttpStatus.BAD_REQUEST,() -> service.insert(null)); FeeRuleDTO zeroUnit=validRule(); zeroUnit.setUnitMinutes(0); assertStatus(org.springframework.http.HttpStatus.BAD_REQUEST,() -> service.insert(zeroUnit)); FeeRuleDTO negative=validRule(); negative.setUnitFee(java.math.BigDecimal.valueOf(-1)); assertStatus(org.springframework.http.HttpStatus.BAD_REQUEST,() -> service.insert(negative)); FeeRuleDTO invalidPeriod=validRule(); invalidPeriod.setEffectiveTo(invalidPeriod.getEffectiveFrom()); assertStatus(org.springframework.http.HttpStatus.BAD_REQUEST,() -> service.insert(invalidPeriod)); }
    @Test @DisplayName("UT-BE-FEERULE-012 새 규칙의 중복키 저장 실패를 충돌로 처리한다") void insert_rejectsDuplicate(){ FeeRuleDTO d=validRule(); when(mapper.list(null,null)).thenReturn(List.of()); doThrow(new org.springframework.dao.DuplicateKeyException("duplicate")).when(mapper).insert(d); assertStatus(org.springframework.http.HttpStatus.CONFLICT,() -> service.insert(d)); }
    @Test @DisplayName("UT-BE-FEERULE-013 겹치는 기본 요금 규칙 등록을 거부한다") void insert_rejectsOverlappingDefault(){ FeeRuleDTO d=validRule(); d.setIsDefault(true); FeeRuleDTO existing=validRule(); existing.setEffectiveFrom(LocalDateTime.now()); existing.setEffectiveTo(null); when(mapper.list(null,null)).thenReturn(List.of()); when(mapper.list(true,null)).thenReturn(List.of(existing)); assertStatus(org.springframework.http.HttpStatus.CONFLICT,() -> service.insert(d)); verify(mapper,never()).insert(any()); }
    @Test @DisplayName("UT-BE-FEERULE-014 종료된 규칙 이름은 다음 버전으로 등록한다") void insert_versionsEndedRule(){ FeeRuleDTO old=validRule(); old.setFeeRuleNo(1); old.setRuleName("기본 요금"); old.setEffectiveFrom(LocalDateTime.now().minusDays(2)); old.setEffectiveTo(LocalDateTime.now().minusDays(1)); old.setIsDefault(false); FeeRuleDTO next=validRule(); next.setEffectiveFrom(LocalDateTime.now().plusDays(1)); when(mapper.list(null,null)).thenReturn(List.of(old)); when(mapper.insert(next)).thenReturn(1); assertThat(service.insert(next)).isEqualTo(1); assertThat(next.getRuleName()).isEqualTo("기본 요금 v.1"); assertThat(next.getFeeRuleNo()).isNull(); }
    @Test @DisplayName("UT-BE-FEERULE-015 예약 규칙 등록 요청은 새 행 대신 기존 행을 수정한다") void insert_updatesScheduledRule(){ FeeRuleDTO scheduled=validRule(); scheduled.setRuleName("기본 요금"); scheduled.setFeeRuleNo(4); scheduled.setEffectiveFrom(LocalDateTime.now().plusDays(2)); FeeRuleDTO request=validRule(); request.setEffectiveFrom(LocalDateTime.now().plusDays(3)); when(mapper.list(null,null)).thenReturn(List.of(scheduled)); when(mapper.update(request)).thenReturn(1); assertThat(service.insert(request)).isEqualTo(1); assertThat(request.getFeeRuleNo()).isEqualTo(4); verify(mapper,never()).insert(any()); }
    @Test @DisplayName("UT-BE-FEERULE-016 활성 규칙 변경은 기존 규칙 종료와 다음 버전 등록을 함께 처리한다") void update_versionsActiveRule(){ FeeRuleDTO active=validRule(); active.setFeeRuleNo(5); active.setRuleName("기본 요금"); active.setEffectiveFrom(LocalDateTime.now().minusDays(1)); active.setIsDefault(false); FeeRuleDTO request=validRule(); request.setEffectiveFrom(LocalDateTime.now().plusDays(1)); when(mapper.detail(5)).thenReturn(active); when(mapper.list(null,null)).thenReturn(List.of(active)); when(mapper.update(active)).thenReturn(1); when(mapper.insert(request)).thenReturn(1); assertThat(service.update(5,request)).isEqualTo(1); assertThat(active.getEffectiveTo()).isEqualTo(request.getEffectiveFrom()); assertThat(request.getRuleName()).isEqualTo("기본 요금 v.1"); }
    @Test @DisplayName("UT-BE-FEERULE-017 수정 번호·대상·시작시각을 검증한다") void update_rejectsInvalidRequests(){ assertStatus(org.springframework.http.HttpStatus.BAD_REQUEST,() -> service.update(0,validRule())); when(mapper.detail(2)).thenReturn(null); assertStatus(org.springframework.http.HttpStatus.NOT_FOUND,() -> service.update(2,validRule())); FeeRuleDTO ended=validRule(); ended.setEffectiveFrom(LocalDateTime.now().minusDays(2)); ended.setEffectiveTo(LocalDateTime.now().minusDays(1)); when(mapper.detail(3)).thenReturn(ended); assertStatus(org.springframework.http.HttpStatus.CONFLICT,() -> service.update(3,validRule())); FeeRuleDTO active=validRule(); active.setEffectiveFrom(LocalDateTime.now().minusDays(1)); when(mapper.detail(4)).thenReturn(active); FeeRuleDTO past=validRule(); past.setEffectiveFrom(LocalDateTime.now().minusHours(1)); assertStatus(org.springframework.http.HttpStatus.BAD_REQUEST,() -> service.update(4,past)); }

    private void assertStatus(org.springframework.http.HttpStatus status,Runnable action){ ResponseStatusException e=catchThrowableOfType(action::run,ResponseStatusException.class); assertThat(e).isNotNull(); assertThat(e.getStatusCode()).isEqualTo(status); }

    private FeeRuleDTO validRule(){ FeeRuleDTO d=new FeeRuleDTO(); d.setRuleName(" 기본 요금 "); d.setUnitMinutes(10); d.setUnitFee(java.math.BigDecimal.valueOf(1000)); d.setExitGraceMinutes(10); d.setEffectiveFrom(LocalDateTime.now().plusDays(1)); d.setIsDefault(false); return d; }
}
