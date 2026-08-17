<template>
    <section class="login-card find-account-card">
        <RouterLink class="find-account-brand" to="/login">Bunhobono APT</RouterLink>

        <!-- 아이디 찾기 1단계: 인증 수단 입력 -->
        <div v-if="currentView === 'id-contact'" class="find-account-content">
            <h2 class="login-title">아이디 찾기</h2>
            <p class="find-description">보안코드를 받을 방법을 선택해 주세요.</p>

            <div class="contact-method-form">
                <div class="contact-type-toggle">
                    <button
                        type="button"
                        :class="{ active: idContactType === 'phone' }"
                        @click="selectContactType('id', 'phone')">
                        전화번호로 찾기
                    </button>
                    <button
                        type="button"
                        :class="{ active: idContactType === 'email' }"
                        @click="selectContactType('id', 'email')">
                        이메일로 찾기
                    </button>
                </div>

                <label v-if="idContactType === 'phone'" key="id-phone" class="form-field">
                    <span>전화번호</span>
                    <input
                        v-model="idPhone"
                        type="text"
                        inputmode="numeric"
                        maxlength="11"
                        placeholder="'-' 없이 숫자만 적어주세요"
                        @input="idPhone = phoneNumbersOnly(idPhone)"
                        @keyup.enter="goToIdNameStep">
                </label>

                <label v-else key="id-email" class="form-field">
                    <span>이메일</span>
                    <div class="email-input-group">
                        <input
                            v-model.trim="idEmailId"
                            type="text"
                            placeholder="이메일 아이디"
                            @keyup.enter="goToIdNameStep">
                        <span class="email-at">@</span>
                        <select v-model="idEmailDomain">
                            <option disabled value="">이메일 선택</option>
                            <option v-for="domain in emailDomains" :key="domain" :value="domain">
                                {{ domain }}
                            </option>
                        </select>
                    </div>
                </label>
            </div>

            <p v-if="idError" class="find-error">{{ idError }}</p>
            <button type="button" class="login-submit" @click="goToIdNameStep">다음</button>
        </div>

        <!-- 아이디 찾기 2단계: 회원 이름과 동·호수 확인 -->
        <div v-else-if="currentView === 'id-name'" class="find-account-content">
            <h2 class="login-title">이름/동호수 확인</h2>
            <p class="find-description">회원가입할 때 입력한 이름과 동·호수를 입력해 주세요.</p>

            <div class="contact-summary">
                <span>{{ contactTypeLabel(idContactType) }}</span>
                <strong>{{ idContact }}</strong>
            </div>

            <label class="form-field">
                <span>이름</span>
                <input
                    v-model.trim="memberName"
                    type="text"
                    maxlength="10"
                    placeholder="이름을 입력하세요"
                    @keyup.enter="sendIdSecurityCode">
            </label>

            <div class="unit-fields">
                <label class="form-field">
                    <span>동</span>
                    <input
                        :value="memberDong"
                        type="text"
                        inputmode="numeric"
                        maxlength="3"
                        placeholder="동"
                        @input="handleUnitInput($event, 'dong', 3)">
                </label>

                <label class="form-field">
                    <span>호수</span>
                    <input
                        :value="memberHo"
                        type="text"
                        inputmode="numeric"
                        maxlength="4"
                        placeholder="호수"
                        @input="handleUnitInput($event, 'ho', 4)"
                        @keyup.enter="sendIdSecurityCode">
                </label>
            </div>

            <p v-if="idError" class="find-error">{{ idError }}</p>
            <div class="find-actions">
                <button type="button" class="secondary-button" :disabled="isSubmitting" @click="idStep = 1">이전</button>
                <button type="button" class="login-submit" :disabled="isSubmitting" @click="sendIdSecurityCode">
                    {{ isSubmitting ? "발송 중..." : "보안코드 발송" }}
                </button>
            </div>
        </div>

        <!-- 아이디 찾기 3단계: 보안코드 확인 -->
        <div v-else-if="currentView === 'id-verify'" class="find-account-content">
            <h2 class="login-title">보안코드 확인</h2>
            <p class="find-description">{{ contactTypeLabel(idContactType) }}로 받은 보안코드 6자리를 입력해 주세요.</p>

            <div class="verification-target phone-target">
                <div>
                    <span>전송 {{ contactTypeLabel(idContactType) }}</span>
                    <strong>{{ idContact }}</strong>
                </div>
                <button
                    type="button"
                    class="send-code-button"
                    :disabled="isSubmitting || resendSeconds > 0"
                    @click="sendIdSecurityCode">
                    {{
                        isSubmitting
                            ? "발송 중..."
                            : resendSeconds > 0
                                ? `재전송 ${resendSeconds}초`
                                : "재전송"
                    }}
                </button>
            </div>

            <label class="form-field">
                <span>보안코드</span>
                <input
                    v-model="idSecurityCode"
                    type="text"
                    inputmode="numeric"
                    maxlength="6"
                    placeholder="보안코드 6자리"
                    @input="idSecurityCode = numbersOnly(idSecurityCode)"
                    @keyup.enter="verifyIdSecurityCode">
            </label>

            <p class="verification-time" :class="{ expired: remainingSeconds === 0 }">
                {{ verificationTimeMessage }}
            </p>

            <p v-if="idError" class="find-error">{{ idError }}</p>
            <div class="find-actions">
                <button type="button" class="secondary-button" :disabled="isSubmitting" @click="backToIdNameStep">이전</button>
                <button
                    type="button"
                    class="login-submit"
                    :disabled="isSubmitting || remainingSeconds === 0"
                    @click="verifyIdSecurityCode">
                    {{ isSubmitting ? "확인 중..." : "인증 확인" }}
                </button>
            </div>
        </div>

        <!-- 아이디 찾기 4단계: 조회 결과 -->
        <div v-else-if="currentView === 'id-result'" class="find-account-content result-content">
            <h2 class="login-title">아이디 확인</h2>
            <p class="find-description">인증된 회원의 아이디입니다.</p>

            <div class="account-result">
                <span>아이디</span>
                <strong>{{ foundLoginId || '백엔드 조회 결과' }}</strong>
            </div>

            <RouterLink class="login-submit result-login-button" to="/login">로그인 하기</RouterLink>
            <!-- [email인증] 아이디 확인 후 비밀번호 찾기 화면으로 이동한다. -->
            <RouterLink
                class="password-find-link"
                to="/resident/find-account?mode=password"
                @click="selectedMode = 'password'">
                비밀번호 찾기
            </RouterLink>
        </div>

        <!-- 비밀번호 찾기 1단계: 아이디 입력 -->
        <div v-else-if="currentView === 'password-id'" class="find-account-content">
            <h2 class="login-title">비밀번호 찾기</h2>
            <p class="find-description">비밀번호를 변경할 아이디를 입력해 주세요.</p>

            <label class="form-field">
                <span>아이디</span>
                <input
                    v-model.trim="passwordLoginId"
                    type="text"
                    placeholder="아이디를 입력하세요"
                    @keyup.enter="goToPasswordContactStep">
            </label>

            <p v-if="passwordError" class="find-error">{{ passwordError }}</p>
            <button type="button" class="step-next-button" @click="goToPasswordContactStep">다음</button>
        </div>

        <!-- 비밀번호 찾기 2단계: 인증 수단 입력 -->
        <div v-else-if="currentView === 'password-contact'" class="find-account-content">
            <h2 class="login-title">본인 확인</h2>
            <p class="find-description">보안코드를 받을 방법을 선택해 주세요.</p>

            <div class="contact-method-form">
                <div class="contact-type-toggle">
                    <button
                        type="button"
                        :class="{ active: passwordContactType === 'phone' }"
                        @click="selectContactType('password', 'phone')">
                        전화번호로 찾기
                    </button>
                    <button
                        type="button"
                        :class="{ active: passwordContactType === 'email' }"
                        @click="selectContactType('password', 'email')">
                        이메일로 찾기
                    </button>
                </div>

                <label v-if="passwordContactType === 'phone'" key="password-phone" class="form-field">
                    <span>전화번호</span>
                    <input
                        v-model="passwordPhone"
                        type="text"
                        inputmode="numeric"
                        maxlength="11"
                        placeholder="'-' 없이 숫자만 적어주세요"
                        @input="passwordPhone = phoneNumbersOnly(passwordPhone)"
                        @keyup.enter="sendPasswordSecurityCode">
                </label>

                <label v-else key="password-email" class="form-field">
                    <span>이메일</span>
                    <div class="email-input-group">
                        <input
                            v-model.trim="passwordEmailId"
                            type="text"
                            placeholder="이메일 아이디"
                            @keyup.enter="sendPasswordSecurityCode">
                        <span class="email-at">@</span>
                        <select v-model="passwordEmailDomain">
                            <option disabled value="">이메일 선택</option>
                            <option v-for="domain in emailDomains" :key="domain" :value="domain">
                                {{ domain }}
                            </option>
                        </select>
                    </div>
                </label>
            </div>

            <p v-if="passwordError" class="find-error">{{ passwordError }}</p>
            <div class="find-actions">
                <button type="button" class="secondary-button" :disabled="isSubmitting" @click="passwordStep = 1">이전</button>
                <button type="button" class="login-submit" :disabled="isSubmitting" @click="sendPasswordSecurityCode">
                    {{ isSubmitting ? "발송 중..." : "보안코드 발송" }}
                </button>
            </div>
        </div>

        <!-- 비밀번호 찾기 3단계: 보안코드 확인 -->
        <div v-else-if="currentView === 'password-verify'" class="find-account-content">
            <h2 class="login-title">보안코드 확인</h2>
            <p class="find-description">{{ contactTypeLabel(passwordContactType) }}로 받은 보안코드 6자리를 입력해 주세요.</p>

            <div class="verification-target phone-target">
                <div>
                    <span>전송 {{ contactTypeLabel(passwordContactType) }}</span>
                    <strong>{{ passwordContact }}</strong>
                </div>
                <button
                    type="button"
                    class="send-code-button"
                    :disabled="isSubmitting || resendSeconds > 0"
                    @click="sendPasswordSecurityCode">
                    {{
                        isSubmitting
                            ? "발송 중..."
                            : resendSeconds > 0
                                ? `재전송 ${resendSeconds}초`
                                : "재전송"
                    }}
                </button>
            </div>

            <label class="form-field">
                <span>보안코드</span>
                <input
                    v-model="passwordSecurityCode"
                    type="text"
                    inputmode="numeric"
                    maxlength="6"
                    placeholder="보안코드 6자리"
                    @input="passwordSecurityCode = numbersOnly(passwordSecurityCode)"
                    @keyup.enter="verifyPasswordSecurityCode">
            </label>

            <p class="verification-time" :class="{ expired: remainingSeconds === 0 }">
                {{ verificationTimeMessage }}
            </p>

            <p v-if="passwordError" class="find-error">{{ passwordError }}</p>
            <div class="find-actions">
                <button type="button" class="secondary-button" :disabled="isSubmitting" @click="backToPasswordContactStep">이전</button>
                <button
                    type="button"
                    class="login-submit"
                    :disabled="isSubmitting || remainingSeconds === 0"
                    @click="verifyPasswordSecurityCode">
                    {{ isSubmitting ? "확인 중..." : "인증 확인" }}
                </button>
            </div>
        </div>

        <!-- 비밀번호 찾기 4단계: 새 비밀번호 입력 -->
        <div v-else class="find-account-content">
            <h2 class="login-title">새 비밀번호 설정</h2>
            <p class="find-description">새로 사용할 비밀번호를 입력해 주세요.</p>

            <label class="form-field">
                <span>새 비밀번호</span>
                <input
                    v-model="newPassword"
                    type="password"
                    autocomplete="off"
                    placeholder="영문, 숫자, 특수문자 포함 8~20자">
            </label>

            <label class="form-field">
                <span>새 비밀번호 확인</span>
                <input
                    v-model="newPasswordConfirm"
                    type="password"
                    autocomplete="off"
                    placeholder="새 비밀번호를 다시 입력하세요"
                    @keyup.enter="changePassword">
            </label>

            <!-- [email인증] 새 비밀번호 확인값의 일치 여부를 입력 중 바로 안내한다. -->
            <p
                v-if="passwordMatchMessage"
                class="password-match-message"
                :class="{ matched: passwordsMatch, mismatched: !passwordsMatch }">
                {{ passwordMatchMessage }}
            </p>

            <p v-if="passwordError" class="find-error">{{ passwordError }}</p>
            <button type="button" class="login-submit" :disabled="isSubmitting" @click="changePassword">
                {{ isSubmitting ? "변경 중..." : "비밀번호 변경" }}
            </button>
        </div>

        <RouterLink v-if="!isFinished" class="back-to-login" to="/login">로그인 화면으로 돌아가기</RouterLink>
    </section>
