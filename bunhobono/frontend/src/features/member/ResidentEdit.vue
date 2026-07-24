<template>
  <main class="resident-edit-page">
    <header class="edit-page-title">
        <h2>정보 수정페이지</h2>
    </header>

    <section class="edit-card">
      <div class="edit-form-area">
        <h3>내 정보 수정</h3>
        <table>
        <tbody>
            <tr>
                <th>가입유형</th>
                <td>{{ member.role || "-" }}</td>
            </tr>
            <tr>
                <th>이름</th>
                <td>{{ member.memName || "-" }}</td>
            </tr>
            <tr>
                <th>동</th>
                <td>{{ member.memDong || "-" }}</td>
            </tr>
            <tr>
                <th>호수</th>
                <td>{{ member.memHo || "-" }}</td>
            </tr>
            <tr>
                <th>연락처</th>
                <td>
                    <div class="phone-fields">
                        <input type="text" inputmode="numeric" maxlength="3" :value="phoneParts.first" @input="handlePhoneInput($event, 'first', 3)">
                        <span>-</span>
                        <input type="text" inputmode="numeric" maxlength="4" :value="phoneParts.middle" @input="handlePhoneInput($event, 'middle', 4)">
                        <span>-</span>
                        <input type="text" inputmode="numeric" maxlength="4" :value="phoneParts.last" @input="handlePhoneInput($event, 'last', 4)">
                    </div>
                </td>
            </tr>
            <tr>
                <th>아이디</th>
                <td>{{ member.loginId || "-" }}</td>
            </tr>
            <tr>
                <th>비밀번호</th>
                <td>
                    <div v-if="showPasswordField" class="password-change-fields">
                        <input v-model="currentPassword" type="password" autocomplete="current-password" placeholder="현재 비밀번호">
                        <input
                            type="password"
                            :value="member.loginPwd"
                            inputmode="text"
                            minlength="8"
                            maxlength="20"
                            autocomplete="new-password"
                            placeholder="새 비밀번호를 입력하세요 (영문, 숫자, 특수문자 포함 8~20자)"
                            @input="handlePasswordInput"
                        >
                        <input v-model="newPasswordConfirm" type="password" inputmode="text" minlength="8" maxlength="20" autocomplete="new-password" placeholder="새 비밀번호 확인">
                        <div class="captcha-box">
                            <img :src="challengeImage" alt="보안문자">
                            <button type="button" @click="loadChallenge">새로고침</button>
                        </div>
                        <p class="captcha-timer" :class="{ expired: challengeRemainingSeconds === 0 }">
                            보안문자 유효시간: {{ challengeTimeLabel }}
                        </p>
                        <input v-model.trim="challengeAnswer" type="text" maxlength="5" autocomplete="off" placeholder="보안문자 입력" :disabled="challengeRemainingSeconds === 0">
                    </div>
                </td>
            </tr>
            <tr>
                <th>상태</th>
                <td>{{ member.memStatus === 'ACTIVE' ? '거주' : member.memStatus || '-' }}</td>
            </tr>
        </tbody>
        </table>
      </div>

      <aside class="edit-page-actions" aria-label="정보 수정 메뉴">
        <button type="button" class="save-button" @click="update">수정 완료</button>
        <button type="button" class="password-button" @click="togglePasswordChange">
          {{ showPasswordField ? "비밀번호 변경 취소" : "비밀번호 변경" }}
        </button>
        <button type="button" class="back-button" @click="goHome">마이페이지로</button>
      </aside>
    </section>
  </main>
</template>

<script setup>
import { computed, onBeforeUnmount, onMounted, reactive, ref } from "vue";
import { useRouter } from "vue-router";
import { useMemStore } from "./memStore";
import { useJwtStore } from "@/features/login/jwtStore";
// 브라우저 기본 alert 대신 공통 Dialog를 사용합니다.
import { useDialog } from "@/shared/alert/useDialog";

const router = useRouter();
const store = useMemStore();
const jwtStore = useJwtStore();

// 공통 Dialog의 alert 대체 함수를 가져옵니다.
const { alertDialog } = useDialog();

const loginId = jwtStore.userId;
const showPasswordField = ref(false);
const currentPassword = ref("");
const newPasswordConfirm = ref("");
const challengeId = ref("");
const challengeImage = ref("");
const challengeAnswer = ref("");
const challengeRemainingSeconds = ref(0);
let challengeTimer = null;
const phoneParts = reactive({ first: "", middle: "", last: "" });

