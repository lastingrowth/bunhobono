<template>
    <section class="statistics-page">

        <!-- 데이터 조회 상태 -->
        <p
            v-if="statsStore.loading"
            class="statistics-message">
            통계 데이터를 불러오는 중입니다.
        </p>

        <p
            v-if="statsStore.errorMessage"
            class="statistics-message error">
            {{ statsStore.errorMessage }}
        </p>

        <!-- 상단 3개 카드 영역 -->
        <div class="statistics-top-grid">
            <!-- 현재 주차 현황 -->
            <article class="statistics-card current-parking-card">
                <div class="card-title-row">
                    <h2>주차 현황</h2>
                </div>

                <div class="current-parking-content">
                    <!-- 입주민 / 방문차량 / 미등록 차량 도넛 그래프 -->
                    <div
                        class="current-parking-donut"
                        :style="currentParkingDonutStyle">
                        <div class="current-parking-donut-inner">
                            <span>총</span>
                            <strong>{{ statsStore.currentParkingTotal }}대</strong>
                        </div>
                    </div>

                    <!-- 도넛 그래프 범례 -->
                    <div class="parking-type-list">
                        <div
                            v-for="item in statsStore.currentParkingTypeStats"
                            :key="item.key"
                            class="parking-type-row">
                            <span>
                                <i :class="['legend-color', item.key]"></i>
                                {{ item.label }}
                            </span>

                            <strong>
                                {{ item.count }}대
                                <small>({{ item.percent }}%)</small>
                            </strong>
                        </div>
                    </div>
                </div>
            </article>

            <!-- 시간대별 평균 주차 대수 -->
            <article class="statistics-card hourly-card">
                <div class="card-title-row">
                    <h2>시간대별 평균 주차 대수</h2>
                </div>

                <!-- SVG 꺾은선 그래프 -->
                <div class="hourly-chart-wrap">
                    <svg
                        class="hourly-chart"
                        viewBox="0 0 360 190"
                        preserveAspectRatio="none">
                        <!-- 배경 기준선 -->
                        <line
                            v-for="line in hourlyGuideLines"
                            :key="line"
                            x1="30"
                            x2="335"
                            :y1="line"
                            :y2="line"
                            class="chart-guide-line"
                        />

                        <!-- 시간대별 주차 대수를 연결하는 선 -->
                        <polyline
                            :points="hourlyPolylinePoints"
                            class="hourly-polyline"
                        />

                        <!-- 각 시간대의 점과 숫자 -->
                        <g
                            v-for="point in hourlyChartPoints"
                            :key="point.label">
                            <circle
                                :cx="point.x"
                                :cy="point.y"
                                r="5"
                                class="hourly-point"
                            />

                            <text
                                :x="point.x"
                                :y="point.y - 10"
                                text-anchor="middle"
                                class="chart-value">
                                {{ point.count }}
                            </text>

                            <text
                                :x="point.x"
                                y="176"
                                text-anchor="middle"
                                class="chart-label">
                                {{ point.label }}
                            </text>
                        </g>
                    </svg>
                </div>
            </article>

            <!-- 비입주민 주차 주의 현황 -->
            <article class="statistics-card warning-card">
                <div class="card-title-row">
                    <h2>비입주민 장기주차 알림</h2>
                </div>

                <div class="warning-list">
                    <button
                        v-for="item in statsStore.nonResidentWarningStats"
                        :key="item.key"
                        type="button"
                        :class="['warning-row', item.key, { 'has-warning': Number(item.count) > 0 }]"
                        @click="goWarningPage(item.key)">
                        <span>{{ item.label }}</span>
                        <strong>{{ item.count }}건</strong>
                    </button>
                </div>

                <p class="card-help-text">
                    기준: 다음날까지 미출차 / 등록 시간 만료
                </p>
            </article>
        </div>

        <!-- 입주민 / 비입주민 입차 비교 -->
        <article class="statistics-card entry-compare-card">
            <div class="card-title-row">
                <div>
                    <h2>입차 비교</h2>
                </div>

                <div class="entry-header-tools">
                    <!-- 막대그래프 범례 -->
                    <div class="chart-legend">
                        <span>
                            <i class="legend-color resident"></i>
                            입주민
                        </span>

                        <span>
                            <i class="legend-color non-resident"></i>
                            비입주민
                        </span>
                    </div>

                    <!-- 주간 / 월간 / 연간 버튼 -->
                    <div class="period-buttons">
                        <button
                            type="button"
                            :class="{ active: statsStore.entryPeriod === 'weekly' }"
                            @click="statsStore.changeEntryPeriod('weekly')">
                            주간
                        </button>

                        <button
                            type="button"
                            :class="{ active: statsStore.entryPeriod === 'monthly' }"
                            @click="statsStore.changeEntryPeriod('monthly')">
                            월간
                        </button>

                        <button
                            type="button"
                            :class="{ active: statsStore.entryPeriod === 'yearly' }"
                            @click="statsStore.changeEntryPeriod('yearly')">
                            연간
                        </button>
                    </div>

                    <div class="period-navigation" aria-label="입차 비교 조회 기간 이동">
                        <button
                            type="button"
                            class="period-nav-button"
                            aria-label="이전 기간"
                            @click="statsStore.moveEntryPeriod('previous')">
                            ‹
                        </button>
                        <span>{{ statsStore.entryPeriodLabel }}</span>
                        <button
                            type="button"
                            class="period-nav-button"
                            aria-label="다음 기간"
                            :disabled="!statsStore.canMoveEntryPeriodNext"
                            @click="statsStore.moveEntryPeriod('next')">
                            ›
                        </button>
                    </div>
                </div>
            </div>

            <!-- 주간 / 월간 / 연간이 같은 위치에서 바뀌는 막대그래프 -->
            <div class="entry-bar-chart">
                <div
                    v-for="item in statsStore.entryCompareStats"
                    :key="item.label"
                    class="entry-bar-item">
                    <div class="bar-column">
                        <div
                            class="bar resident"
                            :style="{ height: getBarHeight(item.residentCount, statsStore.entryCompareMaxCount) }">
                            <span>{{ item.residentCount }}</span>
                        </div>

                        <div
                            class="bar non-resident"
                            :style="{ height: getBarHeight(item.nonResidentCount, statsStore.entryCompareMaxCount) }">
                            <span>{{ item.nonResidentCount }}</span>
                        </div>
                    </div>

                    <strong>{{ item.label }}</strong>
                </div>
            </div>
        </article>

        <!-- 하단 2개 카드 영역 -->
        <div class="statistics-bottom-grid">
            <!-- 현재 주차장 현황 -->
            <article class="statistics-card parking-rate-card">
                <div class="card-title-row">
                    <h2>주차장 현황</h2>
                </div>

                <div class="parking-circle-grid">
                    <div
                        v-for="parking in statsStore.parkingUsageStats"
                        :key="parking.parkingNo"
                        class="parking-circle-item">
                        <strong>{{ parking.parkingName }}</strong>

                        <div
                            class="parking-circle"
                            :style="getCircleStyle(parking.percent, parking.parkingName)">
                            <div class="parking-circle-inner">
                                {{ parking.percent }}%
                            </div>
                        </div>

                        <small>
                            {{ parking.used }} / {{ parking.total }}면
                        </small>
                    </div>
                </div>
            </article>

            <!-- 방문차량 / 미등록 차량 평균 주차 시간 -->
            <article class="statistics-card average-time-card">
                <div class="card-title-row">
                    <h2>차량 평균 주차 시간</h2>
                </div>

                <div class="average-time-list">
                    <div
                        v-for="item in statsStore.averageParkingTimeStats"
                        :key="item.key"
                        :class="['average-time-row', item.key]">
                        <span>{{ item.label }}</span>

                        <div class="average-time-bar-wrap">
                            <div
                                class="average-time-bar"
                                :style="{ width: `${item.percent}%` }">
                            </div>
                        </div>

                        <strong>{{ item.text }}</strong>
                    </div>
                </div>
            </article>
        </div>
    </section>