</template>

<script setup>
import { computed, onUnmounted, ref } from "vue";
import { useRoute, useRouter } from "vue-router";
import { useDialog } from "@/shared/alert/useDialog";
import {
    resetAccountPassword,
    sendAccountRecoveryCode,
    findAccountLoginId
} from "@/features/member/memApi";

const route = useRoute();
const router = useRouter();
const { alertDialog } = useDialog();

const selectedMode = ref(
    route.query.mode === "password" ? "password" : "id"
);

const emailDomains = [
    "naver.com",
    "gmail.com",
    "nate.com",
    "hanmail.net",
    "yahoo.co.kr",
    "kakao.com"
];

// 아이디 찾기 단계와 입력값을 관리한다.
const idStep = ref(1);
const idContactType = ref("phone");
const idPhone = ref("");
const idEmailId = ref("");
const idEmailDomain = ref("");
const memberName = ref("");
const memberDong = ref("");
const memberHo = ref("");
const idSecurityCode = ref("");
const foundLoginId = ref("");
const idError = ref("");

// 비밀번호 찾기 단계와 입력값을 관리한다.
const passwordStep = ref(1);
const passwordLoginId = ref("");
const passwordContactType = ref("phone");
const passwordPhone = ref("");
const passwordEmailId = ref("");
const passwordEmailDomain = ref("");
const passwordSecurityCode = ref("");
const newPassword = ref("");
const newPasswordConfirm = ref("");
const passwordError = ref("");

