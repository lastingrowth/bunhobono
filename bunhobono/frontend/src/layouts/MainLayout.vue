<template>
    <div class="layout"
    :class="{ 'admin-layout': route.path.startsWith('/admin') }">
        <Header @toggle-sidebar="toggleSidebar"/>

        <div class="container">
            <Sidebar :collapsed="sidebarCollapsed"/>

            <main class="content">
                <RouterView/>
            </main>
        </div>
    </div>
</template>

<script setup lang="ts">
import Header from '@/components/Header.vue';
import Sidebar from '@/components/Sidebar.vue';
import { ref } from 'vue';
import { useRoute } from 'vue-router';

const route = useRoute();
const sidebarCollapsed = ref(false)

function toggleSidebar() {
    sidebarCollapsed.value = !sidebarCollapsed.value
}
</script>