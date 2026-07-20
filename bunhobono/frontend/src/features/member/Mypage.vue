<template>
    <header class="detail-header">
        <h2>마이페이지</h2>

        <div class="detail-actions">
            <button @click="goHome">홈</button>
            <button @click="goModify">회원정보수정</button>
            <button @click="openDeleteConfirm">회원탈퇴</button>
        </div>
    </header>

    <!-- 비밀번호는 조회 응답과 마이페이지 화면에 표시하지 않는다. -->
    <table border="">
        <tbody>
            <tr>
                <th>가입유형</th>
                <td>{{ store.member.role }}</td>
            </tr>
            <tr>
                <th>이름</th>
                <td>{{ store.member.memName }}</td>
            </tr>
            <tr>
                <th>동</th>
                <td>{{ store.member.memDong }}</td>
            </tr>
            <tr>
                <th>호수</th>
                <td>{{ store.member.memHo }}</td>
            </tr>
            <tr>
                <th>연락처</th>
                <td>{{ store.member.memPhone }}</td>
            </tr>
            <tr>
                <th>아이디</th>
                <td>{{ store.member.loginId }}</td>
            </tr>
            <tr>
                <th>상태</th>
                <td>{{ store.member.memStatus }}</td>
            </tr>
        </tbody>
    </table>

    <div v-if="showDeleteConfirm" class="security-modal" @click.self="closeDeleteConfirm">
        <section class="security-dialog">
            <h3>회원탈퇴 확인</h3>
            <p>탈퇴하면 즉시 로그아웃되며 다시 로그인할 수 없습니다.</p>

            <label>
                <span>현재 비밀번호</span>
                <input v-model="currentPassword" type="password" autocomplete="current-password">
            </label>

            <div class="captcha-box">
                <img :src="challengeImage" alt="보안문자">
                <button type="button" @click="loadChallenge">새로고침</button>
            </div>
            <p class="captcha-timer" :class="{ expired: challengeRemainingSeconds === 0 }">
                보안문자 유효시간: {{ challengeTimeLabel }}
            </p>

            <label>
                <span>보안문자 입력</span>
                <input v-model.trim="challengeAnswer" type="text" maxlength="5" autocomplete="off" :disabled="challengeRemainingSeconds === 0">
            </label>

            <div class="security-actions">
                <button type="button" @click="closeDeleteConfirm">취소</button>
                <button type="button" :disabled="deleteLoading" @click="goDelete">
                    {{ deleteLoading ? "처리 중" : "회원탈퇴" }}
                </button>
            </div>
        </section>
    </div>
</template>

<script setup>
import { computed, onBeforeUnmount, onMounted, ref } from "vue";
import { useRouter } from "vue-router";
import { useMemStore } from "./memStore";
import { useJwtStore } from "@/features/login/jwtStore";



const router = useRouter();
const store = useMemStore();
const jwtStore = useJwtStore();

// 로그인한 사용자 아이디
const showDeleteConfirm = ref(false);
const currentPassword = ref("");
const challengeId = ref("");
const challengeImage = ref("");
const challengeAnswer = ref("");
const challengeRemainingSeconds = ref(0);
const deleteLoading = ref(false);
let challengeTimer = null;

const challengeTimeLabel = computed(() => {
    if (challengeRemainingSeconds.value <= 0) return "만료됨";
    const minutes = Math.floor(challengeRemainingSeconds.value / 60);
    const seconds = challengeRemainingSeconds.value % 60;
    return `${String(minutes).padStart(2, "0")}:${String(seconds).padStart(2, "0")}`;
});

const stopChallengeTimer = () => {
    if (challengeTimer !== null) {
        clearInterval(challengeTimer);
        challengeTimer = null;
    }
};

const startChallengeTimer = (expiresIn) => {
    stopChallengeTimer();
    challengeRemainingSeconds.value = Number(expiresIn) || 180;
    challengeTimer = setInterval(() => {
        if (challengeRemainingSeconds.value <= 1) {
            challengeRemainingSeconds.value = 0;
            challengeAnswer.value = "";
            stopChallengeTimer();
            return;
        }
        challengeRemainingSeconds.value -= 1;
    }, 1000);
};

onMounted(async () => {
    await store.loadMypage();
});

// 홈
const goHome = () => {
    router.push("/resident/dashboard");
};

// 수정
const goModify = () => {
    router.push(`/resident/mypage/edit`);
};

const loadChallenge = async () => {
    try {
        const challenge = await store.loadSecurityChallenge();
        challengeId.value = challenge.challengeId;
        challengeImage.value = challenge.imageData;
        challengeAnswer.value = "";
        startChallengeTimer(challenge.expiresIn);
    } catch (error) {
        alert(error.response?.data?.detail || error.response?.data?.message || "보안문자를 불러오지 못했습니다.");
    }
};

const openDeleteConfirm = async () => {
    currentPassword.value = "";
    showDeleteConfirm.value = true;
    await loadChallenge();
};

const closeDeleteConfirm = () => {
    showDeleteConfirm.value = false;
    currentPassword.value = "";
    challengeAnswer.value = "";
    challengeRemainingSeconds.value = 0;
    stopChallengeTimer();
};

const goDelete = async () => {
    if (!currentPassword.value) {
        alert("현재 비밀번호를 입력해주세요.");
        return;
    }
    if (challengeRemainingSeconds.value === 0) {
        alert("보안문자가 만료되었습니다. 새로고침해 주세요.");
        return;
    }
    if (!challengeAnswer.value) {
        alert("보안문자를 입력해주세요.");
        return;
    }
    deleteLoading.value = true;
    try {
        const securityData = {
            currentPassword: currentPassword.value,
            challengeId: challengeId.value,
            challengeAnswer: challengeAnswer.value
        };

        // 비밀번호와 보안문자를 먼저 확인한 뒤 마지막에 탈퇴 여부를 묻는다.
        await store.verifyWithdrawal(securityData);
        if (!confirm("정말로 탈퇴하시겠습니까?")) return;

        await store.removeResident(securityData);
        alert("탈퇴되었습니다.");
        jwtStore.logout();
    } catch (error) {
        const message = error.response?.data?.message
            || error.response?.data?.detail
            || error.response?.data?.error;
        alert(message || "회원탈퇴 요청을 처리하지 못했습니다.");
        await loadChallenge();
    } finally {
        deleteLoading.value = false;
    }
};

onBeforeUnmount(stopChallengeTimer);
</script>

<style scoped>
.security-modal { position: fixed; inset: 0; z-index: 1000; display: grid; place-items: center; padding: 20px; background: rgba(15, 23, 42, .5); }
.security-dialog { width: min(420px, 100%); padding: 24px; display: grid; gap: 16px; border-radius: 14px; background: #fff; box-shadow: 0 20px 50px rgba(15, 23, 42, .25); }
.security-dialog h3, .security-dialog p { margin: 0; }
.security-dialog label { display: grid; gap: 7px; }
.security-dialog input { min-height: 42px; padding: 0 12px; border: 1px solid #cbd5e1; border-radius: 8px; }
.captcha-box { display: flex; align-items: center; gap: 10px; }
.captcha-box img { width: 180px; height: 60px; border: 1px solid #cbd5e1; border-radius: 8px; }
.captcha-timer { margin: -8px 0 0; color: #2563eb; font-size: 14px; }
.captcha-timer.expired { color: #dc2626; font-weight: 700; }
.security-actions { display: flex; justify-content: flex-end; gap: 8px; }
</style>