// 인증번호 유효시간, 재전송 제한시간, API 요청 상태를 관리한다.
const remainingSeconds = ref(0);
const resendSeconds = ref(0);
const isSubmitting = ref(false);

let verificationTimerId = null;
let resendTimerId = null;

const currentView = computed(() => {
    if (selectedMode.value === "password") {
        return [
            "password-id",
            "password-contact",
            "password-verify",
            "password-reset"
        ][passwordStep.value - 1];
    }

    return [
        "id-contact",
        "id-name",
        "id-verify",
        "id-result"
    ][idStep.value - 1];
});

const idContact = computed(() => formatContact(
    idContactType.value,
    idPhone.value,
    idEmailId.value,
    idEmailDomain.value
));

const passwordContact = computed(() => formatContact(
    passwordContactType.value,
    passwordPhone.value,
    passwordEmailId.value,
    passwordEmailDomain.value
));

const passwordsMatch = computed(() => (
    newPassword.value.length > 0
    && newPasswordConfirm.value.length > 0
    && newPassword.value === newPasswordConfirm.value
));

const passwordMatchMessage = computed(() => {
    if (!newPassword.value || !newPasswordConfirm.value) {
        return "";
    }

    return passwordsMatch.value
        ? "비밀번호가 일치합니다."
        : "비밀번호가 일치하지 않습니다.";
});