// 새 비밀번호의 형식을 회원가입 규칙과 동일하게 검사한다.
const passwordPattern = /^(?=.*[A-Za-z])(?=.*\d)(?=.*[!@#$%^&*])[A-Za-z\d!@#$%^&*]{8,20}$/;

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

const member = reactive({
    memberNo:"",
    role: "",
    memName: "",
    memDong: "",
    memHo: "",
    memPhone: "",
    loginId: "",
    loginPwd: "",
    memStatus: "",
});

onMounted(async () => {
    await store.loadMypage(loginId);
    Object.assign(member, store.member);
    member.loginPwd = "";
    setPhoneParts(member.memPhone);
});

// 저장된 연락처를 수정 화면의 세 칸으로 나누어 표시한다.
const setPhoneParts = (phone) => {
    const digits = String(phone || "").replace(/\D/g, "").slice(0, 11);
    phoneParts.first = digits.slice(0, 3);
    phoneParts.middle = digits.slice(3, 7);
    phoneParts.last = digits.slice(7, 11);
};

// 연락처 세 칸에는 정해진 길이만큼 숫자만 입력한다.
const handlePhoneInput = (event, part, maxLength) => {
    const numericValue = event.target.value.replace(/\D/g, "").slice(0, maxLength);
    event.target.value = numericValue;
    phoneParts[part] = numericValue;
};

// 허용되지 않은 문자를 제거하고 새 비밀번호를 최대 20자로 제한한다.
const handlePasswordInput = (event) => {
    const passwordValue = event.target.value.replace(/[^A-Za-z\d!@#$%^&*]/g, "").slice(0, 20);
    event.target.value = passwordValue;
    member.loginPwd = passwordValue;
};

const goHome = () => {
    router.push("/resident/mypage");
};

const loadChallenge = async () => {
    try {
        const challenge = await store.loadSecurityChallenge();
        challengeId.value = challenge.challengeId;
        challengeImage.value = challenge.imageData;
        challengeAnswer.value = "";
        startChallengeTimer(challenge.expiresIn);
    } catch (error) {
        await alertDialog({
            theme: "resident",
            type: "error",
            title: "보안문자 오류",
            message: error.response?.data?.detail
                || error.response?.data?.message
                || "보안문자를 불러오지 못했습니다."
        });
    }
};

const togglePasswordChange = async () => {
    showPasswordField.value = !showPasswordField.value;
    currentPassword.value = "";
    member.loginPwd = "";
    newPasswordConfirm.value = "";
    challengeAnswer.value = "";
    if (showPasswordField.value) {
        await loadChallenge();
    } else {
        challengeRemainingSeconds.value = 0;
        stopChallengeTimer();
    }
};

const update = async () => {
    if (phoneParts.first.length !== 3 || phoneParts.middle.length !== 4 || phoneParts.last.length !== 4) {
        await alertDialog({
            theme: "resident",
            type: "warning",
            title: "연락처 확인",
            message: "연락처를 정확히 입력하세요."
        });
        return;
    }
    member.memPhone = `${phoneParts.first}-${phoneParts.middle}-${phoneParts.last}`;

    // 비밀번호 변경 시 회원가입과 동일한 새 비밀번호 형식인지 확인한다.
    if (showPasswordField.value && !passwordPattern.test(member.loginPwd)) {
        await alertDialog({
            theme: "resident",
            type: "warning",
            title: "비밀번호 형식 확인",
            message: "비밀번호는 영문+숫자+특수문자 조합으로 8~20자로 입력하세요."
        });
        return;
    }

    if (!showPasswordField.value) {
        await store.editResident({
            memPhone: member.memPhone,
            loginPwd: null
        });

        // 수정 성공 안내를 resident 밝은 테마 Dialog로 표시합니다.
        // 사용자가 확인 버튼을 누른 후 마이페이지로 이동합니다.
        await alertDialog({
            theme: "resident",
            type: "success",
            title: "정보 수정 완료",
            message: "회원 정보가 수정되었습니다."
        });

        router.push("/resident/mypage");
        return;
    }

    if (!currentPassword.value) {
        await alertDialog({
            theme: "resident",
            type: "warning",
            title: "현재 비밀번호 확인",
            message: "현재 비밀번호를 입력하세요."
        });
        return;
    }
    if (member.loginPwd !== newPasswordConfirm.value) {
        await alertDialog({
            theme: "resident",
            type: "warning",
            title: "비밀번호 불일치",
            message: "새 비밀번호가 일치하지 않습니다."
        });
        return;
    }
    if (challengeRemainingSeconds.value === 0) {
        await alertDialog({
            theme: "resident",
            type: "warning",
            title: "보안문자 만료",
            message: "보안문자가 만료되었습니다. 새로고침해 주세요."
        });
        return;
    }
    if (!challengeAnswer.value) {
        await alertDialog({
            theme: "resident",
            type: "warning",
            title: "보안문자 확인",
            message: "보안문자를 입력하세요."
        });
        return;
    }

    try {
        await store.editResident({ memPhone: member.memPhone, loginPwd: null });
        await store.updateResidentPassword({
            currentPassword: currentPassword.value,
            newPassword: member.loginPwd,
            challengeId: challengeId.value,
            challengeAnswer: challengeAnswer.value
        });
        await alertDialog({
            theme: "resident",
            type: "success",
            title: "비밀번호 변경 완료",
            message: "비밀번호가 변경되었습니다. 다시 로그인해 주세요."
        });
        jwtStore.logout();
    } catch (error) {
        await alertDialog({
            theme: "resident",
            type: "error",
            title: "비밀번호 변경 실패",
            message: error.response?.data?.detail
                || error.response?.data?.message
                || error.response?.data?.error
                || "비밀번호 변경에 실패했습니다."
        });
        await loadChallenge();
    }
};

onBeforeUnmount(stopChallengeTimer);
</script>

<style scoped>
.resident-edit-page { width: min(1120px,100%); margin: 0 auto; padding: 28px 24px; box-sizing: border-box; }
.edit-page-title { margin-bottom: 22px; text-align: center; }
.edit-page-title h2 { margin: 0; color: #203c58; font-size: 30px; }
.edit-card { padding: 34px; display: grid; grid-template-columns: 1fr; gap: 20px; border: 1px solid #cbddec; border-radius: 18px; background: rgba(255,255,255,.92); box-shadow: 0 14px 34px rgba(50,91,126,.12); }
.edit-form-area h3 { margin: 0 0 20px; color: #203c58; font-size: 25px; }
.edit-form-area table { width: 100%; border-collapse: collapse; border: 1px solid #cbd8e4; background: #fff; font-size: 16px; }
.edit-form-area th,.edit-form-area td { min-height: 58px; padding: 10px 18px; border-bottom: 1px solid #dbe5ed; box-sizing: border-box; }
.edit-form-area tr:last-child th,.edit-form-area tr:last-child td { border-bottom: 0; }
.edit-form-area th { width: 170px; border-right: 1px solid #dbe5ed; color: #38536d; background: #f5faff; text-align: center; }
.edit-form-area td { color: #243f58; }
.edit-form-area tr:nth-child(1) td,.edit-form-area tr:nth-child(2) td { color: #287fd5; font-weight: 700; }
.edit-form-area tr:last-child td { color: #2ca66a; font-weight: 800; }
.edit-form-area input:focus { border-color: #45bff2; outline: 3px solid rgba(69,191,242,.16); }
.edit-page-actions { display: flex; flex-direction: row; justify-content: flex-end; gap: 8px; padding-top: 4px; }
.edit-page-actions button { width: auto; min-height: 40px; padding: 8px 14px; border: 1px solid #a9c8df; border-radius: 8px; box-shadow: none; font-size: 13px; font-weight: 700; cursor: pointer; }
.edit-page-actions .save-button { min-height: 40px; border-color: #45bff2; color: #fff; background: #45bff2; }
.edit-page-actions .password-button { border-color: #f2c889; color: #85551d; background: #fff7e8; }
.edit-page-actions .back-button { border-color: #a9dfbf; color: #267047; background: #edfbf3; }
.edit-page-actions button:hover { transform: translateY(-1px); filter: brightness(.97); }
.phone-fields {
    display: flex;
    align-items: center;
    gap: 6px;
}

.phone-fields input {
    width: 72px;
    text-align: center;
}

.password-change-fields { margin-top: 10px; display: grid; gap: 8px; width: 440px; max-width: 100%; }
.password-change-fields input { width: 100%; min-height: 40px; padding: 0 10px; box-sizing: border-box; }
.captcha-box { display: flex; align-items: center; gap: 10px; }
.captcha-box img { width: 180px; height: 60px; border: 1px solid #cbd5e1; border-radius: 8px; }
.captcha-timer { margin: 0; color: #2563eb; font-size: 14px; }
.captcha-timer.expired { color: #dc2626; font-weight: 700; }

@media (max-width: 760px) {
    .resident-edit-page { padding: 20px 14px; }
    .edit-card { padding: 20px; grid-template-columns: 1fr; gap: 24px; }
    .edit-page-actions { display: flex; justify-content: flex-end; gap: 8px; }
    .edit-page-actions button,.edit-page-actions .save-button { min-height: 40px; }
    .edit-form-area th { width: 115px; }
    .edit-form-area th,.edit-form-area td { padding: 8px 10px; font-size: 14px; }
    .phone-fields { flex-wrap: nowrap; }
    .phone-fields input { width: min(72px,23%); }
}
</style>
