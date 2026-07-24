<template>
    <nav
        ref="floatingMenu"
        class="resident-floating-menu"
        :style="{
            left: `${menuLeft}px`,
            transform: 'translateY(-50%)'
        }"
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

const menuLeft = ref(0)
const floatingMenu = ref(null)

// resident 화면마다 콘텐츠 폭이 다르므로 현재 콘텐츠의 오른쪽 끝을 기준으로 배치합니다.
function updateMenuPosition() {
    const content = document.querySelector('.resident-layout .content > *')
    const menu = floatingMenu.value

    if (!content || !menu) return

    const contentRect = content.getBoundingClientRect()
    // 본문 카드와 메뉴 사이에 24px 간격을 둡니다.
    const desiredLeft = contentRect.right + window.scrollX + 24
    const maximumLeft = window.innerWidth - menu.offsetWidth - 12

    menuLeft.value = Math.max(12, Math.min(desiredLeft, maximumLeft))
}

watch(
    () => route.fullPath,
    async () => {
        await nextTick()
        window.requestAnimationFrame(updateMenuPosition)
    }
)

onMounted(async () => {
    window.addEventListener('resize', updateMenuPosition)

    await nextTick()
    window.requestAnimationFrame(updateMenuPosition)
})

onUnmounted(() => {
    window.removeEventListener('resize', updateMenuPosition)
})

</script>

<style scoped>

/* 메뉴 위치 */
.resident-floating-menu {
    /* 화면에 고정하지 않고 resident 본문 영역 옆에 배치합니다. */
    position: absolute;
    /* 실제 left 값은 현재 resident 콘텐츠의 오른쪽 끝을 기준으로 계산합니다. */
    right: auto;
    top: 50%;

    transition: transform 0.35s ease;

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
        right: auto;
    }

    .resident-floating-menu a {
        width: 85px;
        padding: 12px 5px;
        font-size: 12px;
    }
}

</style>
