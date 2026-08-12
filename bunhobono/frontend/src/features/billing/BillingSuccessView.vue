<template>
    <section class="billing-result-page">
        <div class="billing-result">
            <template v-if="processing">
                <h1>결제 승인 중</h1>
                <p>결제정보를 확인하고 있습니다.</p>
            </template>

            <template v-else-if="success">
                <h1>정산 완료</h1>
                <p>결제가 완료되었습니다.</p>
                <p>출차해주세요.</p>

                <button
                    type="button"
                    @click="goToBilling"
                >
                    처음으로
                </button>
            </template>

            <template v-else>
                <h1>결제 승인 실패</h1>
                <p>{{ errorMessage }}</p>

                <button
                    type="button"
                    @click="goToBilling"
                >
                    정산 화면으로
                </button>
            </template>
        </div>
    </section>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { confirmPayment } from './billingApi'

const route = useRoute()
const router = useRouter()

const processing = ref(true)
const success = ref(false)
const errorMessage = ref('')

// 토스페이먼츠가 성공 주소에 전달한 결제정보를 백엔드에서 승인한다.
onMounted(async () => {
    const paymentKey = String(
        route.query.paymentKey ?? ''
    )
    const orderId = String(
        route.query.orderId ?? ''
    )
    const amount = Number(route.query.amount)

    if (
        !paymentKey
        || !orderId
        || !Number.isFinite(amount)
        || amount <= 0
    ) {
        errorMessage.value =
            '결제 승인정보를 확인할 수 없습니다.'
        processing.value = false
        return
    }

    try {
        await confirmPayment(
            paymentKey,
            orderId,
            amount
        )

        success.value = true
    } catch (error) {
        console.error('토스페이먼츠 결제 승인 실패', error)

        errorMessage.value =
            error.response?.data?.message
            || '결제를 승인하지 못했습니다.'
    } finally {
        processing.value = false
    }
})

// 현재 키오스크 번호를 유지하고 차량번호 입력 화면으로 돌아간다.
const goToBilling = () => {
    router.replace({
        name: 'KioskBilling',
        query: {
            kioskNo: route.query.kioskNo
        }
    })
}
</script>

<style scoped src="./billingResult.css"></style>
