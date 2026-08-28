import { defineStore } from "pinia";
import { ref } from "vue";
import { archiveAdminBilling, calculateBill, getAdminBillingDetail, getAdminBillingList, getExitGateNo, getParkingCars, getRobotTask, requestResidentParkOut, updateAdminBilling } from "./billingApi";

export const useBillingStore = defineStore('billing', () => {
    
    const parkingCars = ref([])
    const selectedCar = ref(null)
    const bill = ref(null)
    const loading = ref(false)
    const errorMessage = ref('')

    // 입주민 차량의 로봇 출차 작업정보
    const residentExitTask = ref(null)

    // 관리자 정산 목록
    const adminBillingList = ref([])

    // 관리자 정산 상세정보
    const adminBillingDetail = ref(null)

    // 출차 유형, 차량번호 뒤 4자리, 키오스크 번호로 현재 주차 차량을 조회한다.
    const searchCars = async (lastFourDigits, exitType, kioskNo) => {
        loading.value = true
        errorMessage.value = ''
        parkingCars.value = []
        selectedCar.value = null
        bill.value = null

        try {
            const response = await getParkingCars(lastFourDigits, exitType, kioskNo)

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

    // 출차 유형에 따라 차량만 선택하거나 주차요금까지 조회한다.
    const selectCar = async (
        car,
        kioskNo,
        exitType
    ) => {
        selectedCar.value = car
        bill.value = null
        errorMessage.value = ''

        // 입주민 차량은 정산하지 않으므로 선택한 차량정보만 유지한다.
        if (exitType === 'RESIDENT') {
            return {
                success: true,
                car
            }
        }

        // 비입주민 차량은 선택 직후 기존 정산금액을 조회한다.
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

            const status = error.response?.status
            const serverMessage = error.response?.data?.message
            const failureDetail = status
                ? `HTTP ${status}`
                : '네트워크 오류'

            errorMessage.value = serverMessage
                || `현재 주차요금을 조회하지 못했습니다. (${failureDetail})`

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
        residentExitTask.value = null
        errorMessage.value = ''
    }

    // 차량 선택 화면에서 뒤 4자리 입력 화면으로 이동
    const backToSearch = () => {
        parkingCars.value = []
        selectedCar.value = null
        bill.value = null
        residentExitTask.value = null
        errorMessage.value = ''
    }

    // 새로운 차량을 정산할 때 기존 조회 결과 초기화
    const reset = () => {
        parkingCars.value = []
        selectedCar.value = null
        bill.value = null
        residentExitTask.value = null
        errorMessage.value = ''
    }

    // 현재 주차 중인 비입주민 차량의 관리자 정산 목록 조회
    const loadAdminBillingList = async (showLoading = true) => {
        if (showLoading) {
            loading.value = true
        }

        errorMessage.value = ''

        try {
            const response = await getAdminBillingList()

            adminBillingList.value = Array.isArray(response.data)
                ? response.data
                : []

            return {
                success: true,
                billingList: adminBillingList.value
            }
        } catch (error) {
            console.error('관리자 정산 목록 조회 실패', error)

            adminBillingList.value = []
            errorMessage.value = error.response?.data?.message || '정산 목록을 조회하지 못했습니다.'

            return {
                success: false,
                message: errorMessage.value
            }
        } finally {
            if (showLoading) {
                loading.value = false
            }
        }
    }

    // 정산서 번호로 관리자 정산 상세정보 조회
    const loadAdminBillingDetail = async (billNo, showLoading = true) => {
        if (showLoading) {
            loading.value = true
            adminBillingDetail.value = null
        }

        errorMessage.value = ''

        try {
            const response = await getAdminBillingDetail(billNo)

            adminBillingDetail.value = response.data

            return {
                success: true,
                billingDetail: adminBillingDetail.value
            }
        } catch (error) {
            console.error('관리자 정산 상세 조회 실패', error)

            errorMessage.value =
                error.response?.data?.message
                || '정산 상세정보를 조회하지 못했습니다.'

            return {
                success: false,
                message: errorMessage.value
            }
        } finally {
            if (showLoading) {
                loading.value = false
            }
        }
    }

    // 미결제 정산서의 무료시간과 요금 규칙을 수정하고 재계산된 상세정보를 저장한다.
    const saveAdminBilling = async (billNo, dto) => {
        loading.value = true
        errorMessage.value = ''

        try {
            const response = await updateAdminBilling(billNo, dto)

            adminBillingDetail.value = response.data

            return {
                success: true,
                billingDetail: adminBillingDetail.value
            }
        } catch (error) {
            console.error('관리자 정산 수정 실패', error)

            errorMessage.value =
                error.response?.data?.message
                || '정산정보를 수정하지 못했습니다.'

            return {
                success: false,
                message: errorMessage.value
            }
        } finally {
            loading.value = false
        }
    }

        // 출차 완료된 정산서를 지난 기록으로 직접 이동한다.
    const moveAdminBillingToTrash = async (billNo) => {
        loading.value = true
        errorMessage.value = ''

        try {
            await archiveAdminBilling(billNo)

            adminBillingList.value =
                adminBillingList.value.filter(
                    (billing) => billing.billNo !== billNo
                )

            return {
                success: true
            }
        } catch (error) {
            console.error('완료 정산서 지난 기록 이동 실패', error)

            // 삭제 실패 문구는 목록 오류 상태에 저장하지 않고 화면으로 반환한다.
            return {
                success: false,
                message:
                    error.response?.data?.message
                    || '정산 내역을 삭제할 수 없습니다.'
            }
        } finally {
            loading.value = false
        }
    }

    // 입주민 차량과 같은 층의 출차 게이트를 찾아 로봇 출차를 요청한다.
    const requestResidentExit = async (kioskNo) => {
        if (!selectedCar.value) {
            errorMessage.value = '출차할 차량정보를 확인할 수 없습니다.'
            return {
                success: false,
                message: errorMessage.value
            }
        }

        loading.value = true
        errorMessage.value = ''

        try {
            // 백엔드에서 차량과 같은 층의 출차 게이트 번호를 조회한다.
            const gateResponse = await getExitGateNo(
                selectedCar.value.carLogNo,
                kioskNo
            )

            // 조회된 출차 게이트로 입주민 차량의 로봇 출차를 요청한다.
            const response = await requestResidentParkOut(
                selectedCar.value.carLogNo,
                gateResponse.data
            )

            // 생성된 로봇 출차 작업정보를 상태 확인에 사용한다.
            residentExitTask.value = response.data

            return {
                success: true,
                task: residentExitTask.value
            }
        } catch (error) {
            console.error('입주민 차량 출차 요청 실패', error)

            errorMessage.value =
                error.response?.data?.message
                || error.message
                || '입주민 차량 출차를 요청하지 못했습니다.'

            return {
                success: false,
                message: errorMessage.value
            }
        } finally {
            loading.value = false
        }
    }

    // 저장된 로봇 출차 작업 번호로 현재 진행 상태를 조회한다.
    const loadResidentExitTask = async () => {
        const taskNo = residentExitTask.value?.taskNo

        if (!taskNo) {
            errorMessage.value =
                '로봇 출차 작업정보를 확인할 수 없습니다.'

            return {
                success: false,
                message: errorMessage.value
            }
        }

        try {
            const response = await getRobotTask(taskNo)

            residentExitTask.value = response.data

            return {
                success: true,
                task: residentExitTask.value
            }
        } catch (error) {
            console.error('로봇 출차 작업 조회 실패', error)

            errorMessage.value =
                error.response?.data?.message
                || '로봇 출차 작업 상태를 조회하지 못했습니다.'

            return {
                success: false,
                message: errorMessage.value
            }
        }
    }

    return {
        parkingCars,
        selectedCar,
        bill,
        loading,
        errorMessage,
        residentExitTask,
        adminBillingList,
        adminBillingDetail,
        searchCars,
        selectCar,
        loadAdminBillingList,
        loadAdminBillingDetail,
        saveAdminBilling,
        moveAdminBillingToTrash,
        loadResidentExitTask,
        requestResidentExit,
        backToCarList,
        backToSearch,
        reset,
    }
})
