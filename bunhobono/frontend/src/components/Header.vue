<template>
    <header class="header">
        <div class="header-left">
            <button v-if="showSidebarToggle" class="sidebar-toggle" type="button"
                    aria-label="사이드바 열기 및 닫기" @click="$emit('toggle-sidebar')">
                ☰
            </button>
        
            <h1 class="logo"><RouterLink :to="homePath">아파트 주차관리 시스템</RouterLink></h1>
        </div>

        <!-- 입주민 헤더 -->
        <div v-if="isResidentDashboard" class="resident-header-actions">
            <div class="resident-menu-wrap">
                <button
                    type="button"
                    class="resident-menu-button"
                    :aria-expanded="residentMenuOpen"
                    @click="residentMenuOpen = !residentMenuOpen"
                >
                    메뉴 <span aria-hidden="true">▾</span>
                </button>
                <nav v-if="residentMenuOpen" class="resident-dropdown-menu" aria-label="입주민 메뉴">
                    <RouterLink
                        to="/resident/mypage"
                        :class="{ 'menu-active': route.path.startsWith('/resident/mypage') }"
                        @click="closeResidentMenu"
                    >내 정보 상세보기</RouterLink>
                    <RouterLink
                        to="/resident/vehicles"
                        :class="{ 'menu-active': route.path === '/resident/vehicles' && route.query.mode !== 'notification' }"
                        @click="closeResidentMenu"
                    >차량 관리</RouterLink>
                    <RouterLink
                        to="/resident/carlogs"
                        :class="{ 'menu-active': route.path === '/resident/carlogs' }"
                        @click="closeResidentMenu"
                    >입출차 내역</RouterLink>
                    <RouterLink
                        :to="{ path: '/resident/vehicles', query: { mode: 'notification' } }"
                        :class="{ 'menu-active': route.path === '/resident/vehicles' && route.query.mode === 'notification' }"
                        @click="closeResidentMenu"
                    >차량 알림</RouterLink>
                </nav>
            </div>
            <div class="resident-header-clock">
                <span>▣&nbsp; {{ formattedDate }}</span>
                <i></i>
                <span>◷&nbsp; {{ formattedTime }}</span>
            </div>
        </div>

        <!-- 관리자 헤더 -->
        <div v-if="isAdminRoute" class="admin-header-clock">
            <span>▣&nbsp; {{ formattedDate }}</span>
            <i></i>
            <span>◷&nbsp; {{ formattedTime }}</span>
            <button
                v-if="isAdminDashboard"
                type="button"
                class="header-demo-reset-button"
                :disabled="demoResetting"
                @click="requestDemoReset">
                {{ demoResetting ? '초기화 중...' : '시연 초기화' }}
            </button>
        </div>

        <div v-if="isAdminRoute" class="history-nav">
            <button @click="goBack" :disabled="!historyStore.canBack">←</button>
            <button @click="goForward" :disabled="!historyStore.canForward">→</button>
        </div>

        <div class="user-info">
            <span class="user-role">{{ roleLabel }}</span>
            <span class="divider">|</span>
            <span class="user-name">{{ jwtStore.userId }}</span>

            <button class="logout-btn" @click="logout">로그아웃</button>
        </div>

        <DemoResetConfirm
            :open="resetConfirmOpen"
            :resetting="demoResetting"
            @cancel="closeResetConfirm"
            @confirm="confirmDemoReset" />
        <ManagementFeedbackToast
            :message="resetFeedbackMessage"
            :type="resetFeedbackType" />

    </header>
</template>

<script setup>
import { useJwtStore } from '@/features/login/jwtStore';
import { resetDemo } from '@/features/reset/resetApi';
import DemoResetConfirm from '@/features/reset/DemoResetConfirm.vue';
import ManagementFeedbackToast from '@/shared/components/ManagementFeedbackToast.vue';
import { useHistoryStore } from '@/stores/historyStore';
import { computed, onMounted, onUnmounted, ref } from 'vue';
import { useRoute } from 'vue-router';