const isFinished = computed(() => (
    selectedMode.value === "id" && idStep.value === 4
));

const formattedVerificationTime = computed(() => {
    const minutes = Math.floor(remainingSeconds.value / 60);
    const seconds = remainingSeconds.value % 60;

    return `${minutes}:${String(seconds).padStart(2, "0")}`;
});

const verificationTimeMessage = computed(() => (
    remainingSeconds.value > 0
        ? `남은 시간 ${formattedVerificationTime.value}`
        : "보안코드가 만료되었습니다. 재전송해 주세요."
));

const numbersOnly = (value) => (
    String(value || "").replace(/\D/g, "").slice(0, 6)
);

const phoneNumbersOnly = (value) => (
    String(value || "").replace(/\D/g, "").slice(0, 11)
);

const contactTypeLabel = (type) => (
    type === "email" ? "이메일" : "전화번호"
);

function selectContactType(targetKey, contactType) {
    if (targetKey === "id") {
        if (idContactType.value === contactType) return;

        idContactType.value = contactType;
        idPhone.value = "";
        idEmailId.value = "";
        idEmailDomain.value = "";
        idError.value = "";
        return;
    }

    if (passwordContactType.value === contactType) return;

    passwordContactType.value = contactType;
    passwordPhone.value = "";
    passwordEmailId.value = "";
    passwordEmailDomain.value = "";
    passwordError.value = "";
}

