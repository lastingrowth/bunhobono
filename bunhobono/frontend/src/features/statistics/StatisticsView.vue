<template>
    <section class="statistics-page">
        <!-- 페이지 제목 영역 -->
        <div class="statistics-title-area">
            <h1>통계</h1>
            <p>주차장 이용 현황과 차량 입차 유형을 확인하세요.</p>
        </div>

        <!-- 상단 요약 카드 4개 -->
        <div class="statistics-summary-grid">
            <!-- 오늘 입차 수 -->
            <article class="summary-card blue">
                <div class="summary-icon">
                    🚘
                </div>

                <div>
                    <span>오늘 입차</span>
                    <strong>{{ statsStore.todayInCount }}건</strong>
                </div>
            </article>

            <!-- 오늘 출차 수 -->
            <article class="summary-card green">
                <div class="summary-icon">
                    🚗
                </div>

                <div>
                    <span>오늘 출차</span>
                    <strong>{{ statsStore.todayOutCount }}건</strong>
                </div>
            </article>

            <!-- 오늘 미등록 차량 수 -->
            <article class="summary-card orange">
                <div class="summary-icon">
                    🚙
                </div>

                <div>
                    <span>미등록 차량</span>
                    <strong>{{ statsStore.todayUnknownCount }}건</strong>
                </div>
            </article>

            <!-- OCR 평균 신뢰도 -->
            <article class="summary-card purple">
                <div class="summary-icon">
                    OCR
                </div>

                <div>
                    <span>평균 OCR 신뢰도</span>
                    <strong>{{ statsStore.averageConfidence }}%</strong>
                </div>
            </article>
        </div>

        <!-- 최근 7일 입주민 / 방문 차량 입차 비교 그래프 -->
        <article class="statistics-card weekly-entry-card">
            <div class="card-title-row">
                <h2>최근 7일 입주민 / 방문 차량 입차 비교</h2>

                <!-- 그래프 색상 설명 -->
                <div class="chart-legend">
                    <span>
                        <i class="legend-color resident"></i>
                        입주민 차량
                    </span>

                    <span>
                        <i class="legend-color visit"></i>
                        방문 차량
                    </span>
                </div>
            </div>

            <!-- 막대그래프 -->
            <div class="weekly-bar-chart">
                <div
                    v-for="day in statsStore.weeklyEntryStats"
                    :key="day.label"
                    class="weekly-bar-item">
                    <!-- 막대가 올라가는 영역 -->
                    <div class="bar-column">
                        <!-- 입주민 차량 막대 -->
                        <div
                            class="bar resident"
                            :style="{ height: getBarHeight(day.registeredCount, statsStore.weeklyMaxCount) }">
                            <span>{{ day.registeredCount }}</span>
                        </div>

                        <!-- 방문 차량 막대 -->
                        <div
                            class="bar visit"
                            :style="{ height: getBarHeight(day.visitCount, statsStore.weeklyMaxCount) }">
                            <span>{{ day.visitCount }}</span>
                        </div>
                    </div>

                    <!-- 날짜 라벨 -->
                    <strong>{{ day.label }}</strong>
                </div>
            </div>
        </article>

        <!-- 하단 카드 영역 -->
        <div class="statistics-bottom-grid">
            <!-- 주차장별 사용률 -->
            <article class="statistics-card parking-rate-card">
                <h2>주차장별 사용률</h2>

                <div class="parking-circle-grid">
                    <div
                        v-for="parking in statsStore.parkingUsageStats"
                        :key="parking.parkingNo"
                        class="parking-circle-item">
                        <strong>{{ parking.parkingName }}</strong>

                        <!-- conic-gradient로 만든 원형 사용률 그래프 -->
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

                <p class="card-help-text">
                    * 사용률 = (현재 주차 대수 / 총 주차면) × 100
                </p>
            </article>

            <!-- 오늘 입차 차량 유형 비율 -->
            <article class="statistics-card entry-type-card">
                <div class="entry-type-header">
                    <h2>오늘 입차 차량 유형 비율</h2>

                    <!-- 입차 차량 유형별 색상 설명 -->
                    <div class="entry-type-legend">
                        <span>
                            <i class="legend-color resident"></i>
                            입주민
                        </span>

                        <span>
                            <i class="legend-color visit"></i>
                            방문
                        </span>

                        <span>
                            <i class="legend-color unknown"></i>
                            미등록
                        </span>
                    </div>
                </div>

                <div class="entry-type-content">
                    <!-- 오늘 입차 차량 유형을 하나의 원형 그래프로 표시 -->
                    <div
                        class="entry-type-donut"
                        :style="entryTypeDonutStyle">
                        <div class="entry-type-donut-inner">
                            <span>총</span>
                            <strong>{{ statsStore.todayInCount }}건</strong>
                        </div>
                    </div>

                    <!-- 입주민 / 방문 / 미등록 수치 설명 -->
                    <div class="entry-type-list">
                        <div
                            v-for="item in statsStore.todayEntryTypeStats"
                            :key="item.label"
                            class="entry-type-row">
                            <span>{{ item.label }}</span>
                            <strong>{{ item.percent }}%</strong>
                            <small>({{ item.count }}건)</small>
                        </div>
                    </div>
                </div>
            </article>

            <!-- 최근 7일 미등록 차량 추이 -->
            <article class="statistics-card unknown-trend-card">
                <h2>최근 7일 미등록 차량 추이</h2>

                <!-- SVG로 점이 찍힌 꺾은선 그래프를 그린다. -->
                <div class="unknown-svg-wrap">
                    <svg class="unknown-svg" viewBox="0 0 320 170" preserveAspectRatio="none">
                        <!-- 배경 기준선 -->
                        <line
                            v-for="line in unknownGuideLines"
                            :key="line"
                            x1="20"
                            x2="300"
                            :y1="line"
                            :y2="line"
                            class="unknown-guide-line"
                        />

                        <!-- 날짜별 미등록 차량 수를 이어주는 꺾은선 -->
                        <polyline
                            :points="unknownPolylinePoints"
                            class="unknown-polyline"
                        />

                        <!-- 날짜별 점, 수치, 날짜 표시 -->
                        <g
                            v-for="point in unknownChartPoints"
                            :key="point.label">
                            <circle
                                :cx="point.x"
                                :cy="point.y"
                                r="4.5"
                                class="unknown-point"
                            />

                            <text
                                :x="point.x"
                                :y="point.y - 10"
                                text-anchor="middle"
                                class="unknown-value">
                                {{ point.count }}
                            </text>

                            <text
                                :x="point.x"
                                y="158"
                                text-anchor="middle"
                                class="unknown-label">
                                {{ point.label }}
                            </text>
                        </g>
                    </svg>
                </div>
            </article>

            <!-- 통계 기준 설명 -->
            <article class="statistics-card statistics-note-card">
                <h2>통계 기준</h2>

                <ul>
                    <li>
                        OCR 신뢰도는 모델 confidence 평균입니다.
                    </li>

                    <li>
                        입차 유형은 carlog와 vehicleType 기준입니다.
                    </li>

                    <li>
                        기준 시간은 오늘 00:00 ~ 23:59입니다.
                    </li>
                </ul>
            </article>
        </div>
    </section>
