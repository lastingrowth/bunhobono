<template>
    <nav
        class="resident-floating-menu"
        :style="{ top: `${menuTop}px` }"
    >
        <RouterLink to="/resident/dashboard"
            :class="{
                active: route.path === '/resident/dashboard'
            }">
            홈
        </RouterLink>

        <RouterLink to="/resident/vehicles"
            :class="{
                active: route.path === '/resident/vehicles' && !route.query.mode
            }">
            차량관리
        </RouterLink>

        <RouterLink to="/resident/vehicles?mode=notification"
            :class="{
                active: route.path === '/resident/vehicles'
                && route.query.mode === 'notification'
            }">
            <span>차량알림</span>
            <span
                v-if="resVehicleStore.unreadNotificationCount > 0"
                class="resident-menu-badge"
            >
                {{ resVehicleStore.unreadNotificationCount > 99
                    ? '99+'
                    : resVehicleStore.unreadNotificationCount }}
            </span>
        </RouterLink>

        <RouterLink to="/resident/carlogs"
            :class="{
                active: route.path === '/resident/carlogs'
            }">
            입출차기록
        </RouterLink>

        <RouterLink to="/resident/parkings"
            :class="{
                active: route.path.startsWith('/resident/parkings')
            }">
            주차현황
        </RouterLink>

        <RouterLink to="/resident/boards"
            :class="{
                active: route.path.startsWith('/resident/boards')
            }">
            공지사항
        </RouterLink>

        <RouterLink to="/resident/inquiries"
            :class="{
                active: route.path.startsWith('/resident/inquiries')
            }">
            1:1 문의
        </RouterLink>

        <div class="resident-menu-divider" aria-hidden="true"></div>

        <RouterLink to="/resident/mypage"
            :class="{
                active: route.path.startsWith('/resident/mypage')
            }">
            마이페이지
        </RouterLink>

        <div class="resident-menu-divider" aria-hidden="true"></div>

        <section class="resident-menu-weather" aria-label="부산 현재 날씨">
            <div class="resident-menu-weather-heading">
                <span>부산 날씨</span>
                <button
                    type="button"
                    title="날씨 새로고침"
                    :disabled="weatherLoading"
                    @click="dashboardStore.loadWeather"
                >
                    {{ weatherLoading ? '…' : '↻' }}
                </button>
            </div>

            <div class="resident-menu-weather-main">
                <span aria-hidden="true">{{ weatherIcon }}</span>
                <strong>{{ weather.temperature ?? '--' }}°</strong>
            </div>

            <p v-if="weatherErrorMessage">날씨 정보 없음</p>
            <template v-else>
                <p>{{ weather.precipitation }} · 습도 {{ weather.humidity ?? '--' }}%</p>
                <p>풍속 {{ weather.windSpeed ?? '--' }}m/s</p>
            </template>
        </section>
    </nav>
</template>

<script setup>
import { computed, nextTick, onMounted, onUnmounted, ref, watch } from 'vue';
import { storeToRefs } from 'pinia';
import { useRoute } from 'vue-router';
import { useResVehicleStore } from '@/features/resVehicle/resVehicleStore';
import { useResidentDashboardStore } from '@/stores/residentDashboard';

const route = useRoute()
const resVehicleStore = useResVehicleStore()
const dashboardStore = useResidentDashboardStore()
const { weather, weatherLoading, weatherErrorMessage } = storeToRefs(dashboardStore)
const menuTop = ref(0)

const weatherIcon = computed(() => {
    const state = weather.value.precipitation || ''

    if (state.includes('눈')) return '❄️'
    if (state.includes('비') || state.includes('빗방울')) return '🌧️'
    return '☀️'
})

let currentTop = 0
let animationFrame = 0

function getTargetTop() {
    const header = document.querySelector('.resident-layout .header')
    const headerBottom = header?.getBoundingClientRect().bottom ?? 60

    return window.scrollY + headerBottom + 36
}

function followScroll() {
    const targetTop = getTargetTop()
    const distance = targetTop - currentTop

    currentTop += distance * 0.16
    // 빠르게 아래로 스크롤해도 메뉴가 헤더 영역으로 올라가지 않도록
    // 현재 화면에서 허용되는 최소 위치보다 항상 아래에 표시합니다.
    menuTop.value = Math.max(currentTop, targetTop)

    if (Math.abs(distance) < 0.5) {
        currentTop = targetTop
        menuTop.value = targetTop
        animationFrame = 0
        return
    }

    animationFrame = window.requestAnimationFrame(followScroll)
}

