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