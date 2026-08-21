package api.predictive_maintenance_p;

import lombok.Data;

@Data
public class PdmActionRequestDTO {

    // 관리자가 조치 완료 시 선택적으로 남기는 점검 내용
    private String actionNote;
}
