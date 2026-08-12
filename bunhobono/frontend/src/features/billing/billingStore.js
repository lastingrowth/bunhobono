import { defineStore } from "pinia";
import { ref } from "vue";
import { calculateBill, getParkingCars } from "./billingApi";

export const useBillingStore = defineStore('billing', () => {
    
    const parkingCars = ref([])
    const selectedCar = ref(null)
    const bill = ref(null)
    const loading = ref(false)
    const errorMessage = ref('')

    // 차량번호 뒤 4자리로 현재 주차 중인 차량 목록 조회
    const searchCars = async (lastFourDigits) => {
        loading.value = true
        errorMessage.value = ''
        parkingCars.value = []
        selectedCar.value = null
        bill.value = null

        try {
            const response = await getParkingCars(lastFourDigits)
            parkingCars.value = Array.isArray(response.data)
                ? response.data
                : []

            return {
                success: true,
                cars: parkingCars.value
            }
        } catch (error) {
            console.error('주차 차량 조회 실패', error)

            errorMessage.value
                = error.response?.data?.message || '일치하는 주차 차량을 조회하지 못했습니다.'

            return {
                success: false,
                message: errorMessage.value
            }
        } finally {
            loading.value = false
        }
    }

    // 후보 목록에서 선택한 차량의 주차요금 조회
    const selectCar = async (car, kioskNo) => {
        selectedCar.value = car

        const result = await calculate(
            car.carNo,
            kioskNo
        )

        if (!result.success) {
            selectedCar.value = null
        }

        return result
    }

    // 차량번호로 현재 주차요금 조회
    const calculate = async (carNo, kioskNo) => {
        loading.value = true
        errorMessage.value = ''
        bill.value = null

        try {
            const response = await calculateBill(carNo, kioskNo)
            bill.value = response.data
            return { success: true, bill: response.data}
        } catch (error) {
            console.error('주차요금 조회 실패', error)

            errorMessage.value =
                error.response?.data?.message || '현재 주차요금을 조회하지 못했습니다.'

            return {
                success: false,
                message: errorMessage.value
            }
        } finally {
            loading.value = false
        }
    }

    // 요금 확인 화면에서 차량 선택 화면으로 이동
    const backToCarList = () => {
        selectedCar.value = null
        bill.value = null
        errorMessage.value = ''
    }

    // 차량 선택 화면에서 뒤 4자리 입력 화면으로 이동
    const backToSearch = () => {
        parkingCars.value = []
        selectedCar.value = null
        bill.value = null
        errorMessage.value = ''
    }

    // 새로운 차량을 정산할 때 기존 조회 결과 초기화
    const reset = () => {
        parkingCars.value = []
        selectedCar.value = null
        bill.value = null
        errorMessage.value = ''
    }

    return {
        parkingCars,
        selectedCar,
        bill,
        loading,
        errorMessage,
        searchCars,
        selectCar,
        backToCarList,
        backToSearch,
        reset,
    }
})