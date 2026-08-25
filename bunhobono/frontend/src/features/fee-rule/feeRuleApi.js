import api from "@/shared/api/apiClient";

// 등록된 요금 규칙 목록 조회
export const getFeeRuleList = () => {
    return api.get('/fee-rules')
}

// 새로운 요금 규칙 등록
export const createFeeRule = (dto) => {
    return api.post('/fee-rules', dto)
}

// 예약 규칙을 수정하거나 활성 규칙의 새 버전 등록
export const updateFeeRule = (feeRuleNo, dto) => {
    return api.patch(`/fee-rules/${feeRuleNo}`, dto)
}