</template>

<script setup>
import { computed, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { useStatisticsStore } from '@/features/statistics/statisticsStore'

const router = useRouter()
const statsStore = useStatisticsStore()

let timer = null
let midnightTimer = null

const scheduleMidnightUpdate = () => {
    if (midnightTimer) {
        clearTimeout(midnightTimer)
    }

    const now = new Date()

    const nextMidnight = new Date(now)
    nextMidnight.setDate(now.getDate() + 1)
    nextMidnight.setHours(0, 0, 0, 0)

    const delay = nextMidnight.getTime() - now.getTime()

    midnightTimer = setTimeout(async () => {

        // 오늘 날짜 갱신
        statsStore.updateToday()

        // 자정이 되었으므로 통계를 다시 불러온다.
        await statsStore.loadStatistics()

        // 다음 자정을 다시 예약
        scheduleMidnightUpdate()
    }, delay)
}

// 막대그래프 높이를 계산한다.
// 값이 너무 작아도 화면에서 보이도록 최소 높이를 8%로 둔다.
const getBarHeight = (count, maxCount) => {
    const percent = Math.round((count / maxCount) * 100)

    return `${Math.max(percent, count > 0 ? 8 : 0)}%`
}

// 주차장 이름에 따라 원형 게이지 색상을 다르게 준다.
const getParkingColor = (parkingName) => {
    return '#39ff88'
}

// 주차장 사용률 원형 그래프 스타일을 만든다.
const getCircleStyle = (percent, parkingName) => {
    const color = getParkingColor(parkingName)

    return {
        '--parking-color': color,
        background: `conic-gradient(${color} 0% ${percent}%, #224236 ${percent}% 100%)`,
    }
}

// 현재 주차 현황 도넛 그래프 스타일
const currentParkingDonutStyle = computed(() => {
    const resident = statsStore.currentParkingTypeStats.find((item) => {
        return item.key === 'resident'
    })?.percent ?? 0

    const visit = statsStore.currentParkingTypeStats.find((item) => {
        return item.key === 'visit'
    })?.percent ?? 0

    const residentEnd = resident
    const visitEnd = resident + visit

    return {
        background: `conic-gradient(
            #39ff88 0% ${residentEnd}%,
            #55c7ff ${residentEnd}% ${visitEnd}%,
            #efff5a ${visitEnd}% 100%
        )`,
    }
})

// 시간대별 주차 대수 그래프의 좌표를 계산한다.
const hourlyChartPoints = computed(() => {
    const maxCount = statsStore.hourlyParkingMaxCount || 1
    const chartTop = 30
    const chartBottom = 145
    const chartHeight = chartBottom - chartTop
    const gap = 305 / Math.max(statsStore.hourlyParkingStats.length - 1, 1)

    return statsStore.hourlyParkingStats.map((item, index) => {
        const percent = item.count / maxCount
        const x = 30 + (gap * index)
        const y = chartBottom - (chartHeight * percent)

        return {
            ...item,
            x,
            y,
        }
    })
})

// SVG polyline에 들어갈 좌표 문자열을 만든다.
const hourlyPolylinePoints = computed(() => {
    return hourlyChartPoints.value.map((point) => {
        return `${point.x},${point.y}`
    }).join(' ')
})

// 시간대별 그래프 배경 기준선 위치
const hourlyGuideLines = [35, 70, 105, 140]

// 비입주민 주차 주의 현황을 클릭했을 때 관련 관리 페이지로 이동한다.
// query를 붙이지 않고, 의미 있는 path로 이동시켜 URL을 깔끔하게 유지한다
const goWarningPage = (key) => {
    if (key === 'visitLongStay') {
        router.push('/admin/notice/visit-long-stay')
        return
    }

    if (key === 'unknownLongStay') {
        router.push('/admin/notice/unknown-long-stay')
        return
    }

    if (key === 'parkedExpired') {
        router.push('/admin/vehicles/parked-expired')
    }
}

// 화면을 처음 열 때 통계 데이터를 불러온다.
onMounted(async () => {
    // 다음 자정 예약
    scheduleMidnightUpdate()

    await statsStore.loadStatistics()

    timer = setInterval(async () => {
        await statsStore.loadStatistics()
    }, 10000)
})

onUnmounted(() => {
    clearInterval(timer)
    clearTimeout(midnightTimer)
})
</script>

<style scoped>
.statistics-page {
    padding: 24px 28px;
    color: #142033;
}

.statistics-title-area {
    display: flex;
    justify-content: space-between;
    align-items: flex-start;
    gap: 16px;
    margin-bottom: 18px;
}

.statistics-title-area h1 {
    margin: 0;
    font-size: 34px;
    font-weight: 800;
}

.statistics-title-area p {
    margin: 8px 0 0;
    color: #7d8797;
    font-size: 16px;
}

.refresh-button {
    padding: 10px 16px;
    border: 1px solid #d5deeb;
    border-radius: 10px;
    background: #fff;
    color: #253047;
    font-weight: 700;
    cursor: pointer;
}

.statistics-message {
    margin: 0 0 14px;
    padding: 12px 14px;
    border-radius: 10px;
    background: #eef6ff;
    color: #2878d8;
    font-weight: 700;
}

.statistics-message.error {
    background: #fff0f0;
    color: #e03131;
}

.statistics-card {
    border: 1px solid #dce4ef;
    border-radius: 16px;
    background: #fff;
    box-shadow: 0 8px 20px rgba(18, 32, 51, 0.06);
}

.statistics-top-grid {
    display: grid;
    grid-template-columns: 1.15fr 1.15fr 1fr;
    gap: 18px;
    margin-bottom: 18px;
}

.statistics-bottom-grid {
    display: grid;
    grid-template-columns: 1fr 1fr;
    gap: 18px;
}

.card-title-row {
    display: flex;
    justify-content: space-between;
    align-items: center;
    gap: 16px;
    margin-bottom: 18px;
}

.card-title-row h2 {
    margin: 0;
    font-size: 19px;
    font-weight: 800;
}

.card-title-row p {
    margin: 6px 0 0;
    color: #7d8797;
    font-size: 14px;
}

.current-parking-card,
.hourly-card,
.warning-card,
.parking-rate-card,
.average-time-card {
    padding: 22px;
}

.current-parking-content {
    display: flex;
    align-items: center;
    gap: 28px;
}

.current-parking-donut {
    display: flex;
    align-items: center;
    justify-content: center;
    width: 188px;
    height: 188px;
    border-radius: 50%;
    flex-shrink: 0;
}

.current-parking-donut-inner {
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    width: 104px;
    height: 104px;
    border-radius: 50%;
    background: #fff;
}

.current-parking-donut-inner span {
    color: #7d8797;
    font-size: 13px;
    font-weight: 700;
}

.current-parking-donut-inner strong {
    color: #142033;
    font-size: 26px;
    font-weight: 900;
}

.parking-type-list {
    flex: 1;
}

.parking-type-row {
    display: flex;
    justify-content: space-between;
    align-items: center;
    gap: 14px;
    margin-bottom: 14px;
    color: #253047;
}

.parking-type-row span {
    display: flex;
    align-items: center;
    gap: 8px;
    font-weight: 700;
}

.parking-type-row strong {
    font-size: 15px;
}

.parking-type-row small {
    color: #7d8797;
    font-size: 12px;
}

.legend-color {
    display: inline-block;
    width: 11px;
    height: 11px;
    border-radius: 3px;
}

.legend-color.resident {
    background: #2f80ed;
}

.legend-color.visit,
.legend-color.non-resident {
    background: #20c997;
}

.legend-color.unknown {
    background: #ff8a00;
}

.hourly-chart-wrap {
    height: 220px;
}

.hourly-chart {
    width: 100%;
    height: 100%;
}

.chart-guide-line {
    stroke: #dbe3ef;
    stroke-width: 1;
    stroke-dasharray: 4 4;
}

.hourly-polyline {
    fill: none;
    stroke: #ff9f43;
    stroke-width: 4;
    stroke-linecap: round;
    stroke-linejoin: round;
}

.hourly-point {
    fill: #fff;
    stroke: #ff9f43;
    stroke-width: 4;
}

.chart-value {
    fill: #253047;
    font-size: 12px;
    font-weight: 800;
}

.chart-label {
    fill: #7d8797;
    font-size: 12px;
    font-weight: 700;
}

.warning-list {
    display: flex;
    flex-direction: column;
    gap: 12px;
}

.warning-row {
    display: flex;
    justify-content: space-between;
    align-items: center;
    min-height: 54px;
    padding: 0 16px;
    border: 1px solid #dce4ef;
    border-radius: 12px;
    background: #fbfdff;
    color: #142033;
    text-align: left;
    cursor: pointer;
}

.warning-row:hover {
    border-color: #9bc5ff;
    background: #f4f9ff;
}

.warning-row span {
    font-weight: 800;
}

.warning-row strong {
    font-size: 24px;
    font-weight: 900;
}

.warning-row.visitLongStay strong {
    color: #ff8a00;
}

.warning-row.unknownLongStay strong {
    color: #e03131;
}

.warning-row.expiredVisit strong {
    color: #7048e8;
}

.card-help-text {
    margin: 14px 0 0;
    color: #7d8797;
    font-size: 13px;
}

.entry-compare-card {
    padding: 22px;
    margin-bottom: 18px;
}

.period-buttons {
    display: inline-flex;
    overflow: hidden;
    border: 1px solid #d5deeb;
    border-radius: 12px;
    background: #f6f8fb;
}

.period-buttons button {
    min-width: 76px;
    padding: 10px 16px;
    border: 0;
    border-right: 1px solid #d5deeb;
    background: transparent;
    color: #647086;
    font-weight: 800;
    cursor: pointer;
}

.period-buttons button:last-child {
    border-right: 0;
}

.period-buttons button.active {
    background: #2f80ed;
    color: #fff;
}

.chart-legend {
    display: flex;
    justify-content: center;
    gap: 28px;
    margin: 0;
}

.entry-header-tools {
    display: flex;
    align-items: center;
    gap: 24px;
}

.period-navigation {
    display: flex;
    align-items: center;
    gap: 7px;
}

.period-navigation > span {
    min-width: 112px;
    color: #647086;
    font-size: 12px;
    font-weight: 800;
    text-align: center;
    white-space: nowrap;
}

.period-navigation .period-nav-button {
    width: 30px;
    min-width: 30px;
    height: 30px;
    min-height: 30px;
    padding: 0;
    border-radius: 50% !important;
    font-size: 20px;
    line-height: 28px;
}

.entry-compare-card .card-title-row {
    position: relative;
}

.entry-header-tools .chart-legend {
    position: absolute;
    top: 50%;
    left: 22%;
    transform: translate(-50%, -50%);
}

.chart-legend span {
    display: flex;
    align-items: center;
    gap: 8px;
    color: #253047;
    font-size: 14px;
    font-weight: 700;
}

.entry-bar-chart {
    display: grid;
    grid-template-columns: repeat(auto-fit, minmax(48px, 1fr));
    align-items: end;
    gap: 12px;
    height: 260px;
    padding: 10px 8px 0;
    border-top: 1px dashed #dbe3ef;
}

.entry-bar-item {
    display: flex;
    flex-direction: column;
    justify-content: flex-end;
    align-items: center;
    min-width: 0;
    height: 100%;
}

.bar-column {
    display: flex;
    align-items: flex-end;
    justify-content: center;
    gap: 8px;
    width: 100%;
    height: 210px;
}

.bar {
    position: relative;
    width: 22px;
    min-height: 0;
    border-radius: 6px 6px 0 0;
}

.bar span {
    position: absolute;
    top: -22px;
    left: 50%;
    transform: translateX(-50%);
    color: #253047;
    font-size: 12px;
    font-weight: 800;
}

.bar.resident {
    background: linear-gradient(180deg, #2f80ed, #1c64d1);
}

.bar.non-resident {
    background: linear-gradient(180deg, #20c997, #12a679);
}

.entry-bar-item > strong {
    margin-top: 10px;
    color: #647086;
    font-size: 13px;
    font-weight: 800;
}

.parking-circle-grid {
    display: grid;
    grid-template-columns: repeat(4, minmax(0, 1fr));
    gap: 14px;
}

.parking-circle-item {
    display: flex;
    flex-direction: column;
    align-items: center;
    gap: 12px;
    padding: 16px 10px;
    border: 1px solid #e4ebf5;
    border-radius: 14px;
    background: #fbfdff;
}

.parking-circle-item > strong {
    font-size: 15px;
    font-weight: 800;
}

.parking-circle {
    display: flex;
    align-items: center;
    justify-content: center;
    width: 104px;
    height: 104px;
    border-radius: 50%;
}

.parking-circle-inner {
    display: flex;
    align-items: center;
    justify-content: center;
    width: 72px;
    height: 72px;
    border-radius: 50%;
    background: #fff;
    color: #142033;
    font-size: 22px;
    font-weight: 900;
}

.parking-circle-item small {
    color: var(--parking-color);
    font-size: 13px;
    font-weight: 800;
}

.average-time-list {
    display: flex;
    flex-direction: column;
    gap: 34px;
    padding-top: 14px;
}

.average-time-row {
    display: grid;
    grid-template-columns: 58px 1fr 110px;
    align-items: center;
    gap: 10px;
}

.average-time-row span {
    font-weight: 800;
}

.average-time-row strong {
    text-align: right;
    font-size: 21px;
    font-weight: 900;
    white-space: nowrap;
}

.average-time-bar-wrap {
    height: 13px;
    overflow: hidden;
    border-radius: 999px;
    background: #e9eef5;
}

.average-time-bar {
    height: 100%;
    border-radius: 999px;
}

.average-time-row.visit .average-time-bar {
    background: linear-gradient(90deg, #20c997, #12a679);
}

.average-time-row.visit strong {
    color: #20a86f;
}

.average-time-row.unknown .average-time-bar {
    background: linear-gradient(90deg, #ffb000, #ff7a00);
}

.average-time-row.unknown strong {
    color: #ff7a00;
}

@media (max-width: 900px) {
    .statistics-top-grid,
    .statistics-bottom-grid {
        grid-template-columns: 1fr;
    }

    .parking-circle-grid {
        grid-template-columns: repeat(2, minmax(0, 1fr));
    }
}

/* 데스크톱 통계 화면은 1280 × 720에서도 한 화면에 들어오도록 압축한다. */
@media (min-width: 901px) {
    .statistics-page {
        padding: 8px 12px;
    }

    .statistics-title-area {
        align-items: center;
        margin-bottom: 7px;
    }

    .statistics-title-area h1 {
        font-size: 24px;
    }

    .statistics-title-area p {
        display: none;
    }

    .refresh-button {
        padding: 6px 11px;
        font-size: 12px;
    }

    .statistics-top-grid,
    .statistics-bottom-grid {
        gap: 9px;
    }

    .statistics-top-grid {
        margin-bottom: 9px;
    }

    .current-parking-card,
    .hourly-card,
    .warning-card,
    .parking-rate-card,
    .average-time-card,
    .entry-compare-card {
        padding: 10px 12px;
    }

    .card-title-row {
        margin-bottom: 7px;
        gap: 8px;
    }

    .card-title-row h2 {
        font-size: 15px;
    }

    .card-title-row p {
        margin-top: 2px;
        font-size: 11px;
    }

    .current-parking-content {
        gap: 12px;
    }

    .current-parking-donut {
        width: 118px;
        height: 118px;
    }

    .current-parking-donut-inner {
        width: 66px;
        height: 66px;
    }

    .current-parking-donut-inner strong {
        font-size: 18px;
    }

    .parking-type-row {
        gap: 7px;
        margin-bottom: 7px;
        font-size: 12px;
    }

    .parking-type-row strong {
        font-size: 12px;
    }

    .hourly-chart-wrap {
        height: 125px;
    }

    .warning-list {
        gap: 6px;
    }

    .warning-row {
        min-height: 34px;
        padding: 0 10px;
        border-radius: 8px;
        font-size: 12px;
    }

    .warning-row strong {
        font-size: 16px;
    }

    .card-help-text {
        margin-top: 6px;
        font-size: 10px;
    }

    .entry-compare-card {
        margin-bottom: 9px;
    }

    .period-buttons {
        border-radius: 8px;
    }

    .period-buttons button {
        min-width: 52px;
        padding: 6px 9px;
        font-size: 11px;
    }

    .chart-legend {
        gap: 18px;
        margin-bottom: 3px;
    }

    .chart-legend span {
        font-size: 11px;
    }

    .entry-bar-chart {
        height: 155px;
        padding-top: 5px;
    }

    .bar-column {
        height: 118px;
    }

    .bar {
        width: 16px;
    }

    .entry-bar-item > strong {
        margin-top: 5px;
        font-size: 11px;
    }

    .parking-circle-grid {
        gap: 7px;
    }

    .parking-circle-item {
        gap: 5px;
        padding: 6px 5px;
        border-radius: 9px;
    }

    .parking-circle-item > strong,
    .parking-circle-item small {
        font-size: 11px;
    }

    .parking-circle {
        width: 62px;
        height: 62px;
    }

    .parking-circle-inner {
        width: 43px;
        height: 43px;
        font-size: 14px;
    }

    .average-time-list {
        gap: 20px;
        padding-top: 36px;
    }

    .average-time-row {
        grid-template-columns: 48px 1fr 82px;
        gap: 8px;
        font-size: 12px;
    }

    .average-time-row strong {
        font-size: 15px;
    }
}

@media (max-width: 700px) {
    .statistics-page {
        padding: 18px;
    }

    .statistics-title-area {
        flex-direction: column;
    }

    .current-parking-content {
        flex-direction: column;
        align-items: stretch;
    }

    .period-buttons {
        width: 100%;
    }

    .entry-header-tools {
        width: 100%;
        flex-wrap: wrap;
        gap: 10px;
    }

    .entry-header-tools .chart-legend {
        position: static;
        transform: none;
    }

    .period-buttons button {
        flex: 1;
    }

    .average-time-row {
        grid-template-columns: 1fr;
        gap: 8px;
    }

    .average-time-row strong {
        text-align: left;
    }
}
</style>
