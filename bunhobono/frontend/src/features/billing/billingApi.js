import api from "@/shared/api/apiClient";

// 차량번호 뒤 4자리로 현재 주차 중인 차량 목록 조회
export const getParkingCars = (lastFourDigits) => {
    return api.get('/billing/cars', {
        params: {
            lastFourDigits
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
export const confirmPayment = (
    paymentKey,
    orderId,
    amount
) => {
    return api.post('/billing/confirm', {
        paymentKey,
        orderId,
        amount
    })
}