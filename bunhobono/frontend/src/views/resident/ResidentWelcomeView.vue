<template>
    <section class="resident-welcome">
        <article class="welcome-card">
            <div class="welcome-date-time">
                <span>{{ formattedDate }}</span>
                <span>{{ formattedTime }}</span>
            </div>

            <div class="welcome-content">
                <h2>{{ memberName }}님 반갑습니다.</h2>
                <p><br/></p>

                <div class="welcome-actions">
                    <button type="button" @click="goDashboard">홈페이지로</button>
                    <button type="button" @click="goVisitVehicleForm">방문차량 등록</button>
                </div>
            </div>
        </article>
    </section>
</template>

<script setup>
import { computed, onMounted, onUnmounted, ref } from "vue";
import { useRouter } from "vue-router";
import { useMemStore } from "@/features/member/memStore";

const router = useRouter();
const memberStore = useMemStore();
const now = ref(new Date());
let clockTimer;

const memberName = computed(() => memberStore.member.memName || "입주민");
const formattedDate = computed(() => new Intl.DateTimeFormat("ko-KR", {
    year: "numeric", month: "2-digit", day: "2-digit", weekday: "short",
}).format(now.value));
const formattedTime = computed(() => new Intl.DateTimeFormat("ko-KR", {
    hour: "numeric", minute: "2-digit", hour12: true,
}).format(now.value));

const goVisitVehicleForm = () => router.push("/resident/vehicles?mode=form")
const goDashboard = () => router.push("/resident/dashboard");

onMounted(async () => {
    clockTimer = window.setInterval(() => { now.value = new Date(); }, 1000);
    await memberStore.loadMypage();
});
onUnmounted(() => window.clearInterval(clockTimer));
</script>

<style scoped>
:global(.content:has(.resident-welcome)) {
    padding: 0;
}

.resident-welcome {
    min-height: calc(100vh - var(--header-height));
    display: grid;
    place-items: center;
    padding: 24px;
    background:
        linear-gradient(rgba(13, 35, 55, 0.2), rgba(13, 35, 55, 0.32)),
        url('@/assets/images/back.jpg') center center / cover no-repeat;
}

.welcome-card {
    position: relative;
    width: min(620px, 100%);
    padding: 96px 44px 64px;
    border: 1px solid rgba(255, 255, 255, 0.7);
    border-radius: 24px;
    background: rgba(255, 255, 255, 0.68);
    box-shadow: 0 22px 50px rgba(9, 31, 52, 0.28);
    text-align: center;
    backdrop-filter: blur(6px);
}

.welcome-date-time {
    position: absolute;
    top: 22px;
    right: 26px;
    display: flex;
    gap: 14px;
    padding: 9px 14px;
    border: 1px solid rgba(185, 207, 231, 0.9);
    border-radius: 12px;
    color: #234a73;
    background: rgba(255, 255, 255, 0.72);
    font-size: 14px;
    font-weight: 700;
}

.welcome-content h2 {
    margin: 0 0 10px;
    color: #173b63;
    font-size: clamp(28px, 3vw, 38px);
}

.welcome-content p {
    margin: 0 0 34px;
    color: #486581;
    font-size: 18px;
    font-weight: 600;
}

.welcome-actions {
    display: grid;
    grid-template-columns: 1fr 1fr;
    gap: 16px;
}

.welcome-actions button {
    min-height: 62px;
    border: 1px solid #1f6fd1;
    border-radius: 14px;
    color: #fff;
    background: #2f7ddd;
    font-size: 18px;
    font-weight: 700;
    cursor: pointer;
}

.welcome-actions button:hover {
    background: #1e63bd;
}

@media (max-width: 560px) {
    .resident-welcome {
        padding: 16px;
    }

    .welcome-card {
        padding: 105px 24px 40px;
    }

    .welcome-date-time {
        right: 20px;
        left: 20px;
        justify-content: center;
    }

    .welcome-actions {
        grid-template-columns: 1fr;
    }
}
</style>
