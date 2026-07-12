<template>
    <aside class="sidebar" :class="{collapsed}">
        <RouterLink
            v-for="menu in menuList"
            :key="menu.path"
            :to="menu.path"
            :title="collapsed ? menu.title : ''"
            class="menu-item">

            <span class="menu-icon">
                {{ menu.icon }}
            </span>

            <span v-if="!collapsed" class="menu-title">
                {{ menu.title }}
            </span>

        </RouterLink>
    </aside>
</template>

<script setup>
import { useJwtStore } from '@/features/login/jwtStore';
import { adminMenu, residentMenu } from '@/router/menu';
import { computed } from 'vue';

defineProps({
    collapsed : {
        type : Boolean,
        default : false
    }
})

const jwtStore = useJwtStore()

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