function startVerificationTimer() {
    stopVerificationTimer();
    remainingSeconds.value = 180;

    verificationTimerId = window.setInterval(() => {
        remainingSeconds.value--;

        if (remainingSeconds.value <= 0) {
            remainingSeconds.value = 0;
            stopVerificationTimer();
        }
    }, 1000);
}

function startResendTimer() {
    stopResendTimer();
    resendSeconds.value = 0;
}

function stopVerificationTimer() {
    if (verificationTimerId !== null) {
        window.clearInterval(verificationTimerId);
        verificationTimerId = null;
    }
}

function stopResendTimer() {
    if (resendTimerId !== null) {
        window.clearInterval(resendTimerId);
        resendTimerId = null;
    }
}

onUnmounted(() => {
    stopVerificationTimer();
    stopResendTimer();
});

function formatContact(type, phone, emailId, emailDomain) {
    if (type === "email") {
        return `${emailId}@${emailDomain}`;
    }

    const number = phone.replace(/\D/g, "");

    return number.length === 11
        ? `${number.slice(0, 3)}-${number.slice(3, 7)}-${number.slice(7)}`
        : number;
}

function isValidContact(type, phone, emailId, emailDomain) {
    const phonePattern = /^010\d{8}$/;
    const emailIdPattern = /^[A-Za-z0-9._%+-]+$/;

    if (type === "phone") {
        return phonePattern.test(phone.replace(/\D/g, ""));
    }

    return emailIdPattern.test(emailId) && Boolean(emailDomain);
}

function goToIdNameStep() {
    if (!isValidContact(
        idContactType.value,
        idPhone.value,
        idEmailId.value,
        idEmailDomain.value
    )) {
        idError.value =
            `${contactTypeLabel(idContactType.value)} 형식을 확인해 주세요.`;
        return;
    }

    idError.value = "";
    idStep.value = 2;
}

async function sendIdSecurityCode() {
    if (isSubmitting.value || resendSeconds.value > 0) return;

    if (!memberName.value) {
        idError.value = "이름을 입력해 주세요.";
        return;
    }

    if (!memberDong.value || !memberHo.value) {
        idError.value = "동·호수를 입력해 주세요.";
        return;
    }

    try {
        isSubmitting.value = true;

        await sendAccountRecoveryCode(recoveryData(
            "FIND_ID",
            idContactType.value,
            idContact.value,
            {
                memName: memberName.value,
                dong: Number(memberDong.value),
                ho: Number(memberHo.value)
            }
        ));

        idError.value = "";
        idSecurityCode.value = "";
        startVerificationTimer();
        startResendTimer();
        idStep.value = 3;
    } catch (error) {
        idError.value = errorMessage(
            error,
            "보안코드를 발송하지 못했습니다."
        );
    } finally {
        isSubmitting.value = false;
    }
}

function handleUnitInput(event, target, maxLength) {
    const value = event.target.value
        .replace(/\D/g, "")
        .slice(0, maxLength);

    event.target.value = value;

    if (target === "dong") {
        memberDong.value = value;
    } else {
        memberHo.value = value;
    }
}

async function verifyIdSecurityCode() {
    if (isSubmitting.value) return;

    if (remainingSeconds.value === 0) {
        idError.value =
            "보안코드가 만료되었습니다. 재전송해 주세요.";
        return;
    }

    if (!/^\d{6}$/.test(idSecurityCode.value)) {
        idError.value = "보안코드 6자리를 입력해 주세요.";
        return;
    }

    try {
        isSubmitting.value = true;

        const response = await findAccountLoginId(recoveryData(
            "FIND_ID",
            idContactType.value,
            idContact.value,
            {
                memName: memberName.value,
                dong: Number(memberDong.value),
                ho: Number(memberHo.value),
                code: idSecurityCode.value
            }
        ));

        foundLoginId.value = response.data.loginId;
        idError.value = "";
        stopVerificationTimer();
        stopResendTimer();
        idStep.value = 4;
    } catch (error) {
        idError.value = errorMessage(
            error,
            "보안코드를 확인하지 못했습니다."
        );
    } finally {
        isSubmitting.value = false;
    }
}

