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
