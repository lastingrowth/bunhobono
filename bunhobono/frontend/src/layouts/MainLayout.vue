<template>
    <div class="layout"
    :class="{
        'admin-layout': route.path.startsWith('/admin'),
        'resident-layout': isResidentRoute,
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
    </div>
</template>

<script setup lang="ts">
import Header from '@/components/Header.vue';
import ResidentFloatingMenu from '@/components/ResidentFloatingMenu.vue';
import ResidentFooter from '@/components/ResidentFooter.vue';
import Sidebar from '@/components/Sidebar.vue';
import { computed, ref } from 'vue';
import { useRoute } from 'vue-router';

const route = useRoute();
const sidebarCollapsed = ref(false)
const isResidentRoute = computed(() => route.path.startsWith('/resident'))

const showResidentFloatingMenu = computed(() => {

    const path = route.path;
    const mode = route.query.mode;

    // 표시 허용 페이지
    if (path === "/resident/dashboard") {
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

    return false;

});

function toggleSidebar() {
    sidebarCollapsed.value = !sidebarCollapsed.value
}
</script>
