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

<style scoped>
.billing-result-page {
    box-sizing: border-box;
    min-height: 100vh;
    padding: 28px;
    display: flex;
    justify-content: center;
    align-items: center;
    color: #f8fafc;
    background:
        radial-gradient(
            circle at top right,
            rgba(30, 64, 175, 0.28),
            transparent 34%
        ),
        linear-gradient(
            145deg,
            #06152d 0%,
            #0b2346 52%,
            #07172f 100%
        );
    font-family: Pretendard, "Noto Sans KR", sans-serif;
}

.billing-result-page *,
.billing-result-page *::before,
.billing-result-page *::after {
    box-sizing: border-box;
}

.billing-result {
    width: min(620px, 100%);
    min-height: 420px;
    padding: 54px 42px;
    display: flex;
    flex-direction: column;
    justify-content: center;
    align-items: center;
    border: 1px solid rgba(148, 163, 184, 0.25);
    border-radius: 20px;
    background: rgba(7, 25, 54, 0.92);
    box-shadow: 0 22px 55px rgba(0, 0, 0, 0.28);
    text-align: center;
}

.billing-result h1 {
    margin: 0 0 24px;
    color: #fbbf24;
    font-size: clamp(34px, 5vw, 52px);
    letter-spacing: -0.04em;
}

.billing-result p {
    margin: 6px 0;
    color: #cbd5e1;
    font-size: clamp(18px, 2.4vw, 24px);
    line-height: 1.6;
}

.billing-result button {
    width: min(340px, 100%);
    min-height: 64px;
    margin-top: 38px;
    padding: 14px 24px;
    border: 0;
    border-radius: 12px;
    cursor: pointer;
    color: #172033;
    background: linear-gradient(135deg, #fbbf24, #f59e0b);
    box-shadow: 0 10px 24px rgba(245, 158, 11, 0.22);
    font-size: 21px;
    font-weight: 800;
}

.billing-result button:active {
    transform: translateY(2px);
}

@media (max-width: 650px) {
    .billing-result-page {
        padding: 16px;
    }

    .billing-result {
        min-height: 360px;
        padding: 38px 22px;
    }
}
</style>
