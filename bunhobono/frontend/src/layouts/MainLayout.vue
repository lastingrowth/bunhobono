<template>
    <div class="layout"
    :class="{
        'admin-layout': route.path.startsWith('/admin'),
        'admin-statistics-layout': route.path === '/admin/statistics',
        'resident-layout': isResidentRoute,
        'resident-menu-visible': showResidentFloatingMenu,
        'resident-welcome-layout': route.name === 'ResidentWelcome',
    }">
        <Header
            :show-sidebar-toggle="!isResidentRoute"
            @toggle-sidebar="toggleSidebar"/>

        <div class="container">
            <Sidebar v-if="!isResidentRoute" :collapsed="sidebarCollapsed"/>

            <main class="content">
                <RouterView/>
            </main>
            
            <ResidentFloatingMenu v-if="showResidentFloatingMenu"/>
        </div>

        <ResidentFooter v-if="isResidentRoute"/>

        <nav v-if="showResidentMobileNav" class="resident-mobile-bottom-nav" aria-label="모바일 주요 메뉴">
            <RouterLink to="/resident/dashboard" :class="{ active: route.path === '/resident/dashboard' }">HOME</RouterLink>
            <RouterLink to="/resident/inquiries" :class="{ active: route.path.startsWith('/resident/inquiries') }">1:1 문의</RouterLink>
            <button type="button" @click="openAiChat">AI 챗봇</button>
            <RouterLink to="/resident/mypage" :class="{ active: route.path.startsWith('/resident/mypage') }">마이페이지</RouterLink>
        </nav>

        <!-- 입주민 화면 AI 챗봇 -->
        <AiChatWidget v-if="isResidentRoute"/>
    </div>
</template>

<script setup lang="ts">
import Header from '@/components/Header.vue';
import ResidentFloatingMenu from '@/components/ResidentFloatingMenu.vue';
import ResidentFooter from '@/components/ResidentFooter.vue';
import Sidebar from '@/components/Sidebar.vue';
import AiChatWidget from '@/features/ai-chat/AiChatWidget.vue';
import { computed, ref } from 'vue';
import { useRoute } from 'vue-router';

const route = useRoute();
const sidebarCollapsed = ref(false)
const isResidentRoute = computed(() => route.path.startsWith('/resident'))
const showResidentMobileNav = computed(() => isResidentRoute.value && route.name !== 'ResidentWelcome')

const showResidentFloatingMenu = computed(() => {

    const path = route.path;
    const mode = route.query.mode;

    // 표시 허용 페이지
    if (path === "/resident/dashboard") {
        return true;
    }

    if (path === "/resident/exit-request") {
        return true;
    }

    if (path === "/resident/mypage") {
        return true;
    }

    if (path === "/resident/vehicles") {
        // 차량관리 + 차량알림만 표시
        return !mode || mode === "notification";
    }

    if (path === "/resident/carlogs") {
        return true;
    }

    if (path.startsWith("/resident/boards")) {
        return true;
    }

    if (path.startsWith("/resident/inquiries")) {
        return true;
    }

    if (path.startsWith("/resident/parkings")) {
        return true;
    }

    return false;

});

function toggleSidebar() {
    sidebarCollapsed.value = !sidebarCollapsed.value
}

function openAiChat() {
    window.dispatchEvent(new CustomEvent('open-ai-chat'))
}
</script>

<style scoped>
.resident-mobile-bottom-nav { display: none; }

@media (any-pointer: coarse) and (max-width: 820px),
       (any-pointer: coarse) and (max-height: 820px) {
    .resident-mobile-bottom-nav {
        position: fixed;
        z-index: 1100;
        right: 0;
        bottom: 0;
        left: 0;
        display: grid;
        grid-template-columns: repeat(4, minmax(0, 1fr));
        padding: 14px max(8px, env(safe-area-inset-right)) calc(14px + env(safe-area-inset-bottom)) max(8px, env(safe-area-inset-left));
        border-top: 1px solid #0f64b3;
        background: #1677d2;
        box-shadow: 0 -7px 20px rgba(18, 75, 128, .2);
        backdrop-filter: blur(12px);
    }

    .resident-mobile-bottom-nav :is(a, button) {
        min-width: 0;
        min-height: 52px;
        display: flex;
        align-items: center;
        justify-content: center;
        padding: 8px 3px;
        border: 0;
        color: #ffffff;
        background: transparent;
        font: inherit;
        font-size: 17px;
        font-weight: 950;
        text-align: center;
        text-decoration: none;
    }

    .resident-mobile-bottom-nav .active {
        color: #ffffff;
        font-weight: 950;
    }
}
</style>
