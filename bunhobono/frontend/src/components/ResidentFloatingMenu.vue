<template>
    <nav
        class="resident-floating-menu"
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

        <RouterLink to="/resident/mypage"
            :class="{
                active: route.path.startsWith('/resident/mypage')
            }">
            마이페이지
        </RouterLink>

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
import { computed, onMounted } from 'vue';
import { storeToRefs } from 'pinia';
import { useRoute } from 'vue-router';
import { useResVehicleStore } from '@/features/resVehicle/resVehicleStore';
import { useResidentDashboardStore } from '@/stores/residentDashboard';

const route = useRoute()
const resVehicleStore = useResVehicleStore()
const dashboardStore = useResidentDashboardStore()
const { weather, weatherLoading, weatherErrorMessage } = storeToRefs(dashboardStore)

const weatherIcon = computed(() => {
    const state = weather.value.precipitation || ''

    if (state.includes('눈')) return '❄️'
    if (state.includes('비') || state.includes('빗방울')) return '🌧️'
    return '☀️'
})

onMounted(() => {
    resVehicleStore.loadNotifications().catch(() => {})
    dashboardStore.loadWeather().catch(() => {})
})

</script>

<style scoped>
.resident-floating-menu {
    position: fixed;
    z-index: 999;
    top: var(--header-height);
    right: 0;
    left: 0;
    min-height: 44px;
    padding: 0 16px;
    display: flex;
    align-items: center;
    gap: 4px;
    border-bottom: 1px solid #dce8f2;
    background: rgba(255,255,255,.97);
    box-shadow: 0 4px 15px rgba(40,80,120,.08);
    backdrop-filter: blur(8px);
}

.resident-floating-menu a {
    min-height: 44px;
    padding: 0 13px;
    display: flex;
    justify-content: center;
    align-items: center;
    border-bottom: 3px solid transparent;
    color: #38536d;
    background: transparent;
    text-decoration: none;
    font-size: 14px;
    font-weight: 700;
    white-space: nowrap;
    transition: color .2s, border-color .2s, background .2s;
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

.resident-menu-weather {
    margin-left: auto;
    padding: 0 10px;
    display: flex;
    align-items: center;
    gap: 7px;
    border-left: 1px solid #dce8f2;
    color: #38536d;
    background: transparent;
}

.resident-menu-weather-heading {
    display: flex;
    align-items: center;
    font-size: 9px;
    font-weight: 700;
}

.resident-menu-weather-heading > span {
    display: none;
}

.resident-menu-weather-heading button {
    width: 22px;
    height: 22px;
    padding: 0;
    border: 0;
    color: var(--resident-accent);
    background: transparent;
    cursor: pointer;
}

.resident-menu-weather-main {
    display: flex;
    align-items: center;
    gap: 4px;
    margin: 0;
}

.resident-menu-weather-main span {
    font-size: 15px;
}

.resident-menu-weather-main strong {
    font-size: 14px;
}

.resident-menu-weather p {
    margin: 0;
    color: #5f768a;
    font-size: 11px;
    line-height: 1;
    white-space: nowrap;
}

.resident-floating-menu a.active {
    color: var(--resident-accent);
    border-bottom-color: var(--resident-accent);
    background: #f3fbfe;
}

.resident-floating-menu a:hover {
    color: var(--resident-accent);
    background: #eaf4ff;
}

@media (max-width: 1050px) {
    .resident-floating-menu {
        padding: 0 10px;
        overflow-x: auto;
    }

    .resident-floating-menu a {
        min-height: 44px;
        padding: 0 10px;
        font-size: 12px;
    }

    .resident-menu-weather {
        display: none;
    }
}
</style>
