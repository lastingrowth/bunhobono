import { defineStore } from "pinia";
import { ref } from "vue";
import { createFeeRule, getFeeRuleList, updateFeeRuleEffectiveTo, updateScheduledFeeRule } from "./feeRuleApi";

export const useFeeRuleStore = defineStore('fee-rule', () => {

    // 요금 규칙 목록
    const feeRuleList = ref([])

    const loading = ref(false)
    const saving = ref(false)
    const errorMessage = ref('')

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
    const addFeeRule = async (data) => {
        saving.value = true
        errorMessage.value = ''

        try {
            const response = await createFeeRule(data)

            await loadFeeRuleList()

            return {
                success: true,
                feeRule: response.data
            }
        } catch (error) {
            console.error('요금 규칙 등록 실패', error)

            errorMessage.value =
                error.response?.data?.message
                || '요금 규칙을 등록하지 못했습니다.'

            return {
                success: false,
                message: errorMessage.value
            }
        } finally {
            saving.value = false
        }
    }

    // 요금 규칙의 적용 종료일시를 수정하고 목록을 다시 조회한다.
    const saveFeeRuleEffectiveTo = async (
        feeRuleNo,
        effectiveTo
    ) => {
        saving.value = true
        errorMessage.value = ''

        try {
            const response = await updateFeeRuleEffectiveTo(
                feeRuleNo,
                {
                    effectiveTo
                }
            )

            await loadFeeRuleList()

            return {
                success: true,
                feeRule: response.data
            }
        } catch (error) {
            console.error(
                '요금 규칙 적용 종료일시 수정 실패',
                error
            )

            errorMessage.value =
                error.response?.data?.message
                || '요금 규칙의 적용 종료일시를 수정하지 못했습니다.'

            return {
                success: false,
                message: errorMessage.value
            }
        } finally {
            saving.value = false
        }
    }

        // 예약 상태의 요금 규칙을 수정하고 목록을 다시 조회한다.
    const saveScheduledFeeRule = async (
        feeRuleNo,
        data
    ) => {
        saving.value = true
        errorMessage.value = ''

        try {
            const response = await updateScheduledFeeRule(
                feeRuleNo,
                data
            )

            await loadFeeRuleList()

            return {
                success: true,
                feeRule: response.data
            }
        } catch (error) {
            console.error(
                '예약 요금 규칙 수정 실패',
                error
            )

            errorMessage.value =
                error.response?.data?.message
                || '예약 요금 규칙을 수정하지 못했습니다.'

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
        saveFeeRuleEffectiveTo,
        saveScheduledFeeRule
    }
})