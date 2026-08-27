<template>
    <section class="billing-page">
        <header class="billing-header">
            <strong>BONO 아파트</strong>
            <h1>주차요금 정산</h1>
        </header>

        <!-- 0단계: 출차 유형 선택 -->
        <div
            v-if="!exitType"
            class="billing-step billing-type-step"
        >
            <h2>출차 유형을 선택해주세요</h2>

            <div class="billing-type-actions">
                <button
                    type="button"
                    class="billing-type-button billing-type-resident"
                    @click="exitType = 'RESIDENT'"
                >
                    <strong>입주민 출차</strong>
                    <span>차량을 선택하고 바로 출차합니다</span>
                </button>

                <button
                    type="button"
                    class="billing-type-button billing-type-non-resident"
                    @click="exitType = 'NON_RESIDENT'"
                >
                    <strong>비입주민 출차</strong>
                    <span>차량을 선택하고 주차요금을 정산합니다</span>
                </button>
            </div>
        </div>

        <!-- 1단계: 차량번호 뒤 4자리 입력 -->
        <div
            v-else-if="
                exitType
                && !billingStore.selectedCar
            "
            class="billing-step billing-lookup-step"
        >
            <!-- 뒤로가기 버튼과 현재 출차 유형 제목을 화면 상단에 표시한다. -->
            <div class="billing-lookup-header">
                <button
                    type="button"
                    class="billing-lookup-back"
                    @click="backToExitType"
                >
                    ← 뒤로가기
                </button>

                <h2>
                    {{
                        exitType === 'RESIDENT'
                            ? '입주민 차량 선택'
                            : '비입주민 차량 선택'
                    }}
                </h2>
            </div>

            <!-- 차량번호 입력과 조회 버튼을 왼쪽 영역에 배치한다. -->
            <div class="billing-lookup-search">
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

            <!-- 차량번호 조회 전 안내와 조회 결과를 오른쪽 영역에 표시한다. -->
            <div class="billing-lookup-result">
                <h2>조회된 차량</h2>

                <!-- 차량을 조회하기 전에는 사용 방법을 안내한다. -->
                <div
                    v-if="billingStore.parkingCars.length === 0"
                    class="billing-lookup-empty"
                >
                    <p>
                        차량번호 뒤 4자리를 입력한 후<br />
                        차량 조회 버튼을 눌러주세요.
                    </p>
                </div>

                <!-- 조회된 차량이 있으면 같은 화면 오른쪽에 목록을 표시한다. -->
                <div
                    v-else
                    class="billing-car-list"
                >
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
            </div>
        </div>

        <!-- 3단계: 선택한 입주민 차량정보와 출차 여부를 확인한다. -->
        <div
            v-else-if="
                exitType === 'RESIDENT'
                && billingStore.selectedCar
            "
            class="billing-step billing-detail-step"
        >
            <!-- 선택한 입주민 차량의 입차 이미지를 표시한다. -->
            <div class="billing-selected-image">
                <img
                    :src="getCarImageUrl(billingStore.selectedCar.cameraDataNo)"
                    :alt="
                        `${billingStore.selectedCar.carNo}
                        선택 차량 이미지`
                    "
                />
            </div>

            <!-- 선택한 입주민 차량정보와 출차 버튼을 표시한다. -->
            <div class="billing-detail">
                <h2>입주민 차량 확인</h2>

                <dl>
                    <div>
                        <dt>차량번호</dt>
                        <dd>{{ billingStore.selectedCar.carNo }}</dd>
                    </div>

                    <div>
                        <dt>입차시각</dt>
                        <dd>{{ formatDateTime(billingStore.selectedCar.inTime) }}</dd>
                    </div>

                   <div>
                        <dt>현재 상태</dt>

                        <dd
                            v-if="
                                billingStore.selectedCar.spaceType
                                === 'EXIT_WAIT'
                            "
                        >
                            {{ billingStore.selectedCar.spaceCode }}
                            출차 대기 중
                        </dd>

                        <dd
                            v-else-if="
                                billingStore.residentExitTask?.taskStatus
                                === 'COMPLETED'
                            "
                        >
                            출차존 도착
                        </dd>

                        <dd
                            v-else-if="
                                billingStore.residentExitTask?.taskStatus
                                === 'FAILED'
                            "
                        >
                            이동 실패
                        </dd>

                        <dd v-else-if="residentExitRequested">
                            {{ residentExitStatusText() }}
                        </dd>

                        <dd v-else>
                            주차 중
                        </dd>
                    </div>

                    <div
                        v-if="
                            billingStore.selectedCar.spaceType
                            === 'EXIT_WAIT'
                        "
                    >
                        <dt>자동 재주차</dt>
                        <dd>
                            {{
                                formatReparkRemainingTime(
                                    billingStore.selectedCar.spaceUpdatedAt
                                )
                            }}
                        </dd>
                    </div>
                </dl>

                <!-- 출차 요청 전 확인 문구 -->
                <p
                    v-if="
                        !residentExitRequested
                        && billingStore.selectedCar.spaceType
                        !== 'EXIT_WAIT'
                    "
                    class="billing-exit-confirm"
                >
                    선택한 차량을 출차하시겠습니까?
                </p>

                <p
                    v-else-if="
                        !residentExitRequested
                        && billingStore.selectedCar.spaceType
                        === 'EXIT_WAIT'
                    "
                    class="billing-complete"
                >
                    차량이 출차대기존에 있습니다.
                </p>

                <!-- 로봇 작업 대기 또는 진행 중 안내 -->
                <p
                    v-else-if="
                        billingStore.residentExitTask?.taskStatus
                        !== 'COMPLETED'
                        && billingStore.residentExitTask?.taskStatus
                        !== 'FAILED'
                    "
                    class="billing-complete"
                >
                    차량을 출차존으로 이동하고 있습니다.
                </p>

                <!-- 차량 위치 이동까지 완료된 경우 -->
                <p
                    v-else-if="
                        billingStore.residentExitTask?.taskStatus
                        === 'COMPLETED'
                    "
                    class="billing-complete"
                >
                    차량이 출차존에 도착했습니다.
                </p>

                <!-- 출차존 도착 후 첫 화면 자동 복귀까지 남은 시간을 표시한다. -->
                <div
                    v-if="
                        billingStore.residentExitTask?.taskStatus
                        === 'COMPLETED'
                    "
                    class="billing-exit-countdown"
                >
                    <strong>
                        {{ residentExitResetSeconds }}
                    </strong>

                    <span>
                        초 후 자동으로 처음 화면으로 이동합니다
                    </span>
                </div>

                <!-- 로봇 출차 작업이 실패한 경우 -->
                <p
                    v-if="
                        billingStore.residentExitTask?.taskStatus
                        === 'FAILED'
                    "
                    class="billing-error"
                >
                    {{
                        billingStore.residentExitTask?.failureReason
                        || '차량 이동에 실패했습니다.'
                    }}
                </p>

                <p
                    v-if="billingStore.errorMessage"
                    class="billing-error"
                >
                    {{ billingStore.errorMessage }}
                </p>

                <!-- 출차 요청 전 차량 선택과 출차 실행 버튼 -->
                <div
                    v-if="!residentExitRequested"
                    class="billing-detail-actions"
                >
                    <button
                        type="button"
                        class="billing-secondary-button"
                        @click="billingStore.backToCarList"
                    >
                        다른 차량 선택
                    </button>

                    <button
                        v-if="billingStore.selectedCar.spaceType !== 'EXIT_WAIT'"
                        type="button"
                        class="billing-primary-button"
                        :disabled="billingStore.loading"
                        @click="requestResidentExit"
                    >
                        바로 출차하기
                    </button>
                </div>

                <!-- 이동 중이거나 이동을 완료한 뒤 첫 화면으로 돌아가는 버튼 -->
                <div
                    v-else-if="
                        billingStore.residentExitTask?.taskStatus
                        !== 'FAILED'
                    "
                    class="billing-detail-actions"
                >
                    <button
                        type="button"
                        class="billing-primary-button"
                        @click="resetBilling"
                    >
                        처음으로
                    </button>
                </div>

                <!-- 이동 실패 후 재시도하거나 첫 화면으로 돌아가는 버튼 -->
                <div
                    v-else
                    class="billing-detail-actions"
                >
                    <button
                        type="button"
                        class="billing-secondary-button"
                        @click="resetBilling"
                    >
                        처음으로
                    </button>

                    <button
                        type="button"
                        class="billing-primary-button"
                        :disabled="billingStore.loading"
                        @click="requestResidentExit"
                    >
                        다시 시도
                    </button>
                </div>
            </div>
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
                    :src="getCarImageUrl(billingStore.selectedCar.cameraDataNo)"
                    :alt="
                        `${billingStore.selectedCar.carNo}
                        선택 차량 이미지`
                    "
                />
            </div>

            <div class="billing-detail">
                <div class="billing-car-number-card">
                    {{ billingStore.bill.carNo }}
                </div>

                <dl>
                    <div>
                        <dt>입차시각</dt>
                        <dd>{{ formatDateTime(billingStore.bill.inTime) }}</dd>
                    </div>

                    <div>
                        <dt>주차시간</dt>
                        <dd>{{ formatParkingTime(billingStore.bill.inTime) }}</dd>
                    </div>

                    <div>
                        <dt>무료시간</dt>
                        <dd>{{ formatMinutes(billingStore.bill.freeTime) }}</dd>
                    </div>

                    <div class="billing-amount">
                        <dt>정산금액</dt>
                        <dd>{{ formatAmount(billingStore.bill.billAmount) }}</dd>
                    </div>
                </dl>

                <p
                    v-if="billingStore.errorMessage"
                    class="billing-error billing-payment-error"
                >
                    {{ billingStore.errorMessage }}
                </p>

                <p
                    v-if="billingStore.bill.billStatus === 'PAID'"
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
                        :disabled="paymentLoading"
                        @click="requestTossPayment"
                    >
                        {{ paymentLoading ? '결제창 준비 중' : '결제하기' }}
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
import { onUnmounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import { useBillingStore } from './billingStore'
import { ANONYMOUS, loadTossPayments } from '@tosspayments/tosspayments-sdk'
import { API_BASE_URL } from '@/shared/api/apiClient'

const route = useRoute()
const billingStore = useBillingStore()

// 환경변수에 저장한 토스페이먼츠 테스트 클라이언트 키
const tossClientKey = import.meta.env.VITE_TOSS_CLIENT_KEY

// 토스페이먼츠 결제창을 불러오는 동안 버튼의 중복 실행을 막는다
const paymentLoading = ref(false)

// 입주민 차량의 로봇 출차 요청 완료 여부
const residentExitRequested = ref(false)

// 출차존 도착 후 첫 화면으로 돌아가기까지 남은 시간
const residentExitResetSeconds = ref(10)

// 로봇 출차 작업 상태를 5초마다 확인하는 타이머
let residentExitPollTimer = null

// 출차 완료 화면을 보여준 뒤 첫 화면으로 돌아가는 타이머
let residentExitResetTimer = null

// 출차대기 차량의 자동 재주차 남은 시간을 갱신한다.
const reparkCountdownNow = ref(Date.now())

const reparkCountdownTimer = setInterval(() => {
    reparkCountdownNow.value = Date.now()
}, 1000)

const exitType = ref(null)

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

// 현재 키오스크에서 차량번호 뒤 4자리와 출차 유형으로 주차 차량을 조회한다.
const searchCars = async () => {
    if (!exitType.value) {
        billingStore.errorMessage =
            '출차 유형을 먼저 선택해주세요.'
        return
    }

    if (lastFourDigits.value.length !== 4) {
        billingStore.errorMessage =
            '차량번호 뒤 4자리를 입력해주세요.'
        return
    }

    // 키오스크 번호가 없으면 차량의 주차 층을 비교할 수 없으므로 조회하지 않는다.
    if (!Number.isInteger(kioskNo) || kioskNo <= 0) {
        billingStore.errorMessage =
            '키오스크 정보를 확인할 수 없습니다.'
        return
    }

    await billingStore.searchCars(
        lastFourDigits.value,
        exitType.value,
        kioskNo
    )
}

// 출차 유형에 따라 차량만 선택하거나 정산금액까지 조회한다.
const selectCar = async (car) => {

    // 새 차량을 선택하면 이전 입주민 출차 완료 상태를 초기화한다.
    residentExitRequested.value = false

    if (!Number.isInteger(kioskNo) || kioskNo <= 0) {
        billingStore.errorMessage =
            '키오스크 정보를 확인할 수 없습니다.'
        return
    }

    await billingStore.selectCar(
        car,
        kioskNo,
        exitType.value
    )
}

// 입주민 출차 상태 조회와 자동 초기화 타이머를 중지한다.
const stopResidentExitTimers = () => {
    if (residentExitPollTimer !== null) {
        window.clearInterval(residentExitPollTimer)
        residentExitPollTimer = null
    }

    if (residentExitResetTimer !== null) {
        window.clearInterval(residentExitResetTimer)
        residentExitResetTimer = null
    }
}

// 로봇 출차 작업의 최신 상태를 조회한다.
const checkResidentExitTask = async () => {
    const result = await billingStore.loadResidentExitTask()

    if (!result.success) {
        return
    }

    const taskStatus = result.task?.taskStatus

    // 차량 위치 이동까지 완료되면 10초 카운트다운을 시작한다.
    if (taskStatus === 'COMPLETED') {
        stopResidentExitTimers()
        residentExitResetSeconds.value = 10

        residentExitResetTimer = window.setInterval(() => {
            residentExitResetSeconds.value -= 1

            if (residentExitResetSeconds.value <= 0) {
                resetBilling()
            }
        }, 1000)

        return
    }

    // 로봇 작업이 실패하면 반복 조회를 중단하고 실패 화면을 유지한다.
    if (taskStatus === 'FAILED') {
        stopResidentExitTimers()
    }
}

// 선택한 입주민 차량의 로봇 출차 작업을 요청한다.
const requestResidentExit = async () => {
    stopResidentExitTimers()

    const result = await billingStore.requestResidentExit(kioskNo)

    if (!result.success) {
        return
    }

    residentExitRequested.value = true

    // 생성 직후 현재 작업 상태를 한 번 확인한다.
    await checkResidentExitTask()

    const taskStatus =
        billingStore.residentExitTask?.taskStatus

    // 첫 조회에서 이미 종료된 작업이면 반복 조회를 시작하지 않는다.
    if (
        taskStatus === 'COMPLETED'
        || taskStatus === 'FAILED'
    ) {
        return
    }

    // 작업이 끝날 때까지 5초마다 최신 상태를 확인한다.
    residentExitPollTimer = window.setInterval(() => {
        checkResidentExitTask()
    }, 5000)
}

// 현재 정산서 정보로 토스페이먼츠 카드·간편결제 통합결제창을 실행
const requestTossPayment = async () => {
    const bill = billingStore.bill

    if (!tossClientKey) {
        billingStore.errorMessage = '토스페이먼츠 클라이언트 키를 확인해주세요.'
        return
    }

    if(!bill || !bill.paymentOrderId || Number(bill.billAmount) <= 0) {
        billingStore.errorMessage = '결제할 정산정보를 확인할 수 없습니다.'
        return
    }

    paymentLoading.value = true
    billingStore.errorMessage = ''

    try {
        const tosspayments = await loadTossPayments(tossClientKey)

        const payment = tosspayments.payment({
            customerKey: ANONYMOUS
        })

        await payment.requestPayment({
            method: 'CARD',
            amount: {
                currency: 'KRW',
                value: Number(bill.billAmount)
            },
            orderId: bill.paymentOrderId,
            orderName: `${bill.carNo} 주차요금`,
            successUrl:
                `${window.location.origin}/kiosk/payment/success?kioskNo=${kioskNo}`,
            failUrl:
                `${window.location.origin}/kiosk/payment/fail?kioskNo=${kioskNo}`,
            card: {
                flowMode: 'DEFAULT',
                useEscrow: false,
                useCardPoint: false,
                useAppCardOnly: false
            }
        })
    } catch (error) {
        console.error('토스페이먼츠 결제창 실행 실패', error)

        billingStore.errorMessage = error.message || '결제창을 실행하지 못했습니다.'

        paymentLoading.value = false
    }
}

// 출차 유형 선택 화면으로 돌아가면서 진행 중인 타이머를 정리한다.
const backToExitType = () => {
    stopResidentExitTimers()

    exitType.value = null
    lastFourDigits.value = ''
    residentExitRequested.value = false
    billingStore.reset()
}

// 차량 선택 화면에서 번호 입력 화면으로 이동한다.
const backToSearch = () => {
    lastFourDigits.value = ''
    billingStore.backToSearch()
}

// 타이머와 출차 상태를 초기화하고 첫 화면으로 이동한다.
const resetBilling = () => {
    stopResidentExitTimers()

    exitType.value = null
    lastFourDigits.value = ''
    residentExitRequested.value = false
    billingStore.reset()
}

// 입차 차량의 전체 이미지 주소를 생성한다.
const getCarImageUrl = (cameraDataNo) => {
    if (!cameraDataNo) {
        return ''
    }

    return `${API_BASE_URL}/camera-data/${cameraDataNo}/image`
}

// 로봇 출차 작업 단계를 키오스크 안내 문구로 변환한다.
const residentExitStatusText = () => {
    const taskPhase =
        billingStore.residentExitTask?.taskPhase

    return {
        WAITING: '로봇 배정 중',
        TRAFFIC_WAIT_EMPTY: '이동 경로 대기 중',
        MOVING_TO_PICKUP: '차량 위치로 이동 중',
        PICKUP_POSITIONING: '차량 위치로 이동 중',
        LIFTING: '차량을 들어 올리는 중',
        TRAFFIC_WAIT_LOADED: '출차 이동 경로 대기 중',
        MOVING_TO_DROPOFF: '출차존으로 이동 중',
        DROPOFF_POSITIONING: '출차존으로 이동 중',
        LOWERING: '차량을 내려놓는 중'
    }[taskPhase] || '출차 작업을 준비하고 있습니다.'
}

// 날짜와 시간을 화면 표시 형식으로 변환한다.
const formatDateTime = (value) => {
    if (!value) {
        return '-'
    }
    return new Date(value).toLocaleString('ko-KR')
}

// 키오스크 화면을 벗어나면 남아 있는 반복 조회와 자동 초기화 타이머를 제거한다.
onUnmounted(() => {
    stopResidentExitTimers()
    clearInterval(reparkCountdownTimer)
})

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

// 출차대기면에 도착한 시점부터 자동 재주차까지 남은 시간을 표시한다.
const formatReparkRemainingTime = (spaceUpdatedAt) => {
    const currentTime = reparkCountdownNow.value

    if (!spaceUpdatedAt) {
        return '자동 재주차 시간을 확인할 수 없습니다.'
    }

    const reparkTime =
        new Date(spaceUpdatedAt).getTime()
        + (10 * 60 * 1000)

    const remainingMilliseconds =
        reparkTime - currentTime

    if (remainingMilliseconds <= 0) {
        return '곧 자동으로 다시 주차됩니다.'
    }

    const remainingMinutes =
        Math.ceil(remainingMilliseconds / 60000)

    return `${remainingMinutes}분 후 자동으로 다시 주차됩니다.`
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

/* 첫 화면에서 입주민·비입주민 출차 유형을 선택하는 영역 */
.billing-type-step {
    max-width: 920px;
    display: flex;
    flex-direction: column;
    justify-content: center;
}

/* 두 출차 유형 버튼을 세로로 배치한다. */
.billing-type-actions {
    width: 100%;
    margin-top: 32px;
    display: grid;
    gap: 24px;
}

/* 출차 유형 버튼의 공통 크기와 내부 문구 배치를 설정한다. */
.billing-type-button {
    min-height: 190px;
    padding: 32px 40px;
    display: flex;
    flex-direction: column;
    justify-content: center;
    align-items: center;
    gap: 12px;
    border: 2px solid transparent;
    border-radius: 20px;
    color: #ffffff;
    cursor: pointer;
}

/* 출차 유형 이름을 크게 표시한다. */
.billing-type-button strong {
    font-size: clamp(32px, 4vw, 50px);
    letter-spacing: -0.04em;
}

/* 출차 유형별 안내 문구를 표시한다. */
.billing-type-button span {
    font-size: clamp(17px, 2vw, 22px);
}

/* 입주민 출차 버튼은 청록색 계열로 구분한다. */
.billing-type-resident {
    border-color: #14b8a6;
    background:
        linear-gradient(
            135deg,
            #0f766e,
            #0d9488
        );
}

/* 비입주민 출차 버튼은 주황색 계열로 구분한다. */
.billing-type-non-resident {
    border-color: #f97316;
    background:
        linear-gradient(
            135deg,
            #c2410c,
            #ea580c
        );
}

/* 터치하거나 마우스를 올렸을 때 선택 가능한 버튼임을 강조한다. */
.billing-type-button:hover {
    transform: translateY(-2px);
    filter: brightness(1.08);
}

/* 키보드로 버튼을 선택할 때 포커스 위치를 표시한다. */
.billing-type-button:focus-visible {
    outline: 4px solid #f8fafc;
    outline-offset: 4px;
}

/* 12.9형 iPad의 세로(1024px)·가로(1366px) 화면을 모두 채운다. */
@media (pointer: coarse) and (min-width: 700px) and (max-width: 1400px) {
    .billing-page {
        min-height: 100dvh;
        padding: 20px;
    }

    .billing-header,
    .billing-step {
        width: 100%;
        max-width: none;
    }

    .billing-header {
        margin-bottom: 18px;
    }

    .billing-header h1 {
        font-size: clamp(42px, 4.2vw, 54px);
        font-weight: 900;
    }

    .billing-type-step {
        min-height: calc(100dvh - 134px);
        padding: 28px;
    }

    .billing-type-actions {
        grid-template-columns: repeat(2, minmax(0, 1fr));
        gap: 18px;
        margin-top: 24px;
    }

    .billing-type-button {
        min-width: 0;
        min-height: clamp(340px, 38dvh, 430px);
        height: auto;
        padding: 38px 28px;
    }

    .billing-type-button strong {
        font-size: clamp(27px, 4vw, 42px);
        white-space: nowrap;
    }

    .billing-type-button span {
        font-size: clamp(15px, 2vw, 20px);
        text-align: center;
        word-break: keep-all;
    }

    /* 아이패드에서는 차량 화면을 이미지 위, 정산정보 아래 순서로 표시한다. */
    .billing-detail-step {
        display: grid;
        grid-template-columns: minmax(0, 1fr);
        grid-template-rows: minmax(280px, 42%) minmax(0, 58%);
        grid-template-areas:
            "vehicle-image"
            "billing-information";
        gap: 18px;
        height: calc(100dvh - 134px);
        min-height: calc(100dvh - 134px);
        padding: 22px;
        overflow: hidden;
    }

    .billing-detail-step > .billing-selected-image {
        grid-area: vehicle-image;
    }

    .billing-detail-step > .billing-detail {
        grid-area: billing-information;
        min-height: 0;
    }

    .billing-selected-image,
    .billing-selected-image img {
        width: 100%;
        min-height: 0;
        height: 100%;
        max-height: none;
    }

    .billing-selected-image img {
        object-fit: contain;
    }

    .billing-detail h2 {
        margin-bottom: 8px;
        text-align: center;
        font-size: clamp(30px, 3.5vw, 42px);
    }

    .billing-detail dl > div {
        padding: clamp(8px, 1.15dvh, 13px) 0;
    }

    .billing-detail-actions {
        margin-top: auto;
        padding-top: 14px;
    }
}

/* 차량번호 입력 영역과 조회 결과를 좌우로 배치한다. */
.billing-lookup-step {
    display: grid;
    grid-template-columns: minmax(320px, 0.8fr) minmax(520px, 1.2fr);
    gap: 28px;
    align-items: stretch;
}

/* 뒤로가기 버튼과 현재 출차 유형 제목을 통합 조회 화면 상단 전체에 배치한다. */
.billing-lookup-header {
    grid-column: 1 / -1;
    display: grid;
    grid-template-columns: 180px 1fr 180px;
    align-items: center;
    gap: 20px;
}

/* 현재 출차 유형 제목을 화면 중앙에 표시한다. */
.billing-lookup-header h2 {
    grid-column: 2;
    margin: 0;
    text-align: center;
}

/* 출차 유형 선택 화면으로 돌아가는 버튼을 헤더 왼쪽에 배치한다. */
.billing-lookup-back {
    grid-column: 1;
    justify-self: stretch;
    min-width: 160px;
    min-height: 52px;
    padding: 10px 22px;
    border: 1px solid #64748b;
    border-radius: 12px;
    color: #f8fafc;
    background: #233a5d;
    cursor: pointer;
    font-size: 18px;
    font-weight: 800;
}

/* 뒤로가기 버튼에 마우스를 올리거나 키보드로 선택한 상태를 표시한다. */
.billing-lookup-back:hover {
    background: #304d77;
}

.billing-lookup-back:focus-visible {
    outline: 3px solid #f8fafc;
    outline-offset: 3px;
}

/* 왼쪽 차량번호 입력 영역을 세로로 배치한다. */
.billing-lookup-search {
    min-width: 0;
    display: flex;
    flex-direction: column;
    justify-content: center;
}

/* 오른쪽 차량 조회 결과 영역을 구분한다. */
.billing-lookup-result {
    min-width: 0;
    padding: 24px;
    border: 1px solid #3e587d;
    border-radius: 16px;
    background: rgba(2, 10, 25, 0.45);
}

/* 조회 전 안내 문구를 결과 영역 중앙에 표시한다. */
.billing-lookup-empty {
    min-height: 420px;
    display: flex;
    justify-content: center;
    align-items: center;
    color: #cbd5e1;
    text-align: center;
    font-size: 22px;
    line-height: 1.7;
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

.billing-payment-error {
    padding: 12px 16px;
    border: 1px solid rgba(248, 113, 113, 0.5);
    border-radius: 10px;
    background: rgba(127, 29, 29, 0.28);
    text-align: center;
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

.billing-car-number-card {
    width: 100%;
    min-height: 78px;
    margin: 0 0 18px;
    padding: 14px 24px;
    display: grid;
    place-items: center;
    border: 1px solid rgba(251, 191, 36, 0.62);
    border-radius: 16px;
    color: #fff3bf;
    background:
        linear-gradient(135deg, rgba(180, 125, 12, 0.34), rgba(251, 191, 36, 0.13));
    box-shadow:
        inset 0 1px 0 rgba(255, 255, 255, 0.14),
        0 12px 30px rgba(2, 12, 27, 0.22);
    font-size: clamp(34px, 4.5vw, 54px);
    font-weight: 900;
    letter-spacing: 0.04em;
    line-height: 1;
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

/* 출차존 도착 후 자동으로 첫 화면으로 돌아가기까지 남은 시간을 표시한다. */
.billing-exit-countdown {
    margin: 22px 0 18px;
    display: flex;
    justify-content: center;
    align-items: center;
    gap: 18px;
    color: #cbd5e1;
}

/* 남은 초를 원형 테두리 안에 크게 표시한다. */
.billing-exit-countdown strong {
    width: 92px;
    height: 92px;
    flex: 0 0 92px;
    display: grid;
    place-items: center;
    border: 6px solid #fbbf24;
    border-radius: 50%;
    color: #f8fafc;
    background: rgba(2, 10, 25, 0.55);
    box-shadow: 0 0 22px rgba(251, 191, 36, 0.18);
    font-size: 40px;
    line-height: 1;
}

/* 자동 복귀 안내 문구를 카운트다운 숫자 옆에 표시한다. */
.billing-exit-countdown span {
    max-width: 250px;
    font-size: 18px;
    line-height: 1.5;
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

/* 버튼이 하나인 이동 중·완료 화면에서는 처음으로 버튼을 가운데 배치한다. */
.billing-detail-actions:has(> button:only-child) {
    grid-template-columns: minmax(260px, 380px);
    justify-content: center;
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

    .billing-exit-countdown {
        flex-direction: column;
        gap: 12px;
        text-align: center;
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

/* iPad 세로 모드: 조회 결과를 위에, 차량번호 키패드를 아래에 배치한다. */
@media (pointer: coarse) and (min-width: 700px) and (max-width: 1100px) and (orientation: portrait) {
    .billing-lookup-step {
        grid-template-columns: minmax(0, 1fr);
        grid-template-rows: auto minmax(280px, 1fr) auto;
        grid-template-areas:
            "lookup-header"
            "lookup-result"
            "lookup-search";
        height: calc(100dvh - 134px);
        min-height: calc(100dvh - 134px);
        overflow: hidden;
        gap: 20px;
    }

    .billing-lookup-header {
        grid-area: lookup-header;
        grid-column: 1;
    }

    .billing-lookup-result {
        grid-area: lookup-result;
        display: flex;
        min-height: 0;
        flex-direction: column;
        padding: 20px;
        overflow: hidden;
    }

    .billing-lookup-result > h2 {
        flex: 0 0 auto;
        margin-bottom: 16px;
        font-size: 34px;
    }

    .billing-lookup-empty {
        min-height: 0;
        flex: 1;
        font-size: 24px;
    }

    .billing-car-list {
        min-height: 0;
        max-height: none;
        flex: 1;
        overflow-y: auto;
    }

    .billing-lookup-search {
        grid-area: lookup-search;
        width: min(760px, 100%);
        align-self: end;
        justify-self: center;
        padding-top: 12px;
        border-top: 1px solid rgba(100, 116, 139, 0.65);
        background: #091c39;
    }

    .billing-number-display {
        min-height: 96px;
        margin: 16px 0;
    }

    .billing-keypad button {
        min-height: 72px;
    }
}

/*
 * 12.9형 iPad 최종 상세 레이아웃.
 * 기본 상세 화면과 다른 반응형 규칙보다 뒤에서 선언해 좌우 2열 규칙이
 * 다시 덮어쓰지 못하게 한다. 세로 1024px·가로 1366px 모두 적용한다.
 */
@media (any-pointer: coarse) and (min-width: 700px) and (max-width: 1400px) {
    .billing-page {
        width: 100%;
        min-width: 0;
        height: 100dvh;
        min-height: 100dvh;
        display: flex;
        flex-direction: column;
        align-items: center;
        padding: 18px;
        overflow: hidden;
    }

    .billing-header {
        width: 100%;
        max-width: none;
        flex: 0 0 auto;
        margin-right: auto;
        margin-left: auto;
    }

    .billing-detail-step {
        width: 100%;
        max-width: none;
        min-width: 0;
        min-height: 0;
        height: auto;
        flex: 1 1 auto;
        display: grid;
        grid-template-columns: minmax(0, 1fr);
        grid-template-rows: minmax(0, 43%) minmax(0, 57%);
        grid-template-areas:
            "vehicle-image"
            "billing-information";
        place-items: stretch;
        gap: 16px;
        margin: 0 auto;
        padding: 20px;
        overflow: hidden;
    }

    .billing-detail-step > .billing-selected-image {
        grid-area: vehicle-image;
        width: 100%;
        min-width: 0;
        min-height: 0;
        height: 100%;
        max-height: none;
        display: grid;
        place-items: center;
        margin: 0 auto;
        overflow: hidden;
    }

    .billing-detail-step > .billing-selected-image img {
        width: 100%;
        height: 100%;
        min-width: 0;
        min-height: 0;
        max-width: 100%;
        max-height: 100%;
        display: block;
        margin: auto;
        object-fit: contain;
        object-position: 50% 50%;
    }

    .billing-detail-step > .billing-detail {
        grid-area: billing-information;
        width: 100%;
        min-width: 0;
        min-height: 0;
        margin: 0 auto;
    }

    .billing-detail h2 {
        margin: 0 0 6px;
        text-align: center;
        font-size: clamp(29px, 3.2vw, 40px);
    }

    .billing-car-number-card {
        min-height: clamp(72px, 8.5dvh, 94px);
        margin-bottom: 10px;
        padding: 12px 22px;
        font-size: clamp(40px, 5.2vw, 60px);
    }

    .billing-detail dl {
        min-height: 0;
        flex: 1 1 auto;
        display: grid;
        grid-template-rows: repeat(4, minmax(0, 1fr));
    }

    .billing-detail dl > div {
        min-height: 0;
        padding: clamp(8px, 1.15dvh, 14px) 8px;
        grid-template-columns: minmax(120px, 28%) minmax(0, 1fr);
        gap: 20px;
    }

    .billing-detail dt {
        font-size: clamp(19px, 2vw, 24px);
        font-weight: 700;
    }

    .billing-detail dd {
        font-size: clamp(24px, 2.8vw, 34px);
    }

    .billing-amount dd {
        font-size: clamp(42px, 5.2vw, 62px);
    }

    .billing-detail-actions {
        margin-top: auto;
        padding-top: 12px;
    }

    .billing-detail-actions button {
        min-height: clamp(62px, 7dvh, 78px);
        font-size: clamp(21px, 2.3vw, 28px);
    }
}
</style>
