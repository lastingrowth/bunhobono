<template>
    <section class="billing-page">
        <header class="billing-header">
            <strong>BONO 아파트</strong>
            <h1>주차요금 정산</h1>
        </header>

        <!-- 1단계: 차량번호 뒤 4자리 입력 -->
        <div
            v-if="
                billingStore.parkingCars.length === 0
                && !billingStore.selectedCar
            "
            class="billing-step billing-search-step"
        >
            <h2>차량번호 뒤 4자리 입력</h2>

            <div class="billing-number-display">
                {{ lastFourDigits.padEnd(4, '_') }}
            </div>

            <div class="billing-keypad">
                <button
                    v-for="number in keypadNumbers"
                    :key="number"
                    type="button"
                    @click="appendNumber(number)"
                >
                    {{ number }}
                </button>

                <button
                    type="button"
                    @click="clearNumber"
                >
                    초기화
                </button>

                <button
                    type="button"
                    @click="appendNumber(0)"
                >
                    0
                </button>

                <button
                    type="button"
                    @click="deleteNumber"
                >
                    삭제
                </button>
            </div>

            <p
                v-if="billingStore.errorMessage"
                class="billing-error"
            >
                {{ billingStore.errorMessage }}
            </p>

            <button
                type="button"
                class="billing-primary-button"
                :disabled="
                    lastFourDigits.length !== 4
                    || billingStore.loading
                "
                @click="searchCars"
            >
                {{ billingStore.loading ? '조회 중' : '차량 조회' }}
            </button>
        </div>

        <!-- 2단계: 뒤 4자리가 일치하는 차량 선택 -->
        <div
            v-else-if="
                billingStore.parkingCars.length > 0
                && !billingStore.selectedCar
            "
            class="billing-step billing-car-list-step"
        >
            <h2>내 차량을 선택해주세요</h2>

            <p>
                검색된 차량
                {{ billingStore.parkingCars.length }}대
            </p>

            <div class="billing-car-list">
                <article
                    v-for="car in billingStore.parkingCars"
                    :key="car.carLogNo"
                    class="billing-car-card"
                >
                    <img
                        :src="getCarImageUrl(car.cameraDataNo)"
                        :alt="`${car.carNo} 입차 차량 이미지`"
                    />

                    <div class="billing-car-info">
                        <strong>{{ car.carNo }}</strong>
                        <span>
                            입차시각
                            {{ formatDateTime(car.inTime) }}
                        </span>
                    </div>

                    <button
                        type="button"
                        :disabled="billingStore.loading"
                        @click="selectCar(car)"
                    >
                        선택
                    </button>
                </article>
            </div>

            <p
                v-if="billingStore.errorMessage"
                class="billing-error"
            >
                {{ billingStore.errorMessage }}
            </p>

            <button
                type="button"
                class="billing-secondary-button"
                @click="backToSearch"
            >
                이전
            </button>
        </div>

        <!-- 3단계: 선택한 차량과 정산금액 확인 -->
        <div
            v-else-if="
                billingStore.selectedCar
                && billingStore.bill
            "
            class="billing-step billing-detail-step"
        >
            <div class="billing-selected-image">
                <img
                    :src="
                        getCarImageUrl(
                            billingStore.selectedCar.cameraDataNo
                        )
                    "
                    :alt="
                        `${billingStore.selectedCar.carNo}
                        선택 차량 이미지`
                    "
                />
            </div>

            <div class="billing-detail">
                <h2>차량정보 및 정산금액</h2>

                <dl>
                    <div>
                        <dt>차량번호</dt>
                        <dd>{{ billingStore.bill.carNo }}</dd>
                    </div>

                    <div>
                        <dt>입차시각</dt>
                        <dd>
                            {{
                                formatDateTime(
                                    billingStore.bill.inTime
                                )
                            }}
                        </dd>
                    </div>

                    <div>
                        <dt>주차시간</dt>
                        <dd>
                            {{
                                formatParkingTime(
                                    billingStore.bill.inTime
                                )
                            }}
                        </dd>
                    </div>

                    <div>
                        <dt>무료시간</dt>
                        <dd>
                            {{
                                formatMinutes(
                                    billingStore.bill.freeTime
                                )
                            }}
                        </dd>
                    </div>

                    <div class="billing-amount">
                        <dt>정산금액</dt>
                        <dd>
                            {{
                                formatAmount(
                                    billingStore.bill.billAmount
                                )
                            }}
                        </dd>
                    </div>
                </dl>

                <p
                    v-if="
                        billingStore.bill.billStatus === 'PAID'
                    "
                    class="billing-complete"
                >
                    정산이 완료되었습니다. 출차해주세요.
                </p>

                <div class="billing-detail-actions">
                    <button
                        type="button"
                        class="billing-secondary-button"
                        @click="billingStore.backToCarList"
                    >
                        이전
                    </button>

                    <button
                        v-if="
                            billingStore.bill.billStatus !== 'PAID'
                        "
                        type="button"
                        class="billing-primary-button"
                        disabled
                    >
                        결제하기
                    </button>

                    <button
                        v-else
                        type="button"
                        class="billing-primary-button"
                        @click="resetBilling"
                    >
                        처음으로
                    </button>
                </div>
            </div>
        </div>
    </section>