</template>

<script setup>
import { computed, onMounted } from 'vue'
import { useStatisticsStore } from '@/features/statistics/statisticsStore'

const statsStore = useStatisticsStore()

// 막대그래프 높이를 계산한다.
// 값이 0이면 0%, 값이 있으면 너무 작아서 안 보이지 않게 최소 10%로 표시한다.
const getBarHeight = (count, maxCount) => {
    const percent = Math.round((count / maxCount) * 100)

    return `${Math.max(percent, count > 0 ? 10 : 0)}%`
}

// 주차장 이름에 따라 원형 그래프 색상을 다르게 지정한다.
// A/B/C/D가 같은 색이면 구분이 어려워서 시안처럼 색을 나눴다.
const getParkingColor = (parkingName) => {
    if (parkingName?.includes('A')) {
        return '#2f80ed'
    }

    if (parkingName?.includes('B')) {
        return '#20c997'
    }

    if (parkingName?.includes('C')) {
        return '#ffb000'
    }

    if (parkingName?.includes('D')) {
        return '#7048e8'
    }

    return '#2f80ed'
}

// 주차장별 사용률 원형 그래프 스타일을 만든다.
// --parking-color는 원 안의 퍼센트 글자 색상에도 같이 사용한다.
const getCircleStyle = (percent, parkingName) => {
    const color = getParkingColor(parkingName)

    return {
        '--parking-color': color,
        background: `conic-gradient(${color} 0% ${percent}%, #e7edf5 ${percent}% 100%)`,
    }
}

// 오늘 입차 차량 유형 비율 원형 그래프 스타일
// 입주민, 방문객, 미등록 비율을 하나의 원형 그래프에 나눠서 표시한다.
const entryTypeDonutStyle = computed(() => {
    const resident = statsStore.todayEntryTypeStats.find((item) => {
        return item.label === '입주민'
    })?.percent ?? 0

    const visit = statsStore.todayEntryTypeStats.find((item) => {
        return item.label === '방문객'
    })?.percent ?? 0

    const residentEnd = resident
    const visitEnd = resident + visit

    return {
        background: `conic-gradient(
            #2f80ed 0% ${residentEnd}%,
            #20c997 ${residentEnd}% ${visitEnd}%,
            #ff8a00 ${visitEnd}% 100%
        )`,
    }
})

