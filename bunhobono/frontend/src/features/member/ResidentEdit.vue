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
                <th>이름</th>
                <td>{{ member.memName || "-" }}</td>
            </tr>
            <tr>
                <th>동 · 호수</th>
                <td>{{ member.dong ? `${member.dong}동` : "-" }} {{ member.ho ? `${member.ho}호` : "-" }}</td>
            </tr>
            <tr>
                <th>연락처</th>
                <td>
                    <div class="phone-edit-fields">
                        <div class="phone-input-action">
                            <div class="phone-fields">
                                <input type="text" inputmode="numeric" maxlength="3" :value="phoneParts.first" @input="handlePhoneInput($event, 'first', 3)">
                                <span>-</span>
                                <input type="text" inputmode="numeric" maxlength="4" :value="phoneParts.middle" @input="handlePhoneInput($event, 'middle', 4)">
                                <span>-</span>
                                <input type="text" inputmode="numeric" maxlength="4" :value="phoneParts.last" @input="handlePhoneInput($event, 'last', 4)">
                            </div>
                            <button
                                type="button"
                                class="phone-auth-button"
                                :disabled="!phoneChanged || phoneSending || phoneVerified"
                                @click="sendPhoneAuthCode"
                            >
                                {{ phoneSending ? "발송 중" : phoneVerified ? "인증 완료" : phoneCodeSent ? "재발송" : "인증번호 발송" }}
                            </button>
                        </div>
                        <span v-if="phoneChanged && phoneCodeSent && !phoneVerified" class="phone-auth-guide">
                            3분 내에 인증번호를 입력해 주세요.
                        </span>
                        <span v-if="phoneChanged && phoneVerified" class="phone-auth-success">전화번호 인증이 완료되었습니다.</span>
                        <div v-if="phoneChanged && phoneCodeSent && !phoneVerified" class="phone-code-row">
                            <div class="phone-code-input-wrap">
                                <input
                                    v-model="phoneAuthCode"
                                    type="text"
                                    inputmode="numeric"
                                    maxlength="6"
                                    placeholder="인증번호 6자리"
                                    @input="handlePhoneCodeInput"
                                >
                                <span :class="{ expired: phoneAuthRemainingSeconds === 0 }">{{ phoneAuthTimerText }}</span>
                            </div>
                            <button type="button" :disabled="phoneVerifying || phoneAuthRemainingSeconds === 0" @click="verifyPhoneAuthCode">
                                {{ phoneVerifying ? "확인 중" : "인증 확인" }}
                            </button>
                        </div>
                    </div>
                </td>
            </tr>
            <tr>
                <th>이메일</th>
                <td>
                    <div class="email-edit-fields">
                        <div class="email-input-action">
                            <input
                                v-model.trim="member.email"
                                class="email-input"
                                type="email"
                                maxlength="254"
                                autocomplete="email"
                                placeholder="이메일을 입력하세요"
                                @input="resetEmailVerification"
                            >
                            <button
                                type="button"
                                class="email-auth-button"
                                :disabled="!emailChanged || emailSending || emailVerified"
                                @click="sendEmailAuthCode"
                            >
                                {{ emailSending ? "발송 중" : emailVerified ? "인증 완료" : emailCodeSent ? "재발송" : "인증번호 발송" }}
                            </button>
                        </div>
                        <span
                            v-if="emailChanged && emailCodeSent && !emailVerified"
                            class="email-auth-guide"
                        >
                            3분 내에 인증번호를 입력해 주세요.
                        </span>
                        <span v-if="emailChanged && emailVerified" class="email-auth-success">이메일 인증이 완료되었습니다.</span>
                        <div v-if="emailChanged && emailCodeSent && !emailVerified" class="email-code-row">
                            <div class="email-code-input-wrap">
                                <input
                                    v-model="emailAuthCode"
                                    type="text"
                                    inputmode="numeric"
                                    maxlength="6"
                                    placeholder="인증번호 6자리"
                                    @input="handleEmailCodeInput"
                                >
                                <span :class="{ expired: emailAuthRemainingSeconds === 0 }">{{ emailAuthTimerText }}</span>
                            </div>
                            <button type="button" :disabled="emailVerifying || emailAuthRemainingSeconds === 0" @click="verifyEmailAuthCode">
                                {{ emailVerifying ? "확인 중" : "인증 확인" }}
                            </button>
                        </div>
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
const originalPhone = ref("");
const phoneAuthCode = ref("");
const phoneCodeSent = ref(false);
const phoneVerified = ref(false);
const phoneSending = ref(false);
const phoneVerifying = ref(false);
const phoneAuthRemainingSeconds = ref(0);
let phoneAuthTimer = null;
const originalEmail = ref("");
const emailAuthCode = ref("");
const emailCodeSent = ref(false);
const emailVerified = ref(false);
const emailSending = ref(false);
const emailVerifying = ref(false);
const emailAuthRemainingSeconds = ref(0);
let emailAuthTimer = null;