function startFollowing() {
    if (animationFrame) return
    animationFrame = window.requestAnimationFrame(followScroll)
}

function resetMenuPosition() {
    window.cancelAnimationFrame(animationFrame)
    animationFrame = 0
    currentTop = getTargetTop()
    menuTop.value = currentTop
}

watch(
    () => route.fullPath,
    async () => {
        await nextTick()
        window.requestAnimationFrame(resetMenuPosition)
    }
)

onMounted(() => {
    resetMenuPosition()
    window.addEventListener('scroll', startFollowing, { passive: true })
    window.addEventListener('resize', resetMenuPosition)
    resVehicleStore.loadNotifications().catch(() => {})
    dashboardStore.loadWeather().catch(() => {})
})

onUnmounted(() => {
    window.cancelAnimationFrame(animationFrame)
    window.removeEventListener('scroll', startFollowing)
    window.removeEventListener('resize', resetMenuPosition)
})

</script>

<style scoped>

/* 메뉴 위치 */
.resident-floating-menu {
    /*
     * 페이지별 콘텐츠 크기나 렌더링 시점과 무관하게 같은 좌측 좌표를 사용합니다.
     * top은 현재 스크롤 위치를 부드럽게 따라가도록 스크립트에서 갱신합니다.
     */
    position: absolute;
    left: clamp(12px, 4vw, 60px);
    will-change: top;

    display: flex;
    flex-direction: column;
    gap: 10px;

    padding: 12px;

    border-radius: 16px;
    background: rgba(255,255,255,.95);
    border: 1px solid #dce8f2;
    box-shadow: 0 10px 25px rgba(40,80,120,.15);

    z-index: 1000;
}

/* 메뉴 */
.resident-floating-menu a {
    width: 100px;

    padding: 14px 10px;

    display: flex;
    justify-content: center;
    align-items: center;

    border-radius: 12px;

    color: #38536d;
    background: #f7fbff;

    text-decoration: none;

    font-size: 14px;
    font-weight: 700;

    transition: .2s;
}

.resident-menu-badge {
    display: inline-grid;
    min-width: 19px;
    height: 19px;
    place-items: center;
    margin-left: 6px;
    padding: 0 5px;
    border-radius: 10px;
    color: #fff;
    background: #e5484d;
    font-size: 10px;
    font-weight: 900;
}

.resident-menu-divider {
    height: 1px;
    margin: 2px 4px;
    background: #dce8f2;
}

.resident-menu-weather {
    width: 100px;
    padding: 10px;
    border: 1px solid #dce8f2;
    border-radius: 12px;
    color: #38536d;
    background: linear-gradient(135deg, #f4faff, #eaf5fd);
}

.resident-menu-weather-heading {
    display: flex;
    justify-content: space-between;
    align-items: center;
    font-size: 11px;
    font-weight: 700;
}

.resident-menu-weather-heading button {
    width: 22px;
    height: 22px;
    padding: 0;
    border: 0;
    color: #557993;
    background: transparent;
    cursor: pointer;
}

.resident-menu-weather-main {
    display: flex;
    align-items: center;
    gap: 7px;
    margin: 7px 0 5px;
}

.resident-menu-weather-main span {
    font-size: 20px;
}

.resident-menu-weather-main strong {
    font-size: 19px;
}

.resident-menu-weather p {
    margin: 0;
    color: #71879a;
    font-size: 9px;
    line-height: 1.45;
    white-space: nowrap;
}

/* 현재 페이지 */
.resident-floating-menu a.active {
    color: #1768bd;
    background: #eaf4ff;
}

/* hover */
.resident-floating-menu a:hover {
    background: #eaf4ff;
    transform: translateX(-3px);
}

@media(max-width:900px) {

    .resident-floating-menu {
        left: 12px;
    }

    .resident-floating-menu a {
        width: 85px;
        padding: 12px 5px;
        font-size: 12px;
    }

    .resident-menu-weather {
        width: 85px;
        padding: 8px 6px;
    }

    .resident-menu-weather-main {
        gap: 4px;
    }

    .resident-menu-weather-main strong {
        font-size: 16px;
    }
}

/* [모바일] 상단 메뉴를 사용하므로 왼쪽 플로팅 메뉴는 숨긴다. */
@media(max-width:760px) {
    .resident-floating-menu {
        display: none;
    }
}

</style>