</template>

<script setup>
import { ref } from 'vue'
import { useRoute } from 'vue-router'
import { useBillingStore } from './billingStore'

const route = useRoute()
const billingStore = useBillingStore()

const lastFourDigits = ref('')
const keypadNumbers = [1, 2, 3, 4, 5, 6, 7, 8, 9]

// 주소의 kioskNo를 현재 정산 키오스크 번호로 사용한다.
const kioskNo = Number(route.query.kioskNo)

// 숫자 키패드에서 누른 번호를 입력한다.
const appendNumber = (number) => {
    if (lastFourDigits.value.length >= 4) {
        return
    }

    lastFourDigits.value += String(number)
    billingStore.errorMessage = ''
}

// 입력한 차량번호를 한 자리 삭제한다.
const deleteNumber = () => {
    lastFourDigits.value =
        lastFourDigits.value.slice(0, -1)

    billingStore.errorMessage = ''
}

// 입력한 차량번호를 모두 삭제한다.
const clearNumber = () => {
    lastFourDigits.value = ''
    billingStore.errorMessage = ''
}

// 차량번호 뒤 4자리로 현재 주차 차량을 조회한다.
const searchCars = async () => {
    if (lastFourDigits.value.length !== 4) {
        billingStore.errorMessage =
            '차량번호 뒤 4자리를 입력해주세요.'
        return
    }

    await billingStore.searchCars(
        lastFourDigits.value
    )
}

// 후보 목록에서 선택한 차량의 정산금액을 조회한다.
const selectCar = async (car) => {
    if (!Number.isInteger(kioskNo) || kioskNo <= 0) {
        billingStore.errorMessage =
            '키오스크 정보를 확인할 수 없습니다.'
        return
    }

    await billingStore.selectCar(
        car,
        kioskNo
    )
}

// 차량 선택 화면에서 번호 입력 화면으로 이동한다.
const backToSearch = () => {
    lastFourDigits.value = ''
    billingStore.backToSearch()
}

// 정산 상태를 초기화하고 첫 화면으로 이동한다.
const resetBilling = () => {
    lastFourDigits.value = ''
    billingStore.reset()
}

// 입차 차량의 전체 이미지 주소를 생성한다.
const getCarImageUrl = (cameraDataNo) => {
    if (!cameraDataNo) {
        return ''
    }

    return `${import.meta.env.VITE_API_URL}/camera-data/${cameraDataNo}/image`
}

// 날짜와 시간을 화면 표시 형식으로 변환한다.
const formatDateTime = (value) => {
    if (!value) {
        return '-'
    }

    return new Date(value).toLocaleString('ko-KR')
}

// 입차시각부터 현재까지의 전체 주차시간을 계산한다.
const formatParkingTime = (inTime) => {
    if (!inTime) {
        return '-'
    }

    const milliseconds =
        Date.now() - new Date(inTime).getTime()

    const minutes =
        Math.max(
            0,
            Math.ceil(milliseconds / 60000)
        )

    return formatMinutes(minutes)
}