// 새 비밀번호의 형식을 회원가입 규칙과 동일하게 검사한다.
const passwordPattern = /^(?=.*[A-Za-z])(?=.*\d)(?=.*[!@#$%^&*])[A-Za-z\d!@#$%^&*]{8,20}$/;

const challengeTimeLabel = computed(() => {
    if (challengeRemainingSeconds.value <= 0) return "만료됨";
    const minutes = Math.floor(challengeRemainingSeconds.value / 60);
    const seconds = challengeRemainingSeconds.value % 60;
    return `${String(minutes).padStart(2, "0")}:${String(seconds).padStart(2, "0")}`;
});
const emailChanged = computed(() => (member.email || "").trim().toLowerCase() !== originalEmail.value.trim().toLowerCase());
const currentPhone = computed(() => `${phoneParts.first}-${phoneParts.middle}-${phoneParts.last}`);
const phoneChanged = computed(() => currentPhone.value.replace(/\D/g, "") !== originalPhone.value.replace(/\D/g, ""));
const phoneAuthTimerText = computed(() => {
    const minutes = Math.floor(phoneAuthRemainingSeconds.value / 60);
    const seconds = phoneAuthRemainingSeconds.value % 60;
    return `${String(minutes).padStart(2, "0")}:${String(seconds).padStart(2, "0")}`;
});
const emailAuthTimerText = computed(() => {
    const minutes = Math.floor(emailAuthRemainingSeconds.value / 60);
    const seconds = emailAuthRemainingSeconds.value % 60;
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
    dong: "",
    ho: "",
    memPhone: "",
    email: "",
    loginId: "",
    loginPwd: "",
    memStatus: "",
});

onMounted(async () => {
    await store.loadMypage(loginId);
    Object.assign(member, store.member);
    member.loginPwd = "";
    originalPhone.value = member.memPhone || "";
    originalEmail.value = member.email || "";
    setPhoneParts(member.memPhone);
});

const stopPhoneAuthTimer = () => {
    if (phoneAuthTimer !== null) {
        clearInterval(phoneAuthTimer);
        phoneAuthTimer = null;
    }
};

const startPhoneAuthTimer = () => {
    stopPhoneAuthTimer();
    const expiresAt = Date.now() + 3 * 60 * 1000;
    phoneAuthRemainingSeconds.value = 180;
    phoneAuthTimer = window.setInterval(() => {
        phoneAuthRemainingSeconds.value = Math.max(0, Math.ceil((expiresAt - Date.now()) / 1000));
        if (phoneAuthRemainingSeconds.value === 0) stopPhoneAuthTimer();
    }, 1000);
};

const resetPhoneVerification = () => {
    phoneAuthCode.value = "";
    phoneCodeSent.value = false;
    phoneVerified.value = false;
    phoneAuthRemainingSeconds.value = 0;
    stopPhoneAuthTimer();
};

const handlePhoneCodeInput = (event) => {
    phoneAuthCode.value = event.target.value.replace(/\D/g, "").slice(0, 6);
    event.target.value = phoneAuthCode.value;
};

const sendPhoneAuthCode = async () => {
    if (phoneParts.first.length !== 3 || phoneParts.middle.length !== 4 || phoneParts.last.length !== 4) {
        await alertDialog({ theme: "resident", type: "warning", title: "연락처 확인", message: "연락처를 정확히 입력하세요." });
        return;
    }
    phoneSending.value = true;
    try {
        await store.sendPhoneCode(currentPhone.value);
        phoneCodeSent.value = true;
        phoneAuthCode.value = "";
        startPhoneAuthTimer();
        await alertDialog({ theme: "resident", type: "success", title: "인증번호 발송", message: "연락처로 인증번호를 발송했습니다." });
    } catch (error) {
        await alertDialog({ theme: "resident", type: "error", title: "인증번호 발송 실패", message: error.response?.data?.message || "연락처 인증번호를 발송하지 못했습니다." });
    } finally {
        phoneSending.value = false;
    }
};

const verifyPhoneAuthCode = async () => {
    if (!/^\d{6}$/.test(phoneAuthCode.value)) {
        await alertDialog({ theme: "resident", type: "warning", title: "인증번호 확인", message: "인증번호 6자리를 입력해 주세요." });
        return;
    }
    phoneVerifying.value = true;
    try {
        await store.verifyPhoneCode(currentPhone.value, phoneAuthCode.value);
        phoneVerified.value = true;
        stopPhoneAuthTimer();
        await alertDialog({ theme: "resident", type: "success", title: "전화번호 인증 완료", message: "전화번호가 인증되었습니다." });
    } catch (error) {
        await alertDialog({ theme: "resident", type: "error", title: "인증번호 확인 실패", message: error.response?.data?.message || "인증번호를 확인하지 못했습니다." });
    } finally {
        phoneVerifying.value = false;
    }
};

const stopEmailAuthTimer = () => {
    if (emailAuthTimer !== null) {
        clearInterval(emailAuthTimer);
        emailAuthTimer = null;
    }
};

const startEmailAuthTimer = () => {
    stopEmailAuthTimer();
    const expiresAt = Date.now() + 3 * 60 * 1000;
    emailAuthRemainingSeconds.value = 180;
    emailAuthTimer = window.setInterval(() => {
        emailAuthRemainingSeconds.value = Math.max(0, Math.ceil((expiresAt - Date.now()) / 1000));
        if (emailAuthRemainingSeconds.value === 0) stopEmailAuthTimer();
    }, 1000);
};

const resetEmailVerification = () => {
    emailAuthCode.value = "";
    emailCodeSent.value = false;
    emailVerified.value = false;
    emailAuthRemainingSeconds.value = 0;
    stopEmailAuthTimer();
};

const handleEmailCodeInput = (event) => {
    emailAuthCode.value = event.target.value.replace(/\D/g, "").slice(0, 6);
    event.target.value = emailAuthCode.value;
};

const sendEmailAuthCode = async () => {
    if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(member.email || "")) {
        await alertDialog({ theme: "resident", type: "warning", title: "이메일 형식 확인", message: "이메일 주소를 정확히 입력하세요." });
        return;
    }
    emailSending.value = true;
    try {
        await store.sendEmailCode(member.email.trim());
        emailCodeSent.value = true;
        emailAuthCode.value = "";
        startEmailAuthTimer();
        await alertDialog({ theme: "resident", type: "success", title: "인증번호 발송", message: "이메일로 인증번호를 발송했습니다." });
    } catch (error) {
        await alertDialog({ theme: "resident", type: "error", title: "인증번호 발송 실패", message: error.response?.data?.message || "이메일 인증번호를 발송하지 못했습니다." });
    } finally {
        emailSending.value = false;
    }
};

const verifyEmailAuthCode = async () => {
    if (!/^\d{6}$/.test(emailAuthCode.value)) {
        await alertDialog({ theme: "resident", type: "warning", title: "인증번호 확인", message: "인증번호 6자리를 입력해 주세요." });
        return;
    }
    emailVerifying.value = true;
    try {
        await store.verifyEmailCode(member.email.trim(), emailAuthCode.value);
        emailVerified.value = true;
        stopEmailAuthTimer();
        await alertDialog({ theme: "resident", type: "success", title: "이메일 인증 완료", message: "이메일이 인증되었습니다." });
    } catch (error) {
        await alertDialog({ theme: "resident", type: "error", title: "인증번호 확인 실패", message: error.response?.data?.message || "인증번호를 확인하지 못했습니다." });
    } finally {
        emailVerifying.value = false;
    }
};

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
    resetPhoneVerification();
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
    if (phoneChanged.value && !phoneVerified.value) {
        await alertDialog({
            theme: "resident",
            type: "warning",
            title: "전화번호 인증 필요",
            message: "변경할 전화번호의 인증을 완료해 주세요."
        });
        return;
    }

    if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(member.email || "")) {
        await alertDialog({
            theme: "resident",
            type: "warning",
            title: "이메일 확인",
            message: "이메일 주소를 정확히 입력하세요."
        });
        return;
    }
    if (emailChanged.value && !emailVerified.value) {
        await alertDialog({
            theme: "resident",
            type: "warning",
            title: "이메일 인증 필요",
            message: "변경할 이메일의 인증을 완료해 주세요."
        });
        return;
    }

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
            email: member.email,
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
        await store.editResident({ memPhone: member.memPhone, email: member.email, loginPwd: null });
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