// 미등록 차량 추이 그래프의 점 좌표를 계산한다.
// SVG는 위쪽으로 갈수록 y값이 작아지므로, count가 클수록 y값을 작게 만든다.
const unknownChartPoints = computed(() => {
    const maxCount = statsStore.weeklyUnknownMaxCount || 1
    const chartTop = 24
    const chartBottom = 132
    const chartHeight = chartBottom - chartTop
    const gap = 280 / Math.max(statsStore.weeklyUnknownStats.length - 1, 1)

    return statsStore.weeklyUnknownStats.map((day, index) => {
        const percent = day.count / maxCount
        const x = 20 + (gap * index)
        const y = chartBottom - (chartHeight * percent)

        return {
            ...day,
            x,
            y,
        }
    })
})

// polyline에 넣을 좌표 문자열을 만든다.
// 예: "20,120 66,90 113,70" 형태로 SVG가 선을 그릴 수 있게 한다.
const unknownPolylinePoints = computed(() => {
    return unknownChartPoints.value.map((point) => {
        return `${point.x},${point.y}`
    }).join(' ')
})

// 미등록 차량 추이 그래프의 배경 기준선 위치
const unknownGuideLines = [36, 72, 108, 144]

// 화면이 처음 열릴 때 통계 데이터를 불러온다.
onMounted(async () => {
    await statsStore.loadStatistics()
})
</script>

<style scoped>
.statistics-page {
    padding: 24px 28px;
    color: #142033;
}

