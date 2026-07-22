<template>
    <header class="header">
        <div class="header-left">
            <button v-if="showSidebarToggle" class="sidebar-toggle" type="button"
                    aria-label="사이드바 열기 및 닫기" @click="$emit('toggle-sidebar')">
                ☰
            </button>
        
            <h1 class="logo"><RouterLink :to="homePath">아파트 주차관리 시스템</RouterLink></h1>
        </div>

        <div v-if="isResidentDashboard" class="resident-header-actions">
            <button type="button" class="resident-refresh-button" @click="refreshResidentDashboard">새로고침</button>
            <button type="button" class="resident-home-button" @click="goResidentHome">초기화면</button>
            <div class="resident-header-clock">
                <span>▣&nbsp; {{ formattedDate }}</span>
                <i></i>
                <span>◷&nbsp; {{ formattedTime }}</span>
            </div>
        </div>

        <div v-if="isAdminRoute" class="admin-header-clock">
            <span>▣&nbsp; {{ formattedDate }}</span>
            <i></i>
            <span>◷&nbsp; {{ formattedTime }}</span>
        </div>

        <div class="user-info">
            <span class="user-role">{{ jwtStore.role }}</span>
            <span class="divider">|</span>
            <span class="user-name">{{ jwtStore.userId }}</span>

            <button class="logout-btn" @click="logout">로그아웃</button>
        </div>

    </header>
</template>

<script setup>
import { useJwtStore } from '@/features/login/jwtStore';
import { computed, onMounted, onUnmounted, ref } from 'vue';
import { useRoute, useRouter } from 'vue-router';

const jwtStore = useJwtStore()
const route = useRoute()
const router = useRouter()
const now = ref(new Date())
let clockTimer

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
const formattedDate = computed(() => new Intl.DateTimeFormat('ko-KR', {
    year: 'numeric', month: '2-digit', day: '2-digit', weekday: 'short',
}).format(now.value))
const formattedTime = computed(() => new Intl.DateTimeFormat('ko-KR', {
    hour: 'numeric', minute: '2-digit', hour12: true,
}).format(now.value))

function refreshResidentDashboard() {
    if (route.path === '/resident/dashboard') {
        window.dispatchEvent(new CustomEvent('resident-dashboard-refresh'))
        return
    }

    router.go(0)
}

function goResidentHome() {
    router.push('/resident')
}

function logout() {
    jwtStore.logout()
}

onMounted(() => {
    clockTimer = window.setInterval(() => { now.value = new Date() }, 1000)
})

onUnmounted(() => window.clearInterval(clockTimer))

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
  margin-left: auto;
  margin-right: 18px;
  display: flex;
  align-items: center;
  gap: 7px;
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

.resident-refresh-button {
  background: #42d77d;
}

.resident-home-button {
  background: #45bff2;
}

.resident-refresh-button:hover {
  background: #2fc86b;
}

.resident-home-button:hover {
  background: #2eafe8;
}
</style>
