<template>
    <aside class="sidebar" :class="{collapsed}">
        <RouterLink
            v-for="menu in menuList"
            :key="menu.path"
            :to="menu.path"
            :title="collapsed ? menu.title : ''"
            class="menu-item"
            :class="{ 'router-link-active': isMenuActive(menu) }"
            active-class="route-record-active"
            exact-active-class="route-record-exact-active">

            <span v-if="!collapsed" class="menu-title">
                {{ menu.title }}
            </span>

            <span
                v-if="!collapsed && menu.path === PDM_MENU_PATH && faultCount > 0"
                class="menu-alert-count"
                :title="`현재 고장 설비 ${faultCount}대`"
                :aria-label="`현재 고장 설비 ${faultCount}대`">
                {{ faultCount }}
            </span>

        </RouterLink>
    </aside>
</template>

<script setup>
import { useJwtStore } from '@/features/login/jwtStore';
import {
    getLatestCameraPdm,
    getLatestGatePdm,
    getLatestRobotPdm,
    PDM_ACTION_COMPLETED_EVENT
} from '@/features/predictive-maintenance/predictiveMaintenanceApi';
import { adminMenu, residentMenu } from '@/router/menu';
import { computed, onBeforeUnmount, ref, watch } from 'vue';
import { useRoute, useRouter } from 'vue-router';

const PDM_MENU_PATH = '/admin/predictive-maintenance'
const FAULT_REFRESH_INTERVAL = 30_000

defineProps({
    collapsed : {
        type : Boolean,
        default : false
    }
})

const jwtStore = useJwtStore()
const route = useRoute()
const router = useRouter()
const faultCount = ref(0)
let faultRefreshTimer = null
let isRefreshingFaultCount = false
let faultRefreshRequested = false

const isFaultRisk = (riskLevel) => {
    const normalized = String(riskLevel || '').trim().toUpperCase()
    return ['위험', 'FAULT', 'CRITICAL', 'DANGER'].includes(normalized)
}

const isActionableFault = (record) => (
    isFaultRisk(record?.riskLevel) && record?.actionStatus !== 'COMPLETED'
)

const refreshFaultCount = async () => {
    if (jwtStore.role !== 'ADMIN') {
        return
    }

    if (isRefreshingFaultCount) {
        faultRefreshRequested = true
        return
    }

    isRefreshingFaultCount = true

    try {
        const responses = await Promise.allSettled([
            getLatestCameraPdm(),
            getLatestGatePdm(),
            getLatestRobotPdm()
        ])

        const successfulResponses = responses.filter(
            (result) => result.status === 'fulfilled'
        )

        if (successfulResponses.length === 0) {
            return
        }

        faultCount.value = successfulResponses.reduce((total, result) => {
            const records = Array.isArray(result.value.data) ? result.value.data : []
            return total + records.filter(isActionableFault).length
        }, 0)
    } catch (error) {
        console.error('사이드바 예지보전 고장 대수 조회 실패', error)
    } finally {
        isRefreshingFaultCount = false

        if (faultRefreshRequested) {
            faultRefreshRequested = false
            void refreshFaultCount()
        }
    }
}

const stopFaultRefresh = () => {
    if (faultRefreshTimer) {
        window.clearInterval(faultRefreshTimer)
        faultRefreshTimer = null
    }
}

const startFaultRefresh = () => {
    stopFaultRefresh()
    refreshFaultCount()
    faultRefreshTimer = window.setInterval(refreshFaultCount, FAULT_REFRESH_INTERVAL)
}

const refreshOnFocus = () => refreshFaultCount()
const refreshAfterActionCompleted = () => refreshFaultCount()

watch(
    () => jwtStore.role,
    (role) => {
        if (role === 'ADMIN') {
            startFaultRefresh()
            window.addEventListener('focus', refreshOnFocus)
            window.addEventListener(PDM_ACTION_COMPLETED_EVENT, refreshAfterActionCompleted)
            return
        }

        stopFaultRefresh()
        window.removeEventListener('focus', refreshOnFocus)
        window.removeEventListener(PDM_ACTION_COMPLETED_EVENT, refreshAfterActionCompleted)
        faultCount.value = 0
    },
    { immediate: true }
)

watch(() => route.fullPath, refreshFaultCount)

onBeforeUnmount(() => {
    stopFaultRefresh()
    window.removeEventListener('focus', refreshOnFocus)
    window.removeEventListener(PDM_ACTION_COMPLETED_EVENT, refreshAfterActionCompleted)
})

// 같은 차량 관리 경로에서는 type 값까지 비교해 해당 메뉴만 활성화한다.
const isMenuActive = (menu) => {
    const target = router.resolve(menu.path)
    const isSamePath = route.path === target.path
    const isChildPath = route.path.startsWith(`${target.path}/`)

    if (!isSamePath && !isChildPath) {
        return false
    }

    if (target.query.type) {
        return isSamePath && route.query.type === target.query.type
    }

    return true
}

const menuList = computed(() => {
    if (jwtStore.role === 'ADMIN') {
        return adminMenu
    }

    if (jwtStore.role === 'RESIDENT') {
        return residentMenu
    }

    return [];
})
</script>