onBeforeUnmount(() => {
    stopChallengeTimer();
    stopPhoneAuthTimer();
    stopEmailAuthTimer();
});
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
.edit-form-area tr:nth-child(1) td,.edit-form-area tr:nth-child(5) td { color: #287fd5; font-weight: 700; }
.edit-form-area input:focus { border-color: #45bff2; outline: 3px solid rgba(69,191,242,.16); }
.email-input { width: min(420px, 100%); min-height: 40px; padding: 0 12px; border: 1px solid #cbd8e4; border-radius: 7px; box-sizing: border-box; color: #243f58; background: #fff; }
.phone-edit-fields,.email-edit-fields { width: min(620px, 100%); display: grid; gap: 8px; }
.phone-input-action,.phone-code-row,.email-input-action,.email-code-row { display: grid; grid-template-columns: minmax(0, 1fr) auto; gap: 8px; }
.email-input-action .email-input { width: 100%; }
.phone-auth-button,.phone-code-row button,.email-auth-button,.email-code-row button { min-height: 40px; padding: 0 13px; border: 1px solid #9fcbe6; border-radius: 7px; color: #176da8; background: #edf8fe; font-weight: 700; cursor: pointer; }
.phone-auth-button:disabled,.phone-code-row button:disabled,.email-auth-button:disabled,.email-code-row button:disabled { color: #91a2af; border-color: #d7e0e6; background: #f3f5f7; cursor: not-allowed; }
.phone-auth-guide,.email-auth-guide { color: #5f7488; font-size: 13px; }
.phone-auth-success,.email-auth-success { color: #24865a; font-size: 13px; font-weight: 700; }
.phone-code-input-wrap,.email-code-input-wrap { position: relative; }
.phone-code-input-wrap input,.email-code-input-wrap input { width: 100%; min-height: 40px; padding: 0 62px 0 12px; border: 1px solid #cbd8e4; border-radius: 7px; box-sizing: border-box; }
.phone-code-input-wrap span,.email-code-input-wrap span { position: absolute; top: 50%; right: 11px; color: #287fd5; font-size: 12px; transform: translateY(-50%); }
.phone-code-input-wrap span.expired,.email-code-input-wrap span.expired { color: #dc2626; }
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

@media (any-pointer: coarse) and (max-width: 820px),
       (any-pointer: coarse) and (max-height: 820px) {
    .resident-edit-page { width: 100%; padding: 18px 12px; }
    .edit-page-title { margin-bottom: 16px; }
    .edit-page-title h2 { font-size: 25px; }
    .edit-card { padding: 18px 12px; grid-template-columns: 1fr; gap: 20px; border-radius: 14px; }
    .edit-form-area h3 { margin-bottom: 14px; font-size: 21px; }
    .edit-form-area table { table-layout: fixed; }
    .edit-form-area th { width: 88px; }
    .edit-form-area th,.edit-form-area td { padding: 9px 8px; font-size: 14px; overflow-wrap: anywhere; }
    .edit-page-actions { display: grid; grid-template-columns: 1fr 1fr; gap: 8px; padding-top: 0; }
    .edit-page-actions button,.edit-page-actions .save-button { width: 100%; min-height: 44px; margin: 0; }
    .edit-page-actions .back-button { grid-column: 1 / -1; }
    .phone-fields { display: grid; grid-template-columns: minmax(0, 1fr) auto minmax(0, 1.25fr) auto minmax(0, 1.25fr); gap: 4px; }
    .phone-fields input { width: 100%; min-width: 0; min-height: 44px; padding: 0 4px; font-size: 16px; }
    .phone-input-action,.phone-code-row { grid-template-columns: 1fr; }
    .password-change-fields { width: 100%; margin-top: 6px; gap: 10px; }
    .password-change-fields input { min-height: 46px; font-size: 16px; }
    .captcha-box { display: grid; grid-template-columns: minmax(0, 1fr) auto; }
    .captcha-box img { width: 100%; max-width: 180px; }
    .captcha-box button { min-height: 44px; }
}
</style>
