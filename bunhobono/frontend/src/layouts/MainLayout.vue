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
        </div>

        <ResidentFooter v-if="isResidentRoute"/>
    </div>
</template>

<script setup lang="ts">
import Header from '@/components/Header.vue';
import ResidentFooter from '@/components/ResidentFooter.vue';
import Sidebar from '@/components/Sidebar.vue';
import { computed, ref } from 'vue';
import { useRoute } from 'vue-router';

const route = useRoute();
const sidebarCollapsed = ref(false)
const isResidentRoute = computed(() => route.path.startsWith('/resident'))

function toggleSidebar() {
    sidebarCollapsed.value = !sidebarCollapsed.value
}
</script>
