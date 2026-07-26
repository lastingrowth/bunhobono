<template>
    <nav
        ref="floatingMenu"
        class="resident-floating-menu"
        :style="{
            left: `${menuLeft}px`,
            top: `${menuTop}px`
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

        <RouterLink to="/resident/boards"
            :class="{
                active: route.path.startsWith('/resident/boards')
            }">
            공지사항
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
const menuTop = ref(0)
const floatingMenu = ref(null)
let contentResizeObserver = null

function getContentAnchor() {
    const selectors = [
        '.resident-layout .resident-board',
        '.resident-layout .mypage-card',
        '.resident-layout .resident-board-list-page',
        '.resident-layout .resident-vehicle-management',
        '.resident-layout .content > *',
    ]

    return selectors
        .map((selector) => document.querySelector(selector))
        .find(Boolean)
}

// 화면 왼쪽 끝과 콘텐츠 카드 왼쪽 끝 사이의 중앙에 메뉴를 배치합니다.
function updateMenuPosition() {
    const content = getContentAnchor()
    const header = document.querySelector('.resident-layout .header')
    const menu = floatingMenu.value

    if (!content || !menu) return

    const contentRect = content.getBoundingClientRect()
    const headerBottom = header?.getBoundingClientRect().bottom ?? 60
    const availableWidth = Math.max(0, contentRect.left)
    const desiredLeft = (availableWidth - menu.offsetWidth) / 2
    const maximumLeft = window.innerWidth - menu.offsetWidth - 12

    menuLeft.value = Math.max(12, Math.min(desiredLeft, maximumLeft))
    menuTop.value = Math.round(headerBottom + 20)
}

function observeCurrentContent() {
    contentResizeObserver?.disconnect()

    const content = getContentAnchor()

    if (!content) return

    contentResizeObserver = new ResizeObserver(updateMenuPosition)
    contentResizeObserver.observe(content)
}

watch(
    () => route.fullPath,
    async () => {
        await nextTick()
        observeCurrentContent()
        window.requestAnimationFrame(updateMenuPosition)
    }
)

onMounted(async () => {
    window.addEventListener('resize', updateMenuPosition)

    await nextTick()
    observeCurrentContent()
    window.requestAnimationFrame(updateMenuPosition)
})

onUnmounted(() => {
    window.removeEventListener('resize', updateMenuPosition)
    contentResizeObserver?.disconnect()
})

</script>

<style scoped>

/* 메뉴 위치 */
.resident-floating-menu {
    /* 헤더 아래 20px 위치에서 스크롤과 무관하게 고정합니다. */
    position: fixed;
    right: auto;
    transition: left 0.2s ease;

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