function goToPasswordContactStep() {
    if (!passwordLoginId.value) {
        passwordError.value = "아이디를 입력해 주세요.";
        return;
    }

    passwordError.value = "";
    passwordStep.value = 2;
}

async function sendPasswordSecurityCode() {
    if (isSubmitting.value || resendSeconds.value > 0) return;

    if (!isValidContact(
        passwordContactType.value,
        passwordPhone.value,
        passwordEmailId.value,
        passwordEmailDomain.value
    )) {
        passwordError.value =
            `${contactTypeLabel(passwordContactType.value)} 형식을 확인해 주세요.`;
        return;
    }

    try {
        isSubmitting.value = true;

        await sendAccountRecoveryCode(recoveryData(
            "RESET_PASSWORD",
            passwordContactType.value,
            passwordContact.value,
            {
                loginId: passwordLoginId.value
            }
        ));

        passwordError.value = "";
        passwordSecurityCode.value = "";
        startVerificationTimer();
        startResendTimer();
        passwordStep.value = 3;
    } catch (error) {
        passwordError.value = errorMessage(
            error,
            "보안코드를 발송하지 못했습니다."
        );
    } finally {
        isSubmitting.value = false;
    }
}

function verifyPasswordSecurityCode() {
    if (isSubmitting.value) return;

    if (remainingSeconds.value === 0) {
        passwordError.value =
            "보안코드가 만료되었습니다. 재전송해 주세요.";
        return;
    }

    if (!/^\d{6}$/.test(passwordSecurityCode.value)) {
        passwordError.value =
            "보안코드 6자리를 입력해 주세요.";
        return;
    }

    passwordError.value = "";
    passwordStep.value = 4;
}