// 분 단위 시간을 시간과 분으로 표시한다.
const formatMinutes = (value) => {
    const minutes = Number(value ?? 0)
    const hours = Math.floor(minutes / 60)
    const remainingMinutes = minutes % 60

    if (hours === 0) {
        return `${remainingMinutes}분`
    }

    if (remainingMinutes === 0) {
        return `${hours}시간`
    }

    return `${hours}시간 ${remainingMinutes}분`
}

// 정산금액을 원화 형식으로 표시한다.
const formatAmount = (value) => {
    return `${Number(value ?? 0).toLocaleString('ko-KR')}원`
}
</script>

<style scoped>
.billing-page {
    box-sizing: border-box;
    min-height: 100vh;
    padding: 28px;
    color: #f8fafc;
    background:
        radial-gradient(circle at top right, rgba(30, 64, 175, 0.28), transparent 34%),
        linear-gradient(145deg, #06152d 0%, #0b2346 52%, #07172f 100%);
    font-family: Pretendard, "Noto Sans KR", sans-serif;
}

.billing-page *,
.billing-page *::before,
.billing-page *::after {
    box-sizing: border-box;
}

.billing-header {
    width: min(1180px, 100%);
    min-height: 76px;
    margin: 0 auto 24px;
    padding: 0 28px;
    display: grid;
    grid-template-columns: 1fr auto 1fr;
    align-items: center;
    border: 1px solid rgba(148, 163, 184, 0.25);
    border-radius: 16px;
    background: rgba(3, 15, 35, 0.7);
    box-shadow: 0 18px 45px rgba(0, 0, 0, 0.24);
}

.billing-header strong {
    color: #fbbf24;
    font-size: 20px;
}

.billing-header h1 {
    margin: 0;
    font-size: clamp(25px, 3vw, 38px);
    letter-spacing: -0.04em;
}

.billing-step {
    width: min(1180px, 100%);
    min-height: calc(100vh - 156px);
    margin: 0 auto;
    padding: 34px;
    border: 1px solid rgba(148, 163, 184, 0.25);
    border-radius: 20px;
    background: rgba(7, 25, 54, 0.88);
    box-shadow: 0 22px 55px rgba(0, 0, 0, 0.28);
}

.billing-step h2 {
    margin: 0 0 12px;
    text-align: center;
    font-size: clamp(28px, 3.4vw, 44px);
    letter-spacing: -0.04em;
}

.billing-step > p {
    margin: 0 0 24px;
    color: #cbd5e1;
    text-align: center;
    font-size: 18px;
}

.billing-search-step {
    max-width: 760px;
    display: flex;
    flex-direction: column;
    justify-content: center;
}

.billing-number-display {
    min-height: 112px;
    margin: 22px 0;
    display: flex;
    justify-content: center;
    align-items: center;
    border: 2px solid #64748b;
    border-radius: 16px;
    background: rgba(2, 10, 25, 0.7);
    color: #fff;
    font-size: clamp(58px, 9vw, 92px);
    font-weight: 800;
    letter-spacing: 0.18em;
    line-height: 1;
    text-indent: 0.18em;
}

.billing-keypad {
    display: grid;
    grid-template-columns: repeat(3, 1fr);
    gap: 12px;
}

.billing-keypad button {
    min-height: 74px;
    border: 1px solid #64748b;
    border-radius: 12px;
    cursor: pointer;
    color: #fff;
    background: linear-gradient(180deg, #213b61, #142b4d);
    font-size: 30px;
    font-weight: 800;
}

.billing-keypad button:active {
    transform: translateY(2px);
    background: #102541;
}

.billing-primary-button,
.billing-secondary-button,
.billing-car-card button {
    min-height: 58px;
    padding: 12px 24px;
    border: 0;
    border-radius: 12px;
    cursor: pointer;
    font-size: 20px;
    font-weight: 800;
}

.billing-primary-button,
.billing-car-card button {
    color: #172033;
    background: linear-gradient(135deg, #fbbf24, #f59e0b);
    box-shadow: 0 10px 24px rgba(245, 158, 11, 0.22);
}

.billing-primary-button {
    width: 100%;
    margin-top: 18px;
}

.billing-secondary-button {
    color: #f8fafc;
    background: #233a5d;
}

.billing-primary-button:disabled,
.billing-car-card button:disabled {
    cursor: wait;
    opacity: 0.55;
}

.billing-error {
    margin: 18px 0 0 !important;
    color: #fca5a5 !important;
    font-weight: 700;
}

.billing-car-list-step {
    display: flex;
    flex-direction: column;
}

.billing-car-list {
    display: grid;
    gap: 14px;
}

.billing-car-card {
    min-height: 150px;
    padding: 12px;
    display: grid;
    grid-template-columns: minmax(220px, 34%) 1fr 130px;
    align-items: center;
    gap: 22px;
    border: 1px solid #3e587d;
    border-radius: 16px;
    background: rgba(13, 38, 76, 0.92);
}

.billing-car-card img {
    width: 100%;
    height: 126px;
    display: block;
    border-radius: 11px;
    object-fit: cover;
    background: #020617;
}

.billing-car-info {
    min-width: 0;
    display: flex;
    flex-direction: column;
    gap: 14px;
}

.billing-car-info strong {
    font-size: clamp(27px, 3vw, 39px);
}

.billing-car-info span {
    color: #cbd5e1;
    font-size: 18px;
}

.billing-car-list-step > .billing-secondary-button {
    align-self: flex-start;
    min-width: 180px;
    margin-top: auto;
}

.billing-detail-step {
    display: grid;
    grid-template-columns: minmax(0, 1.08fr) minmax(380px, 0.92fr);
    align-items: stretch;
    gap: 36px;
}

.billing-selected-image {
    min-height: 480px;
    overflow: hidden;
    border: 1px solid #3e587d;
    border-radius: 18px;
    background: #020617;
}

.billing-selected-image img {
    width: 100%;
    height: 100%;
    min-height: 480px;
    display: block;
    object-fit: cover;
}

.billing-detail {
    display: flex;
    flex-direction: column;
}

.billing-detail h2 {
    margin-bottom: 28px;
    text-align: left;
}

.billing-detail dl {
    margin: 0;
}

.billing-detail dl > div {
    padding: 16px 0;
    display: grid;
    grid-template-columns: 130px 1fr;
    align-items: center;
    gap: 18px;
    border-bottom: 1px solid rgba(148, 163, 184, 0.22);
}

.billing-detail dt {
    color: #cbd5e1;
    font-size: 17px;
}

.billing-detail dd {
    margin: 0;
    text-align: right;
    font-size: 23px;
    font-weight: 800;
}

.billing-detail .billing-amount {
    margin-top: 12px;
    border-bottom: 0;
}

.billing-amount dd {
    color: #fbbf24;
    font-size: clamp(38px, 5vw, 58px);
}

.billing-complete,
.billing-payment-wait {
    margin: 18px 0 0;
    padding: 14px;
    border-radius: 10px;
    text-align: center;
    font-weight: 800;
}

.billing-complete {
    color: #86efac;
    background: rgba(22, 101, 52, 0.3);
}

.billing-payment-wait {
    color: #fde68a;
    background: rgba(146, 64, 14, 0.3);
}

.billing-detail-actions {
    margin-top: auto;
    display: grid;
    grid-template-columns: 1fr 1fr;
    gap: 14px;
}

.billing-detail-actions .billing-primary-button {
    margin-top: 0;
}

@media (max-width: 900px) {
    .billing-page {
        padding: 16px;
    }

    .billing-header {
        grid-template-columns: 1fr;
        justify-items: center;
        gap: 6px;
        padding: 14px;
    }

    .billing-header strong {
        display: none;
    }

    .billing-step {
        min-height: calc(100vh - 128px);
        padding: 22px;
    }

    .billing-car-card {
        grid-template-columns: 180px 1fr 100px;
        gap: 14px;
    }

    .billing-detail-step {
        grid-template-columns: 1fr;
    }

    .billing-selected-image,
    .billing-selected-image img {
        min-height: 280px;
        max-height: 360px;
    }
}

@media (max-width: 650px) {
    .billing-car-card {
        grid-template-columns: 1fr;
    }

    .billing-car-card img {
        height: 180px;
    }

    .billing-car-card button {
        width: 100%;
    }
}
</style>
