import api from "@/shared/api/apiClient";

// 출차 유형, 차량번호 뒤 4자리, 키오스크 번호로 현재 주차 차량을 조회한다.
export const getParkingCars = (lastFourDigits, exitType, kioskNo) => {
    return api.get('/billing/cars', {
        params: {
            lastFourDigits,
            exitType,
            kioskNo
        }
    })
}

// 차량번호와 키오스크 번호로 현재 주차요금 조회
export const calculateBill = (carNo, kioskNo) => {
    return api.post('/billing/calculate', {
        carNo,
        kioskNo
    })
}

// 토스페이먼츠 결제를 승인하고 정산을 완료 처리
export const confirmPayment = (paymentKey, orderId, amount) => {
    return api.post('/billing/confirm', {
        paymentKey,
        orderId,
        amount
    })
}

// 현재 주차 중인 비입주민 차량의 관리자 정산 목록 조회
export const getAdminBillingList = () => {
    return api.get('/billing/admin')
}

// 입출차 기록 번호로 관리자 정산 상세정보 조회
export const getAdminBillingDetail = (carLogNo) => {
    return api.get(`/billing/admin/${carLogNo}`)
}

// 미결제 정산의 무료시간 수정 및 정산금액 재계산
export const updateAdminBilling = (carLogNo, freeTime) => {
    return api.patch(`/billing/admin/${carLogNo}`, {freeTime})
}

// 완료 정산서를 지난 기록으로 직접 이동
export const archiveAdminBilling = (billNo) => {
    return api.delete(`/billing/admin/${billNo}/archive`)
}

// 차량이 입차한 주차장과 같은 층의 활성 출차 게이트 번호 조회
export const getExitGateNo = (carLogNo) => {
    return api.get(`/billing/cars/${carLogNo}/exit-gate`)
}

// 입주민 차량을 지정한 B1 출차 게이트의 대기면으로 이동하도록 요청
export const requestResidentParkOut = (carLogNo, exitGateNo) => {
    return api.post('/robot-tasks/park-out', null, {
        params: {
            carLogNo,
            exitGateNo
        }
    })
}

// 로봇 출차 작업의 현재 진행 상태 조회
export const getRobotTask = (taskNo) => {
    return api.get(`/robot-tasks/${taskNo}`)
}