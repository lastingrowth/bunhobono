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
import { useRoute, useRouter } from 'vue-router';

defineProps({
    collapsed : {
        type : Boolean,
        default : false
    }
})

const jwtStore = useJwtStore()
const route = useRoute()
const router = useRouter()

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
