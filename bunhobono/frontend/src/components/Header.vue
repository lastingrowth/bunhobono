<template>
    <header class="header">
        <div class="header-left">
            <button v-if="showSidebarToggle" class="sidebar-toggle" type="button"
                    aria-label="사이드바 열기 및 닫기" @click="$emit('toggle-sidebar')">
                ☰
            </button>
        
            <h1 class="logo"><RouterLink :to="homePath">아파트 주차관리 시스템</RouterLink></h1>
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
import { computed } from 'vue';

const jwtStore = useJwtStore()

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

function logout() {
    jwtStore.logout()
}

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
</style>