async function changePassword() {
    if (isSubmitting.value) return;

    const passwordPattern =
        /^(?=.*[A-Za-z])(?=.*\d)(?=.*[!@#$%^&*])[A-Za-z\d!@#$%^&*]{8,20}$/;

    if (!passwordPattern.test(newPassword.value)) {
        passwordError.value =
            "비밀번호는 영문, 숫자, 특수문자를 포함해 8~20자로 입력해 주세요.";
        return;
    }

    if (newPassword.value !== newPasswordConfirm.value) {
        passwordError.value = "새 비밀번호가 일치하지 않습니다.";
        return;
    }

    if (remainingSeconds.value === 0) {
        passwordError.value = "보안코드가 만료되었습니다. 다시 발송해 주세요.";
        passwordStep.value = 3;
        return;
    }

    try {
        isSubmitting.value = true;

        await resetAccountPassword(recoveryData(
            "RESET_PASSWORD",
            passwordContactType.value,
            passwordContact.value,
            {
                loginId: passwordLoginId.value,
                code: passwordSecurityCode.value,
                newPassword: newPassword.value
            }
        ));

        passwordError.value = "";
        stopVerificationTimer();

        await alertDialog({
            type: "success",
            title: "비밀번호 변경 완료",
            message: "변경이 완료되었습니다."
        });

        await router.push("/login");
    } catch (error) {
        const message = errorMessage(
            error,
            "비밀번호를 변경하지 못했습니다."
        );
        passwordError.value = message;
        if (message.includes("보안코드")) {
            passwordStep.value = 3;
        }
    } finally {
        isSubmitting.value = false;
    }
}

function recoveryData(purpose, channel, contact, additional = {}) {
    return {
        purpose,
        channel: channel.toUpperCase(),
        contact,
        ...additional
    };
}

function errorMessage(error, fallback) {
    return error.response?.data?.message
        || error.response?.data?.detail
        || error.response?.data?.error
        || fallback;
}

function backToIdNameStep() {
    stopVerificationTimer();
    stopResendTimer();

    remainingSeconds.value = 0;
    resendSeconds.value = 0;
    idSecurityCode.value = "";
    idError.value = "";
    idStep.value = 2;
}

function backToPasswordContactStep() {
    stopVerificationTimer();
    stopResendTimer();

    remainingSeconds.value = 0;
    resendSeconds.value = 0;
    passwordSecurityCode.value = "";
    passwordError.value = "";
    passwordStep.value = 2;
}
</script>

<style scoped>
.find-account-card {
    width: 100%;
    max-width: 540px;
    min-height: 530px;
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    box-sizing: border-box;
}

.find-account-brand {
    margin-bottom: 30px;
    color: #0369a1;
    font-size: 22px;
    font-weight: 900;
    font-style: italic;
    line-height: 1;
    letter-spacing: -1px;
    text-align: center;
    text-decoration: none;
}

.find-account-content {
    width: 100%;
    max-width: 400px;
    display: flex;
    flex-direction: column;
    gap: 18px;
}

.find-account-content .login-title {
    margin-bottom: 8px;
    font-size: 30px;
    font-weight: 900;
    line-height: 1.25;
    text-align: center;
}

.find-description {
    margin: -8px 0 6px;
    color: #64748b;
    font-size: 14px;
    line-height: 1.6;
    text-align: center;
}

.contact-method-form {
    display: flex;
    flex-direction: column;
    gap: 18px;
}

/* 아이디 찾기에서 동과 호수를 같은 행에 입력한다. */
.unit-fields {
    display: grid;
    grid-template-columns: repeat(2, minmax(0, 1fr));
    gap: 12px;
}

.contact-type-toggle {
    padding: 5px;
    display: grid;
    grid-template-columns: repeat(2, minmax(0, 1fr));
    gap: 5px;
    border-radius: 11px;
    background: #e2e8f0;
}

.contact-type-toggle button {
    height: 44px;
    border: 0;
    border-radius: 8px;
    cursor: pointer;
    color: #64748b;
    background: transparent;
    font-size: 14px;
    font-weight: 800;
}

.contact-type-toggle button.active {
    color: #ffffff;
    background: #0284c7;
    box-shadow: 0 5px 12px rgba(2, 132, 199, 0.22);
}

.email-input-group {
    display: grid;
    grid-template-columns: minmax(0, 1fr) auto minmax(0, 1fr);
    align-items: center;
    gap: 10px;
}

.email-input-group input,
.email-input-group select {
    width: 100%;
    min-width: 0;
    height: 48px;
    box-sizing: border-box;
}

.email-input-group select {
    padding: 0 12px;
    border: 1px solid var(--border-color);
    border-radius: 5px;
    outline: none;
    color: var(--text-color);
    background: #f7f9fa;
    font-size: 15px;
}

.email-input-group select:focus {
    border-color: var(--primary);
    box-shadow: 0 0 0 3px rgba(91, 124, 153, 0.15);
}

.email-at {
    color: #334155;
    font-size: 18px;
    font-weight: 800;
}

.step-next-button {
    min-width: 92px;
    height: 48px;
    padding: 0 24px;
    align-self: flex-end;
    border: 0;
    border-radius: 10px;
    cursor: pointer;
    color: #ffffff;
    background: #0284c7;
    font-size: 16px;
    font-weight: 800;
}

.step-next-button:hover {
    background: #0369a1;
}

.contact-summary,
.verification-target,
.account-result {
    padding: 15px 16px;
    display: flex;
    flex-direction: column;
    gap: 5px;
    border: 1px solid #dbeafe;
    border-radius: 11px;
    background: #f8fbff;
}

.contact-summary span,
.verification-target span,
.account-result span {
    color: #64748b;
    font-size: 13px;
}

.contact-summary strong,
.verification-target strong,
.account-result strong {
    color: #0f172a;
    font-size: 16px;
}

.phone-target {
    flex-direction: row;
    align-items: center;
    justify-content: space-between;
    gap: 12px;
}

.phone-target > div {
    min-width: 0;
    display: flex;
    flex-direction: column;
    gap: 5px;
}

.phone-target strong {
    overflow-wrap: anywhere;
}

.send-code-button,
.secondary-button {
    height: 46px;
    padding: 0 18px;
    flex: 0 0 auto;
    border: 1px solid #0284c7;
    border-radius: 10px;
    cursor: pointer;
    color: #0284c7;
    background: #ffffff;
    font-size: 15px;
    font-weight: 800;
    transition: transform 0.18s ease, color 0.18s ease, background-color 0.18s ease;
}

.send-code-button {
    min-width: 92px;
    height: 42px;
    border-radius: 8px;
}

.send-code-button:hover,
.secondary-button:hover {
    color: #ffffff;
    background: #0284c7;
    transform: translateY(-1px);
}

/* [email인증] 이전·발송·인증 버튼을 같은 너비와 높이로 맞춘다. */
.find-actions {
    display: grid;
    grid-template-columns: repeat(2, minmax(0, 1fr));
    gap: 10px;
}

.find-actions .secondary-button,
.find-actions .login-submit {
    width: 100%;
    height: 48px;
    margin-top: 0;
    box-sizing: border-box;
}

.find-error {
    margin: -4px 0 0;
    color: #dc2626;
    font-size: 13px;
    font-weight: 700;
}

/* [email인증] 새 비밀번호 일치 여부를 성공·오류 색상으로 구분한다. */
.password-match-message {
    margin: -4px 0 0;
    font-size: 14px;
    font-weight: 800;
}

.password-match-message.matched {
    color: #16a34a;
}

.password-match-message.mismatched {
    color: #dc2626;
}

.verification-time {
    margin: -8px 0 0;
    color: #0284c7;
    font-size: 14px;
    font-weight: 800;
    text-align: right;
}

.verification-time.expired {
    color: #dc2626;
}

.result-content {
    text-align: center;
}

.account-result {
    padding: 24px 18px;
}

.account-result strong {
    font-size: 22px;
}

.result-login-button {
    display: flex;
    align-items: center;
    justify-content: center;
    box-sizing: border-box;
    text-decoration: none;
}

/* [email인증] 아이디 조회 결과에서 비밀번호 찾기 링크를 구분해 표시한다. */
.password-find-link {
    margin: 18px auto 0;
    display: inline-block;
    color: #64748b;
    font-size: 15px;
    font-weight: 700;
    text-decoration: underline;
    text-underline-offset: 4px;
}

.password-find-link:hover {
    color: #0284c7;
}

.back-to-login {
    margin: 22px auto 0;
    display: inline-block;
    color: #64748b;
    font-size: 14px;
    font-weight: 700;
    text-align: center;
    text-decoration: underline;
    text-underline-offset: 4px;
}

.back-to-login:hover {
    color: #0284c7;
}

.send-code-button:disabled,
.secondary-button:disabled,
.login-submit:disabled,
.step-next-button:disabled {
    cursor: not-allowed;
    opacity: 0.55;
    transform: none;
}

.send-code-button:disabled:hover,
.secondary-button:disabled:hover {
    color: #0284c7;
    background: #ffffff;
    transform: none;
}

@media (max-width: 560px) {
    .find-account-card {
        min-height: auto;
        padding: 26px 18px;
        justify-content: flex-start;
    }

    .find-account-brand {
        margin-bottom: 22px;
        font-size: 20px;
    }

    .find-account-content {
        max-width: none;
        gap: 14px;
    }

    .find-account-content .login-title {
        margin-bottom: 4px;
        font-size: 24px;
    }

    .find-description {
        margin: -4px 0 2px;
        font-size: 13px;
        line-height: 1.5;
    }

    .contact-method-form {
        gap: 14px;
    }

    .contact-type-toggle button {
        padding: 0 6px;
        font-size: 13px;
    }

    .email-input-group {
        grid-template-columns: minmax(0, 1fr) minmax(0, 1fr);
        gap: 8px;
    }

    .email-at {
        display: none;
    }

    .form-field input,
    .email-input-group select {
        font-size: 16px;
    }

    .unit-fields {
        gap: 8px;
    }

    .contact-summary,
    .verification-target,
    .account-result {
        padding: 13px 14px;
    }

    .find-actions {
        grid-template-columns: repeat(2, minmax(0, 1fr));
        gap: 8px;
    }

    .find-actions .secondary-button,
    .find-actions .login-submit {
        padding: 0 8px;
        font-size: 14px;
    }

    .step-next-button {
        width: 100%;
        align-self: stretch;
    }

    .back-to-login {
        margin-top: 18px;
    }
}

@media (max-width: 400px) {
    .phone-target {
        align-items: stretch;
        flex-direction: column;
    }

    .phone-target .send-code-button {
        width: 100%;
    }
}
</style>