.statistics-title-area {
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

.statistics-summary-grid {
    display: grid;
    grid-template-columns: repeat(4, minmax(0, 1fr));
    gap: 18px;
    margin-bottom: 22px;
}

.summary-card {
    display: flex;
    align-items: center;
    gap: 22px;
    min-height: 112px;
    padding: 24px 30px;
    border: 1px solid #dce4ef;
    border-radius: 16px;
    background: #fff;
    box-shadow: 0 8px 20px rgba(18, 32, 51, 0.06);
}

.summary-icon {
    display: flex;
    align-items: center;
    justify-content: center;
    width: 72px;
    height: 72px;
    border-radius: 50%;
    font-weight: 800;
}

.summary-card.blue .summary-icon {
    background: #eaf2ff;
    color: #1f6feb;
}

.summary-card.green .summary-icon {
    background: #e8f8ee;
    color: #19a85b;
}

.summary-card.orange .summary-icon {
    background: #fff1dd;
    color: #ff8a00;
}

.summary-card.purple .summary-icon {
    background: #f0eaff;
    color: #7048e8;
}

.summary-card span {
    display: block;
    color: #333f52;
    font-size: 16px;
    font-weight: 700;
}

.summary-card strong {
    display: block;
    margin-top: 8px;
    font-size: 34px;
    font-weight: 800;
}

.summary-card.blue strong {
    color: #1f6feb;
}

.summary-card.green strong {
    color: #19a85b;
}

.summary-card.orange strong {
    color: #ff8a00;
}

.summary-card.purple strong {
    color: #7048e8;
}

.statistics-card {
    border: 1px solid #dce4ef;
    border-radius: 16px;
    background: #fff;
    box-shadow: 0 8px 20px rgba(18, 32, 51, 0.06);
}

.statistics-card h2 {
    margin: 0;
    font-size: 19px;
    font-weight: 800;
}

.weekly-entry-card {
    padding: 22px 26px;
    margin-bottom: 22px;
}

.card-title-row {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 16px;
    margin-bottom: 18px;
}

.chart-legend {
    display: flex;
    align-items: center;
    gap: 18px;
    color: #4d5a6d;
    font-size: 14px;
    font-weight: 700;
}

.chart-legend span {
    display: flex;
    align-items: center;
    gap: 7px;
}

.legend-color {
    width: 12px;
    height: 12px;
    border-radius: 3px;
}

.legend-color.resident {
    background: #2f80ed;
}

.legend-color.visit {
    background: #20c997;
}

.legend-color.unknown {
    background: #ff8a00;
}

.entry-type-header {
    margin-bottom: 18px;
}

.entry-type-header h2 {
    margin-bottom: 12px;
}

/* 오늘 입차 차량 유형 비율 범례를 제목 아래 한 줄로 정리 */
.entry-type-legend {
    display: flex;
    align-items: center;
    gap: 14px;
    color: #4d5a6d;
    font-size: 13px;
    font-weight: 700;
}

.entry-type-legend span {
    display: flex;
    align-items: center;
    gap: 7px;
}

.weekly-bar-chart {
    display: grid;
    grid-template-columns: repeat(7, minmax(0, 1fr));
    gap: 22px;
    min-height: 270px;
    padding: 22px 10px 0;
    border-top: 1px solid #edf1f6;
}

.weekly-bar-item {
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: flex-end;
    gap: 12px;
}

.bar-column {
    display: flex;
    align-items: flex-end;
    justify-content: center;
    gap: 10px;
    width: 100%;
    height: 230px;
}

.bar {
    position: relative;
    width: 30px;
    min-height: 4px;
    border-radius: 8px 8px 0 0;
}

.bar span {
    position: absolute;
    top: -24px;
    left: 50%;
    transform: translateX(-50%);
    color: #3e4b5f;
    font-size: 13px;
    font-weight: 700;
}

.bar.resident {
    background: linear-gradient(180deg, #1269e8, #3b8cff);
}

.bar.visit {
    background: linear-gradient(180deg, #14b86e, #33d18a);
}

.weekly-bar-item strong {
    color: #6f7b8f;
    font-size: 14px;
}

.statistics-bottom-grid {
    display: grid;
    grid-template-columns: 1.15fr 0.9fr 1.15fr 0.6fr;
    gap: 18px;
}

.parking-rate-card,
.entry-type-card,
.unknown-trend-card,
.statistics-note-card {
    min-height: 250px;
    padding: 22px;
}

.parking-circle-grid {
    display: grid;
    grid-template-columns: repeat(4, 1fr);
    gap: 14px;
    margin-top: 22px;
}

.parking-circle-item {
    display: flex;
    flex-direction: column;
    align-items: center;
    gap: 10px;
}

.parking-circle-item strong {
    color: #4b5668;
    font-size: 13px;
}

.parking-circle {
    display: flex;
    align-items: center;
    justify-content: center;
    width: 86px;
    height: 86px;
    border-radius: 50%;
}

.parking-circle-inner {
    display: flex;
    align-items: center;
    justify-content: center;
    width: 62px;
    height: 62px;
    border-radius: 50%;
    background: #fff;
    color: var(--parking-color);
    font-size: 20px;
    font-weight: 800;
}

.parking-circle-item small {
    color: #5d6878;
    font-weight: 700;
}

.card-help-text {
    margin: 18px 0 0;
    color: #8a95a8;
    font-size: 13px;
}

.entry-type-content {
    display: flex;
    align-items: center;
    gap: 22px;
    margin-top: 18px;
}

.entry-type-donut {
    display: flex;
    align-items: center;
    justify-content: center;
    width: 142px;
    height: 142px;
    border-radius: 50%;
}

.entry-type-donut-inner {
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    width: 88px;
    height: 88px;
    border-radius: 50%;
    background: #fff;
}

.entry-type-donut-inner span {
    color: #6f7b8f;
    font-size: 14px;
}

.entry-type-donut-inner strong {
    color: #142033;
    font-size: 20px;
}

.entry-type-list {
    flex: 1;
}

.entry-type-row {
    display: grid;
    grid-template-columns: 1fr 52px 60px;
    align-items: center;
    gap: 8px;
    margin-bottom: 12px;
}

.entry-type-row span {
    color: #4b5668;
    font-weight: 700;
}

.entry-type-row strong {
    color: #142033;
    font-size: 18px;
}

.entry-type-row small {
    color: #7d8797;
}

.unknown-svg-wrap {
    height: 190px;
    margin-top: 20px;
    padding: 8px 4px 0;
    border-top: 1px solid #edf1f6;
}

.unknown-svg {
    width: 100%;
    height: 100%;
}

.unknown-guide-line {
    stroke: #edf1f6;
    stroke-width: 1;
}

.unknown-polyline {
    fill: none;
    stroke: #ff8a00;
    stroke-width: 3;
    stroke-linecap: round;
    stroke-linejoin: round;
}

.unknown-point {
    fill: #fff;
    stroke: #ff8a00;
    stroke-width: 3;
}

.unknown-value {
    fill: #ff8a00;
    font-size: 12px;
    font-weight: 800;
}

.unknown-label {
    fill: #7d8797;
    font-size: 11px;
    font-weight: 700;
}

.statistics-note-card {
    background: #fbfdff;
}

.statistics-note-card ul {
    margin: 22px 0 0;
    padding-left: 18px;
    color: #3f4b5d;
    font-size: 14px;
    line-height: 1.8;
}

@media (max-width: 1200px) {
    .statistics-summary-grid,
    .statistics-bottom-grid {
        grid-template-columns: repeat(2, minmax(0, 1fr));
    }
}

@media (max-width: 760px) {
    .statistics-summary-grid,
    .statistics-bottom-grid {
        grid-template-columns: 1fr;
    }

    .parking-circle-grid {
        grid-template-columns: repeat(2, 1fr);
    }

    .weekly-bar-chart {
        overflow-x: auto;
    }
}
</style>