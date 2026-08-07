<template>
    <nav
        class="resident-floating-menu"
        :style="{ top: `${menuTop}px` }"
    >
        <RouterLink to="/resident/mypage"
            :class="{
                active: route.path === '/resident/mypage'
            }">
            마이페이지
        </RouterLink>

        <RouterLink to="/resident/vehicles"
            :class="{
                active: route.path === '/resident/vehicles' && !route.query.mode
            }">
            차량관리
        </RouterLink>

        <RouterLink to="/resident/carlogs"
            :class="{
                active: route.path === '/resident/carlogs'
            }">
            입출차기록
        </RouterLink>

        <RouterLink to="/resident/boards"
            :class="{
                active: route.path.startsWith('/resident/boards')
            }">
            공지사항
        </RouterLink>

        <RouterLink to="/resident/inquiries"
            :class="{
                active: route.path.startsWith('/resident/inquiries')
            }">
            1:1 문의
        </RouterLink>

        <RouterLink to="/resident/vehicles?mode=notification"
            :class="{
                active: route.path === '/resident/vehicles'
                && route.query.mode === 'notification'
            }">
            차량알림
        </RouterLink>
    </nav>
</template>

<script setup>
import { nextTick, onMounted, onUnmounted, ref, watch } from 'vue';
import { useRoute } from 'vue-router';

const route = useRoute()
const menuTop = ref(0)

let currentTop = 0
let animationFrame = 0

function getTargetTop() {
    const header = document.querySelector('.resident-layout .header')
    const headerBottom = header?.getBoundingClientRect().bottom ?? 60

    return window.scrollY + headerBottom + 36
}

function followScroll() {
    const targetTop = getTargetTop()
    const distance = targetTop - currentTop

    currentTop += distance * 0.16
    // 빠르게 아래로 스크롤해도 메뉴가 헤더 영역으로 올라가지 않도록
    // 현재 화면에서 허용되는 최소 위치보다 항상 아래에 표시합니다.
    menuTop.value = Math.max(currentTop, targetTop)

    if (Math.abs(distance) < 0.5) {
        currentTop = targetTop
        menuTop.value = targetTop
        animationFrame = 0
        return
    }

    animationFrame = window.requestAnimationFrame(followScroll)
}

function startFollowing() {
    if (animationFrame) return
    animationFrame = window.requestAnimationFrame(followScroll)
}

function resetMenuPosition() {
    window.cancelAnimationFrame(animationFrame)
    animationFrame = 0
    currentTop = getTargetTop()
    menuTop.value = currentTop
}

watch(
    () => route.fullPath,
    async () => {
        await nextTick()
        window.requestAnimationFrame(resetMenuPosition)
    }
)

onMounted(() => {
    resetMenuPosition()
    window.addEventListener('scroll', startFollowing, { passive: true })
    window.addEventListener('resize', resetMenuPosition)
})

onUnmounted(() => {
    window.cancelAnimationFrame(animationFrame)
    window.removeEventListener('scroll', startFollowing)
    window.removeEventListener('resize', resetMenuPosition)
})

</script>

<style scoped>

/* 메뉴 위치 */
.resident-floating-menu {
    /*
     * 페이지별 콘텐츠 크기나 렌더링 시점과 무관하게 같은 좌측 좌표를 사용합니다.
     * top은 현재 스크롤 위치를 부드럽게 따라가도록 스크립트에서 갱신합니다.
     */
    position: absolute;
    left: clamp(12px, 4vw, 60px);
    will-change: top;

    display: flex;
    flex-direction: column;
    gap: 10px;

    padding: 12px;

    border-radius: 16px;
    background: rgba(255,255,255,.95);
    border: 1px solid #dce8f2;
    box-shadow: 0 10px 25px rgba(40,80,120,.15);

    z-index: 1000;
}

/* 메뉴 */
.resident-floating-menu a {
    width: 100px;

    padding: 14px 10px;

    display: flex;
    justify-content: center;
    align-items: center;

    border-radius: 12px;

    color: #38536d;
    background: #f7fbff;

    text-decoration: none;

    font-size: 14px;
    font-weight: 700;

    transition: .2s;
}

/* 현재 페이지 */
.resident-floating-menu a.active {
    color: #1768bd;
    background: #eaf4ff;
}

/* hover */
.resident-floating-menu a:hover {
    background: #eaf4ff;
    transform: translateX(-3px);
}

@media(max-width:900px) {

    .resident-floating-menu {
        left: 12px;
    }

    .resident-floating-menu a {
        width: 85px;
        padding: 12px 5px;
        font-size: 12px;
    }
}

</style>
