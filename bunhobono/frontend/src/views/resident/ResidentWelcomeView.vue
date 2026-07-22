<template>
    <section class="resident-welcome">
        <article class="welcome-card">
            <div class="welcome-content">
                <h2>{{ memberName }}님 반갑습니다.</h2>
                <p><br/></p>

                <div class="welcome-actions">
                    <button type="button" @click="goDashboard">홈페이지로</button>
                    <button type="button" @click="goVisitVehicleForm">방문차량 등록</button>
                </div>
                <p v-if="visitRegistrationMessage" class="visit-registration-message">
                    {{ visitRegistrationMessage }}
                </p>
                <RouterLink
                    v-if="visitRegistrationMessage"
                    class="vehicle-list-text-link"
                    to="/resident/vehicles"
                >
                    차량 목록으로 가기
                </RouterLink>
            </div>
        </article>
    </section>
</template>

<script setup>
import { computed, onMounted, ref } from "vue";
import { useRouter } from "vue-router";
import { useMemStore } from "@/features/member/memStore";
import { useResVehicleStore } from "@/features/resVehicle/resVehicleStore";

const router = useRouter();
const memberStore = useMemStore();
const resVehicleStore = useResVehicleStore();
const visitRegistrationMessage = ref("");
const memberName = computed(() => memberStore.member.memName || "입주민");

const goVisitVehicleForm = async () => {
    await resVehicleStore.loadVehicleList();

    if (resVehicleStore.hasActiveVisitVehicle) {
        visitRegistrationMessage.value = "방문차량이 이미 등록되어있습니다";
        return;
    }

    visitRegistrationMessage.value = "";
    router.push("/resident/vehicles?mode=form");
};
const goDashboard = () => router.push("/resident/dashboard");

onMounted(async () => {
    await memberStore.loadMypage();
});
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
        linear-gradient(180deg, rgba(248, 252, 255, 0.44) 0%, rgba(250, 253, 255, 0.63) 45%, rgba(255, 255, 255, 0.81) 75%, rgba(255, 255, 255, 0.91) 100%),
        url('@/assets/images/back.jpg') center center / cover fixed no-repeat;
}

.welcome-card {
    position: relative;
    width: min(760px, 100%);
    padding: 64px 44px;
    border: 1px solid rgba(255, 255, 255, 0.7);
    border-radius: 24px;
    background: rgba(255, 255, 255, 0.68);
    box-shadow: 0 22px 50px rgba(9, 31, 52, 0.28);
    text-align: center;
    backdrop-filter: blur(6px);
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

.visit-registration-message {
    margin: 18px 0 0 !important;
    color: #d13b45 !important;
    font-size: 15px !important;
    font-weight: 700 !important;
}

.vehicle-list-text-link {
    display: inline-block;
    margin-top: 8px;
    color: #287fd5;
    font-size: 14px;
    font-weight: 700;
    text-decoration: underline;
    text-underline-offset: 4px;
    cursor: pointer;
}

.vehicle-list-text-link:hover {
    color: #175fa9;
}

@media (max-width: 560px) {
    .resident-welcome {
        padding: 16px;
    }

    .welcome-card {
        padding: 50px 24px 40px;
    }

    .welcome-actions {
        grid-template-columns: 1fr;
    }
}
</style>
