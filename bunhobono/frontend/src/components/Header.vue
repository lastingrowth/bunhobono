<template>
    <header class="header">
        <div class="header-left">
            <button class="sidebar-toggle" type="button"
                    aria-label="사이드바 열기 및 닫기" @click="$emit('toggle-sidebar')">
                ☰
            </button>
        
            <h1 class="logo"><RouterLink to="/admin">아파트 주차관리 시스템</RouterLink></h1>
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

const jwtStore = useJwtStore()

function logout() {
    jwtStore.logout()
}

const emit = defineEmits([
    'toggle-sidebar'
])
</script>

<style scoped>
.logo a {
  color: #0f3b73;
  text-decoration: none;
  font-weight: 800;
}

.logo a:hover {
  color: #0b63ce;
  text-decoration: none;
}
</style>