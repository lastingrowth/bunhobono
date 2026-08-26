import { defineStore } from "pinia";
import { ref } from "vue";
import { createFeeRule, getFeeRuleList, updateFeeRule } from "./feeRuleApi";

export const useFeeRuleStore = defineStore('fee-rule', () => {

    // 요금 규칙 목록
    const feeRuleList = ref([])

    const loading = ref(false)
    const saving = ref(false)
    const errorMessage = ref('')

    // 서버 상태값을 화면에 표시할 안내 문구로 변환한다.
    const saveErrorMessage = (error, isUpdate) => {
        const status = error.response?.status

        if (status === 409) {
            return isUpdate
                ? '이미 종료된 규칙이거나, 입력한 이름이 이미 사용 중이거나, 기본 규칙의 적용 기간이 겹쳐 수정할 수 없습니다.'
                : '기본 규칙의 적용 기간이 겹치거나 동일한 규칙이 동시에 저장되어 등록할 수 없습니다.'
        }

        if (status === 400) {
            return '입력값과 적용 시작·종료일시를 확인해 주세요.'
        }

        if (status === 404) {
            return '해당 요금 규칙을 찾을 수 없습니다.'
        }

        return isUpdate
            ? '요금 규칙을 수정하지 못했습니다.'
            : '요금 규칙을 등록하지 못했습니다.'
    }

    // 등록된 요금 규칙 목록 조회
    const loadFeeRuleList = async () => {
        loading.value = true
        errorMessage.value = ''

        try {
            const response = await getFeeRuleList()

            feeRuleList.value = Array.isArray(response.data)
                ? response.data
                : []

            return {
                success: true,
                feeRules: feeRuleList.value
            }
        } catch (error) {
            console.error('요금 규칙 목록 조회 실패', error)

            feeRuleList.value = []
            errorMessage.value =
                error.response?.data?.message
                || '요금 규칙 목록을 조회하지 못했습니다.'

            return {
                success: false,
                message: errorMessage.value
            }
        } finally {
            loading.value = false
        }
    }

    // 새로운 요금 규칙을 등록하고 목록을 다시 조회한다.
    const addFeeRule = async (dto) => {
        saving.value = true
        errorMessage.value = ''

        try {
            const response = await createFeeRule(dto)

            if (response.data !== 1) {
                errorMessage.value = '요금 규칙을 등록하지 못했습니다.'

                return {
                    success: false,
                    message: errorMessage.value
                }
            }

            await loadFeeRuleList()

            return {
                success: true,
                status: response.data
            }
        } catch (error) {
            console.error('요금 규칙 등록 실패', error)

            errorMessage.value = saveErrorMessage(error, false)

            return {
                success: false,
                message: errorMessage.value
            }
        } finally {
            saving.value = false
        }
    }

    // 예약 규칙을 수정하거나 활성 규칙의 새 버전을 등록하고 목록을 다시 조회한다.
    const saveFeeRule = async (feeRuleNo, dto) => {
        saving.value = true
        errorMessage.value = ''

        try {
            const response = await updateFeeRule(feeRuleNo, dto)

            if (response.data !== 1) {
                errorMessage.value = '요금 규칙을 수정하지 못했습니다.'

                return {
                    success: false,
                    message: errorMessage.value
                }
            }

            await loadFeeRuleList()

            return {
                success: true,
                status: response.data
            }
        } catch (error) {
            console.error('요금 규칙 수정 실패', error)

            errorMessage.value = saveErrorMessage(error, true)

            return {
                success: false,
                message: errorMessage.value
            }
        } finally {
            saving.value = false
        }
    }

    return {
        feeRuleList,
        loading,
        saving,
        errorMessage,
        loadFeeRuleList,
        addFeeRule,
        saveFeeRule
    }
})