const jwtStore = useJwtStore()
const route = useRoute()
const historyStore = useHistoryStore()
const now = ref(new Date())
const residentMenuOpen = ref(false)
const demoResetting = ref(false)
const resetConfirmOpen = ref(false)
const resetFeedbackMessage = ref('')
const resetFeedbackType = ref('success')
let clockTimer
let resetFeedbackTimer
let resetReloadTimer

defineProps({
    showSidebarToggle: {
        type: Boolean,
        default: true,
    },
})

const homePath = computed(() => {
    if (jwtStore.role === 'ADMIN') {
        return '/admin/dashboard'
    }

    if (jwtStore.role === 'RESIDENT') {
        return '/resident'
    }

    return '/login'
})

const isResidentDashboard = computed(() => route.path.startsWith('/resident'))
const isAdminRoute = computed(() => route.path.startsWith('/admin'))
const isAdminDashboard = computed(() => route.path === '/admin/dashboard')
const roleLabel = computed(() => {
    if (jwtStore.role === 'RESIDENT') return '입주민'
    if (jwtStore.role === 'ADMIN') return '관리자'
    return jwtStore.role
})
const formattedDate = computed(() => new Intl.DateTimeFormat('ko-KR', {
    year: 'numeric', month: '2-digit', day: '2-digit', weekday: 'short',
}).format(now.value))
const formattedTime = computed(() => new Intl.DateTimeFormat('ko-KR', {
    hour: 'numeric', minute: '2-digit', hour12: true,
}).format(now.value))

function closeResidentMenu() {
    residentMenuOpen.value = false
}

function logout() {
    historyStore.clear()
    jwtStore.logout()
}

function requestDemoReset() {
    if (!demoResetting.value) {
        resetConfirmOpen.value = true
    }
}

function closeResetConfirm() {
    if (!demoResetting.value) {
        resetConfirmOpen.value = false
    }
}

function showResetFeedback(message, type = 'success') {
    resetFeedbackMessage.value = message
    resetFeedbackType.value = type
    window.clearTimeout(resetFeedbackTimer)
    resetFeedbackTimer = window.setTimeout(() => {
        resetFeedbackMessage.value = ''
    }, 3000)
}

async function confirmDemoReset() {
    if (demoResetting.value) return
    demoResetting.value = true

    try {
        await resetDemo()
        resetConfirmOpen.value = false
        showResetFeedback('시연 데이터와 영상 상태를 초기화했습니다.')
        resetReloadTimer = window.setTimeout(() => {
            window.location.reload()
        }, 1200)
    } catch (error) {
        console.error('시연 초기화 실패', error)
        const message = error?.response?.data?.message
            || error?.response?.data?.error
            || '시연 초기화에 실패했습니다. Spring과 FastAPI 실행 상태를 확인해주세요.'
        resetConfirmOpen.value = false
        showResetFeedback(message, 'error')
    } finally {
        demoResetting.value = false
    }
}

function goBack() {
    historyStore.back()
}

function goForward() {
    historyStore.forward()
}

onMounted(() => {
    clockTimer = window.setInterval(() => { now.value = new Date() }, 1000)
})

onUnmounted(() => {
    window.clearInterval(clockTimer)
    window.clearTimeout(resetFeedbackTimer)
    window.clearTimeout(resetReloadTimer)
})

const emit = defineEmits([
    'toggle-sidebar'
])
</script>

<style scoped>
.logo a {
  color: var(--primary-hover);
  text-decoration: none;
  font-weight: 800;
}

.logo a:hover {
  color: var(--primary);
  text-decoration: none;
}

.resident-header-actions {
  position: relative;
  flex-shrink: 0;
  margin-left: auto;
  margin-right: 18px;
  display: flex;
  align-items: center;
  gap: 7px;
}

