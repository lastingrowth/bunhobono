<template>
    <section class="billing-result-page">
        <div class="billing-result">
            <h1>결제를 완료하지 못했습니다</h1>

            <p>{{ errorMessage }}</p>

            <button
                type="button"
                @click="goToBilling"
            >
                정산 화면으로
            </button>
        </div>
    </section>
</template>

<script setup>
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'

const route = useRoute()
const router = useRouter()

// 토스페이먼츠가 실패 주소에 전달한 코드와 메시지를 화면에 표시한다.
const errorMessage = computed(() => {
    const code = String(route.query.code ?? '')
    const message = String(route.query.message ?? '')

    if (code === 'PAY_PROCESS_CANCELED') {
        return '결제가 취소되었습니다.'
    }

    if (message) {
        return message
    }

    return '결제 처리 중 오류가 발생했습니다.'
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
