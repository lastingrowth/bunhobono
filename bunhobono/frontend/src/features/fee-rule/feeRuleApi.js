import api from "@/shared/api/apiClient";

// 등록된 요금 규칙 목록 조회
export const getFeeRuleList = () => {
    return api.get('/billing/admin/fee-rules')
}

// 새로운 요금 규칙 등록
export const createFeeRule = (data) => {
    return api.post('/billing/admin/fee-rules', data)
}

// 요금 규칙의 적용 종료일시 수정
export const updateFeeRuleEffectiveTo = (feeRuleNo, data) => {
    return api.patch(
        `/billing/admin/fee-rules/${feeRuleNo}/effective-to`,
        data
    )
}

// 예약 상태의 요금 규칙 전체 수정
export const updateScheduledFeeRule = (feeRuleNo, data) => {
    return api.patch(
        `/billing/admin/fee-rules/${feeRuleNo}`,
        data
    )
}