@media (max-width: 1000px) {
  .header { gap: 10px; padding-inline: 12px; }
  .header-left { gap: 8px; }
  .header .logo { font-size: 17px; white-space: nowrap; }
  .resident-header-actions { gap: 5px; margin-right: 6px; }
  .resident-header-actions .resident-menu-button { padding: 3px 5px; font-size: 11px; }
  .resident-header-clock { gap: 6px; margin-left: 2px; padding: 5px 7px; font-size: 10px; }
  .header .user-info { flex-shrink: 0; gap: 7px; white-space: nowrap; }
  .header .user-role { padding: 3px 6px; font-size: 10px; }
  .header .user-name { font-size: 11px; }
  .header .logout-btn { flex-shrink: 0; padding: 6px 9px; font-size: 11px; white-space: nowrap; }
}

@media (max-width: 650px) {
  .header { gap: 7px; padding-inline: 8px; }
  .header .logo { font-size: 14px; }
  .resident-header-actions { margin-left: auto; margin-right: 2px; }
  .resident-header-clock span:first-child,
  .resident-header-clock i,
  .header .user-role,
  .header .user-name { display: none; }
}

.resident-menu-wrap {
  position: relative;
}

.resident-header-actions .resident-menu-button {
  display: flex;
  align-items: center;
  gap: 7px;
  background: transparent;
}

.resident-header-actions .resident-menu-button:hover {
  background: transparent;
}

.resident-dropdown-menu {
  position: absolute;
  z-index: 100;
  top: calc(100% + 10px);
  left: 0;
  display: grid;
  width: 190px;
  overflow: hidden;
  padding: 7px;
  border: 1px solid #cbddeb;
  border-radius: 12px;
  background: rgba(255, 255, 255, .98);
  box-shadow: 0 12px 28px rgba(31, 68, 103, .2);
}

.resident-dropdown-menu a {
  padding: 11px 13px;
  border-radius: 8px;
  color: #294966;
  font-size: 13px;
  font-weight: 700;
  text-decoration: none;
  white-space: nowrap;
}

.resident-dropdown-menu a:hover,
.resident-dropdown-menu a.menu-active {
  color: #1768bd;
  background: #e8f4ff;
}

.resident-header-actions button {
  padding: 8px 13px;
  border: 1px solid transparent;
  border-radius: 9px;
  color: #ffffff;
  font-size: 13px;
  font-weight: 700;
  cursor: pointer;
}

.resident-header-clock {
  margin-left: 5px;
  padding: 7px 11px;
  display: flex;
  align-items: center;
  gap: 9px;
  border-radius: 10px;
  color: #38536d;
  background: #edf8ff;
  font-size: 12px;
  font-weight: 700;
  white-space: nowrap;
}

.resident-header-clock i {
  width: 1px;
  height: 14px;
  background: #b8d8ea;
}

.admin-header-clock {
  margin-left: auto;
  margin-right: 20px;
  padding: 7px 12px;
  display: flex;
  align-items: center;
  gap: 10px;
  border: 1px solid #596168;
  color: #f4f6f7;
  background: #30363b;
  font-size: 12px;
  font-weight: 700;
  white-space: nowrap;
}

.admin-header-clock i {
  width: 1px;
  height: 14px;
  background: #69737b;
}

.header-demo-reset-button {
  height: 25px;
  margin-left: 3px;
  padding: 0 9px;
  border: 1px solid #7a858e;
  border-radius: 2px;
  cursor: pointer;
  color: #ffffff;
  background: #414950;
  font-size: 11px;
  font-weight: 800;
  white-space: nowrap;
}

.header-demo-reset-button:hover {
  border-color: #ffffff;
  background: #505a63;
}

.header-demo-reset-button:disabled {
  cursor: not-allowed;
  opacity: 0.55;
}

.resident-header-actions .resident-menu-button {
  min-height: auto;
  padding: 4px 6px;
  border: 0;
  border-radius: 0;
  color: #315c86;
  background: transparent;
  font-weight: 700;
}

.resident-header-actions .resident-menu-button:hover {
  color: #1685c7;
  background: transparent;
  text-decoration: underline;
  text-underline-offset: 4px;
}

.history-nav {
    display: flex;
    align-items: center;
    gap: 8px;
    margin-right: 16px;
}

.history-nav button {
    width: 34px;
    height: 34px;
}
</style>
