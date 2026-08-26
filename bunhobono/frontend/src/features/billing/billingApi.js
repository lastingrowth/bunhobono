import api from "@/shared/api/apiClient";

// 출차 유형에 따라 입주민 입출차 기록 또는 비입주민 정산서를 조회한다.
export const getParkingCars = (lastFourDigits, exitType, kioskNo) => {
    const url = exitType === 'RESIDENT'
        ? '/carlog/parking-cars'
        : '/billing/guest-cars'

    return api.get(url, {
        params: {
            lastFourDigits,
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

// 로그인한 입주민이 등록한 방문차량의 주차요금 고지서 조회
export const getResidentBill = (billNo) => {
    return api.get(`/billing/resident/${billNo}`)
}

// 현재 주차 중인 비입주민 차량의 관리자 정산 목록 조회
export const getAdminBillingList = () => {
    return api.get('/billing/admin')
}

// 정산서 번호로 관리자 정산 상세정보 조회
export const getAdminBillingDetail = (billNo) => {
    return api.get(`/billing/admin/${billNo}`)
}

// 미결제 정산서의 무료시간과 요금 규칙 수정 및 정산금액 재계산
export const updateAdminBilling = (billNo, dto) => {
    return api.patch(`/billing/admin/${billNo}`, dto)
}

// 완료 정산서를 지난 기록으로 직접 이동
export const archiveAdminBilling = (billNo) => {
    return api.delete(`/billing/admin/${billNo}/archive`)
}

// 입출차 기록과 현재 키오스크 위치에 맞는 활성 출차 게이트 번호 조회
export const getExitGateNo = (carLogNo, kioskNo) => {
    return api.get(`/gates/${carLogNo}/exit-gate`, {
        params: {
            kioskNo
        }
    })
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
