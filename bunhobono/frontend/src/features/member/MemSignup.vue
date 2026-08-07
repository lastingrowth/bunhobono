<template>
    <section class="signup-card" :class="{ 'admin-signup-card': props.adminMode }">
        <div v-if="props.adminMode" class="signup-header">
            <div>
                <p>MEMBER MANAGEMENT</p>
                <h2>관리자 회원 추가</h2>
            </div>
            <button class="back-button" type="button" @click="router.push('/admin/members')">목록</button>
        </div>

        <div v-else class="login-brand">
            <div class="brand-symbol">P</div>
            <h1>아파트 주차관리 시스템</h1>
            <p>SMART PARKING SYSTEM</p>
        </div>

        <h2 v-if="!props.adminMode" class="login-title">입주민 회원가입</h2>

        <p v-if="needsAvailableUnit && availabilityLoading" class="availability-guide">가입 가능한 세대를 확인하고 있습니다.</p>
        <p v-else-if="needsAvailableUnit && availabilityError" class="availability-guide availability-error">{{ availabilityError }}</p>
        <p v-else-if="needsAvailableUnit && !hasAvailableUnits" class="availability-guide availability-error">
            현재 가입 가능한 세대가 없습니다. 관리사무소에 문의해주세요.
        </p>

        <form class="signup-form" novalidate @submit.prevent="signupGo">
            <div v-if="props.adminMode" class="form-row">
                <label class="form-field">
                    <span>가입유형</span>
                    <select v-model="member.role" @change="syncStatusWithRole">
                        <option value="RESIDENT">RESIDENT</option>
                        <option value="ADMIN">ADMIN</option>
                    </select>
                </label>

                <label class="form-field">
                    <span>상태</span>
                    <select v-model="member.memStatus">
                        <option
                            v-for="status in statusOptions"
                            :key="status.value"
                            :value="status.value" >
                            {{ status.label }}
                        </option>
                    </select>
                </label>
            </div>

            <label class="form-field">
                <span>이름</span>
                <input v-model="member.memName" type="text" minlength="2" maxlength="10" placeholder="한글 2~10자" required>
            </label>

            <label class="form-field">
                <span>아이디</span>
                <div class="input-action">
                    <input v-model="member.loginId" type="text" minlength="4" maxlength="20" placeholder="영문, 숫자 포함 4~20자" required>
                    <button type="button" @click="idCheck">중복 확인</button>
                </div>
            </label>

            <div class="form-row password-row">
                <label class="form-field password-field">
                    <span>비밀번호</span>
                    <input
                        type="password"
                        :value="member.loginPwd"
                        minlength="8"
                        maxlength="20"
                        placeholder="영문, 숫자, 특수문자 포함 8~20자"
                        required
                        @input="handlePasswordInput">
                </label>

                <label class="form-field password-field">
                    <span>비밀번호 확인</span>
                    <input
                        type="password"
                        :value="passwordConfirm"
                        :class="{
                            'password-match': passwordConfirm && passwordsMatch,
                            'password-mismatch': passwordConfirm && !passwordsMatch
                        }"
                        minlength="8"
                        maxlength="20"
                        placeholder="비밀번호를 다시 입력하세요"
                        required
                        @input="handlePasswordConfirmInput">
                    <small
                        v-if="passwordConfirm"
                        class="password-feedback"
                        :class="passwordsMatch ? 'is-match' : 'is-mismatch'">
                        {{ passwordsMatch ? '비밀번호가 일치합니다.' : '비밀번호가 일치하지 않습니다.' }}
                    </small>
                </label>
            </div>

            <div class="form-row">
                <label class="form-field">
                    <span>동</span>
                    <!-- 서버가 확인한 가입 가능 세대의 동만 표시한다. -->
                    <select v-model.number="member.dong" :disabled="props.adminMode && member.role === 'ADMIN' || !hasAvailableUnits" required @change="member.ho = ''">
                        <option disabled value="">동을 선택하세요</option>
                        <option v-if="props.adminMode && member.role === 'ADMIN'" :value="null">관리실</option>
                        <option v-for="dong in availableDongs" v-else :key="dong" :value="dong">{{ dong }}동</option>
                    </select>
                </label>

                <label class="form-field">
                    <span>호수</span>
                    <select v-model.number="member.ho" :disabled="props.adminMode && member.role === 'ADMIN' || !member.dong" required>
                        <option disabled value="">호수를 선택하세요</option>
                        <option v-if="props.adminMode && member.role === 'ADMIN'" :value="null">-</option>
                        <optgroup v-else label="1·2라인">
                            <option v-for="ho in line12HoOptions" :key="ho" :value="ho">{{ ho }}호</option>
                        </optgroup>
                    </select>
                </label>
            </div>

            <div class="form-field contact-field">
                <span>연락처</span>
                <div class="phone-input-action">
                    <div class="phone-fields">
                        <input type="text" inputmode="numeric" maxlength="3" :value="phoneParts.first" required @input="handlePhoneInput($event, 'first', 3)">
                        <span>-</span>
                        <input type="text" inputmode="numeric" maxlength="4" :value="phoneParts.middle" required @input="handlePhoneInput($event, 'middle', 4)">
                        <span>-</span>
                        <input type="text" inputmode="numeric" maxlength="4" :value="phoneParts.last" required @input="handlePhoneInput($event, 'last', 4)">
                    </div>
                    <button
                        type="button"
                        class="phone-auth-button"
                        :disabled="phoneSending || phoneVerified"
                        @click="sendPhoneAuthCode">
                        {{ phoneSending ? '발송 중' : phoneVerified ? '인증 완료' : phoneCodeSent ? '인증번호 재발송' : '인증번호 발송' }}
                    </button>
                </div>
                <span v-if="phoneVerified" class="phone-auth-success">전화번호 인증이 완료되었습니다.</span>
                <div v-if="phoneCodeSent && !phoneVerified" class="input-action phone-code-row">
                    <div class="phone-code-input-wrap">
                        <input
                            v-model="phoneAuthCode"
                            type="text"
                            inputmode="numeric"
                            maxlength="6"
                            placeholder="인증번호 6자리"
                            @input="handlePhoneCodeInput">
                        <span
                            class="phone-auth-timer"
                            :class="{ 'is-expired': phoneAuthRemainingSeconds === 0 }">
                            {{ phoneAuthTimerText }}
                        </span>
                    </div>
                    <button
                        type="button"
                        :disabled="phoneVerifying || phoneAuthRemainingSeconds === 0"
                        @click="verifyPhoneAuthCode">
                        {{ phoneVerifying ? '확인 중' : '인증 확인' }}
                    </button>
                </div>
            </div>

            <div class="form-field email-field">
                <span>이메일</span>
                <div class="email-fields">
                    <input
                        v-model.trim="emailParts.id"
                        type="text"
                        maxlength="64"
                        placeholder="이메일 아이디"
                        required>
                    <span class="email-at">@</span>
                    <select v-model="emailParts.domain" required>
                        <option disabled value="">이메일 선택</option>
                        <option value="naver.com">naver.com</option>
                        <option value="gmail.com">gmail.com</option>
                        <option value="nate.com">nate.com</option>
                        <option value="hanmail.net">hanmail.net</option>
                        <option value="yahoo.co.kr">yahoo.co.kr</option>
                        <option value="kakao.com">kakao.com</option>
                    </select>
                </div>
            </div>

            <button class="login-submit" type="submit" :disabled="needsAvailableUnit && (!hasAvailableUnits || availabilityLoading)">
                {{ props.adminMode ? '회원 추가' : '회원가입' }}
            </button>
        </form>

        <div v-if="!props.adminMode" class="signup-guide">
            <span>이미 계정이 있으신가요?</span>
            <RouterLink class="signup-link" to="/login">로그인</RouterLink>
        </div>
    </section>
</template>

<script setup>
import { computed, onBeforeUnmount, onMounted, reactive, ref } from "vue";
import { useRouter } from "vue-router";
import { useDialog } from "@/shared/alert/useDialog";
import { useMemStore } from "./memStore";

const router = useRouter();
const store = useMemStore();
const { alertDialog } = useDialog();
const props = defineProps({
    adminMode: {
        type: Boolean,
        default: false
    }
});
const availabilityLoading = ref(true);
const availabilityError = ref("");

const member = ref({
    role: "RESIDENT",
    memName: "",
    dong: "",
    ho: "",
    memPhone: "",
    email: "",
    loginId: "",
    loginPwd: "",
    // 공개 입주민 회원가입은 관리자 승인 전까지 PENDING 상태로 저장한다.
    // 승인 대기 중에도 해당 동·호수는 가입 신청자가 점유한 상태다.
    memStatus: props.adminMode ? "ACTIVE" : "PENDING",
});
const idChecked = ref(false);
const checkedLoginId = ref("");
const passwordConfirm = ref("");
const phoneParts = reactive({ first: "", middle: "", last: "" });
const emailParts = reactive({ id: "", domain: "" });
const phoneAuthCode = ref("");
const phoneCodeSent = ref(false);
const phoneVerified = ref(false);
const phoneSending = ref(false);
const phoneVerifying = ref(false);
const phoneAuthRemainingSeconds = ref(0);
let phoneAuthTimerId = null;

const needsAvailableUnit = computed(() => !props.adminMode || member.value.role === "RESIDENT");
const dialogTheme = computed(() => props.adminMode ? "admin" : "resident");
const passwordsMatch = computed(() => member.value.loginPwd === passwordConfirm.value);
const phoneAuthTimerText = computed(() => {
    const minutes = Math.floor(phoneAuthRemainingSeconds.value / 60);
    const seconds = phoneAuthRemainingSeconds.value % 60;
    return `${String(minutes).padStart(2, "0")}:${String(seconds).padStart(2, "0")}`;
});

const stopPhoneAuthTimer = () => {
    if (phoneAuthTimerId !== null) {
        clearInterval(phoneAuthTimerId);
        phoneAuthTimerId = null;
    }
};

const startPhoneAuthTimer = () => {
    stopPhoneAuthTimer();
    const expiresAt = Date.now() + 3 * 60 * 1000;
    phoneAuthRemainingSeconds.value = 180;

    phoneAuthTimerId = window.setInterval(() => {
        phoneAuthRemainingSeconds.value = Math.max(
            0,
            Math.ceil((expiresAt - Date.now()) / 1000)
        );

        if (phoneAuthRemainingSeconds.value === 0) {
            stopPhoneAuthTimer();
        }
    }, 1000);
};

onBeforeUnmount(stopPhoneAuthTimer);

const statusOptions = computed(() => {
    if (member.value.role === 'ADMIN') {
        return [
            { value: 'ACTIVE', label: '근무' },
            { value: 'ON_LEAVE', label: '휴직' },
            { value: 'INACTIVE', label: '퇴사' },
        ]
    }

    return [
        { value: 'ACTIVE', label: '현재 회원' },
    ]
})

const hasAvailableUnits = computed(() => store.availableSignupUnits.length > 0);
const availableDongs = computed(() => [
    ...new Set(store.availableSignupUnits.map((unit) => Number(unit.dong)))
].sort((left, right) => left - right));
const selectedDongUnits = computed(() => store.availableSignupUnits.filter(
    (unit) => Number(unit.dong) === Number(member.value.dong)
));
const line12HoOptions = computed(() => selectedDongUnits.value
    .map((unit) => Number(unit.ho))
    .sort((left, right) => left - right));

const loadAvailableUnits = async () => {
    availabilityLoading.value = true;
    availabilityError.value = "";
    try {
        await store.loadAvailableSignupUnits();
    } catch (error) {
        console.error(error);
        availabilityError.value = "가입 가능한 세대를 불러오지 못했습니다.";
    } finally {
        availabilityLoading.value = false;
    }
};

onMounted(loadAvailableUnits);

// 관리자 모드에서 가입유형에 맞는 상태와 동·호수 기본값을 설정한다.
const syncStatusWithRole = () => {
    // role을 바꾸면 해당 role에서 사용할 수 있는 첫 번째 상태값으로 초기화한다.
    member.value.memStatus = statusOptions.value[0]?.value ?? 'ACTIVE'
    member.value.dong = member.value.role === "ADMIN" ? null : "";
    member.value.ho = member.value.role === "ADMIN" ? null : "";
};

// 외부 회원가입과 관리자 회원 추가에 공통으로 적용하는 정규식.
const namePattern = /^(?=.*[가-힣])[가-힣]{2,10}$/;
const loginIdPattern = /^(?=.*[A-Za-z])(?=.*\d)[A-Za-z\d]{4,20}$/;
const passwordPattern = /^(?=.*[A-Za-z])(?=.*\d)(?=.*[!@#$%^&*])[A-Za-z\d!@#$%^&*]{8,20}$/;
const emailIdPattern = /^[A-Za-z0-9._%+-]{1,64}$/;

const validateSignupFields = async () => {
    if (!namePattern.test(member.value.memName)) {
        await alertDialog({
            theme: dialogTheme.value,
            type: "warning",
            title: "이름 형식 확인",
            message: "한글 2~10자로 입력하세요."
        });
        return false;
    }
    if (phoneParts.first.length !== 3 || phoneParts.middle.length !== 4 || phoneParts.last.length !== 4) {
        await alertDialog({
            theme: dialogTheme.value,
            type: "warning",
            title: "연락처 형식 확인",
            message: "연락처를 정확히 입력하세요."
        });
        return false;
    }
    if (needsAvailableUnit.value && (!member.value.dong || !member.value.ho)) {
        await alertDialog({
            theme: dialogTheme.value,
            type: "warning",
            title: "동·호수 확인",
            message: "동과 호수를 선택해주세요."
        });
        return false;
    }
    if (!loginIdPattern.test(member.value.loginId)) {
        await alertDialog({
            theme: dialogTheme.value,
            type: "warning",
            title: "아이디 형식 확인",
            message: "아이디는 영문과 숫자를 조합해 4~20자로 입력하세요."
        });
        return false;
    }

    if (!idChecked.value || checkedLoginId.value !== member.value.loginId) {
        await alertDialog({
            theme: dialogTheme.value,
            type: "warning",
            title: "아이디 중복 확인",
            message: "아이디 중복확인을 해주세요."
        });
        return false;
    }

    if (!passwordPattern.test(member.value.loginPwd)) {
        await alertDialog({
            theme: dialogTheme.value,
            type: "warning",
            title: "비밀번호 형식 확인",
            message: "비밀번호는 영문+숫자+특수기호 조합으로 8~20자로 입력하세요."
        });
        return false;
    }

    if (!passwordConfirm.value || !passwordsMatch.value) {
        await alertDialog({
            theme: dialogTheme.value,
            type: "warning",
            title: "비밀번호 확인",
            message: "비밀번호를 동일하게 다시 입력해 주세요."
        });
        return false;
    }

    if (phoneParts.first.length !== 3 || phoneParts.middle.length !== 4 || phoneParts.last.length !== 4) {
        await alertDialog({
            theme: dialogTheme.value,
            type: "warning",
            title: "연락처 형식 확인",
            message: "연락처를 정확히 입력하세요."
        });
        return false;
    }

    if (!emailIdPattern.test(emailParts.id) || !emailParts.domain) {
        await alertDialog({
            theme: dialogTheme.value,
            type: "warning",
            title: "이메일 형식 확인",
            message: "이메일 주소를 정확히 입력하세요."
        });
        return false;
    }

    if (!phoneVerified.value) {
        await alertDialog({
            theme: dialogTheme.value,
            type: "warning",
            title: "전화번호 인증 확인",
            message: "전화번호 인증을 완료해 주세요."
        });
        return false;
    }

    return true;
};

// 연락처 세 칸에는 정해진 길이만큼 숫자만 입력한다.
const handlePhoneInput = (event, part, maxLength) => {
    const numericValue = event.target.value.replace(/\D/g, "").slice(0, maxLength);
    event.target.value = numericValue;
    phoneParts[part] = numericValue;
    phoneAuthCode.value = "";
    phoneCodeSent.value = false;
    phoneVerified.value = false;
    phoneAuthRemainingSeconds.value = 0;
    stopPhoneAuthTimer();
};

const getPhoneNumber = () => `${phoneParts.first}${phoneParts.middle}${phoneParts.last}`;

const handlePhoneCodeInput = (event) => {
    phoneAuthCode.value = event.target.value.replace(/\D/g, "").slice(0, 6);
    event.target.value = phoneAuthCode.value;
};

// [sms인증] 입력한 전화번호로 인증번호를 발송한다.
const sendPhoneAuthCode = async () => {
    const phone = getPhoneNumber();
    if (!/^010\d{8}$/.test(phone)) {
        await alertDialog({
            theme: dialogTheme.value,
            type: "warning",
            title: "연락처 형식 확인",
            message: "휴대전화 번호를 정확히 입력하세요."
        });
        return;
    }

    phoneSending.value = true;
    try {
        await store.sendPhoneCode(phone);
        phoneCodeSent.value = true;
        phoneAuthCode.value = "";
        startPhoneAuthTimer();
        await alertDialog({
            theme: dialogTheme.value,
            type: "success",
            title: "인증번호 발송",
            message: "인증번호를 발송했습니다. 3분 안에 입력해 주세요."
        });
    } catch (error) {
        await alertDialog({
            theme: dialogTheme.value,
            type: "error",
            title: "인증번호 발송 실패",
            message: error.response?.data?.message || error.response?.data?.error || "인증번호를 발송하지 못했습니다."
        });
    } finally {
        phoneSending.value = false;
    }
};

// [sms인증] 문자로 받은 인증번호를 확인한다.
const verifyPhoneAuthCode = async () => {
    if (!/^\d{6}$/.test(phoneAuthCode.value)) {
        await alertDialog({
            theme: dialogTheme.value,
            type: "warning",
            title: "인증번호 확인",
            message: "인증번호 6자리를 입력해 주세요."
        });
        return;
    }

    phoneVerifying.value = true;
    try {
        await store.verifyPhoneCode(getPhoneNumber(), phoneAuthCode.value);
        phoneVerified.value = true;
        stopPhoneAuthTimer();
        await alertDialog({
            theme: dialogTheme.value,
            type: "success",
            title: "전화번호 인증 완료",
            message: "전화번호가 인증되었습니다."
        });
    } catch (error) {
        await alertDialog({
            theme: dialogTheme.value,
            type: "error",
            title: "인증번호 확인 실패",
            message: error.response?.data?.message || error.response?.data?.error || "인증번호를 확인하지 못했습니다."
        });
    } finally {
        phoneVerifying.value = false;
    }
};

// 비밀번호에는 허용된 영문, 숫자, 특수문자만 입력한다.
const sanitizePassword = (event) => {
    const passwordValue = event.target.value.replace(/[^A-Za-z\d!@#$%^&*]/g, "").slice(0, 20);
    event.target.value = passwordValue;
    return passwordValue;
};

const handlePasswordInput = (event) => {
    member.value.loginPwd = sanitizePassword(event);
};

const handlePasswordConfirmInput = (event) => {
    passwordConfirm.value = sanitizePassword(event);
};

// 아이디 중복확인
const idCheck = async () => {
    if (!loginIdPattern.test(member.value.loginId)) {
        await alertDialog({
            theme: dialogTheme.value,
            type: "warning",
            title: "아이디 형식 확인",
            message: "아이디는 영문과 숫자를 조합해 4~20자로 입력하세요."
        });
        return;
    }

    const exists = await store.idCheck(member.value.loginId);

    if (exists) {
        // true = 이미 존재
        idChecked.value = false;
        checkedLoginId.value = "";
        await alertDialog({
            theme: dialogTheme.value,
            type: "warning",
            title: "아이디 중복 확인",
            message: "이미 사용 중인 아이디입니다."
        });
    } else {
        // false = 사용 가능
        idChecked.value = true;
        checkedLoginId.value = member.value.loginId;
        await alertDialog({
            theme: dialogTheme.value,
            type: "success",
            title: "아이디 사용 가능",
            message: "사용 가능한 아이디입니다."
        });
    }    
}

const signupGo = async () => {

    if (!(await validateSignupFields())) return;

    member.value.memPhone = `${phoneParts.first}-${phoneParts.middle}-${phoneParts.last}`;
    member.value.email = `${emailParts.id}@${emailParts.domain}`;

    try {
        await store.signup(member.value);

        await alertDialog({
            theme: dialogTheme.value,
            type: "success",
            title: props.adminMode ? "회원 추가 완료" : "회원가입 완료",
            message: props.adminMode ? "회원이 추가되었습니다." : "회원등록 성공"
        });
        await router.push(props.adminMode ? "/admin/members" : "/login");
    } catch (e) {
        console.error(e);
        await alertDialog({
            theme: dialogTheme.value,
            type: "error",
            title: "회원등록 실패",
            message: e.response?.data?.message || e.response?.data?.error || "회원등록 실패"
        });
        if (needsAvailableUnit.value) {
            member.value.dong = "";
            member.value.ho = "";
            await loadAvailableUnits();
        }
    }
};
</script>

<style scoped>
.signup-card {
    --signup-placeholder-size: 15px;
    width: 100%;
    max-width: 560px;
    padding: 38px 40px 32px;
    border: 1px solid rgba(255, 255, 255, 0.75);
    border-radius: 24px;
    background: rgba(255, 255, 255, 0.94);
    box-shadow: 0 24px 64px rgba(15, 23, 42, 0.18);
}

.admin-signup-card {
    max-width: 940px;
    padding: 30px 32px 34px;
    margin: 20px auto 40px;
    border-radius: 18px;
}

.admin-signup-card .signup-header {
    margin-bottom: 18px;
}

.admin-signup-card .signup-form {
    display: grid;
    grid-template-columns: repeat(2, minmax(0, 1fr));
    row-gap: 16px;
    column-gap: 20px;
    margin-right: auto;
    margin-left: auto;
}

.admin-signup-card .signup-form > .form-row {
    display: contents;
}

.admin-signup-card .signup-form > .password-row {
    display: grid;
    grid-column: 1 / -1;
    grid-template-columns: repeat(2, minmax(0, 1fr));
    gap: 20px;
}

.admin-signup-card .form-field {
    min-width: 0;
    gap: 10px;
}

.admin-signup-card .form-field input,
.admin-signup-card .form-field select,
.admin-signup-card .phone-fields input {
    height: 46px;
}

.admin-signup-card .login-submit {
    grid-column: 1 / -1;
    height: 50px;
}

:global(.admin-layout .signup-card.admin-signup-card) {
    border: 1px solid var(--admin-line) !important;
    background: var(--admin-surface) !important;
    box-shadow: 0 18px 44px rgba(0, 0, 0, 0.28) !important;
}

:global(.admin-layout .admin-signup-card .signup-header p),
:global(.admin-layout .admin-signup-card .signup-header h2),
:global(.admin-layout .admin-signup-card .form-field > span),
:global(.admin-layout .admin-signup-card .phone-fields > span) {
    color: var(--admin-ink) !important;
}

:global(.admin-layout .admin-signup-card .form-field input),
:global(.admin-layout .admin-signup-card .form-field select) {
    border-color: var(--admin-line) !important;
    color: var(--admin-ink) !important;
    background: var(--admin-surface-muted) !important;
}

:global(.admin-layout .admin-signup-card .form-field input::placeholder) {
    color: var(--admin-muted) !important;
}

:global(.admin-layout .admin-signup-card .login-submit) {
    color: #171b1f !important;
    background: var(--admin-accent) !important;
    box-shadow: 0 8px 20px rgba(0, 0, 0, 0.25) !important;
}

:global(.admin-layout .admin-signup-card .back-button),
:global(.admin-layout .admin-signup-card .phone-auth-button),
:global(.admin-layout .admin-signup-card .input-action button),
:global(.admin-layout .admin-signup-card .login-submit) {
    font-size: 15px !important;
}

.signup-header {
    margin-bottom: 24px;
    padding-bottom: 18px;
    display: flex;
    justify-content: space-between;
    align-items: center;
    gap: 16px;
    border-bottom: 1px solid var(--border-color);
}

.signup-header p {
    margin: 0 0 6px;
    font-size: 13px;
    font-weight: 700;
    letter-spacing: 2px;
    color: var(--primary);
}

.signup-header h2 {
    margin: 0;
    font-size: 28px;
    letter-spacing: -0.6px;
    color: var(--heading-color);
}

.back-button {
    height: 42px;
    padding: 0 18px;
    border: 1px solid var(--border-color);
    border-radius: 10px;
    cursor: pointer;
    font-size: 15px;
    font-weight: 700;
    color: var(--text-color);
    background: var(--bg-header);
    transition: transform 0.18s ease, border-color 0.18s ease, background-color 0.18s ease;
}

.back-button:hover {
    border-color: var(--primary);
    background: #f1f5f9;
    transform: translateY(-1px);
}

.signup-form {
    display: flex;
    flex-direction: column;
    gap: 16px;
}

.availability-guide {
    margin: 0 0 20px;
    padding: 12px 15px;
    border: 1px solid #dbeafe;
    border-radius: 10px;
    color: var(--text-muted);
    background: #f7fbff;
    font-size: 14px;
}

.availability-error {
    color: #dc2626;
    background: #fef2f2;
}

.form-row {
    display: grid;
    grid-template-columns: 1fr 1fr;
    gap: 16px;
}

.form-field {
    min-width: 0;
    gap: 10px;
}

.form-field > span {
    font-size: 15px;
    font-weight: 800;
    letter-spacing: -0.1px;
}

.form-field input,
.form-field select {
    border-radius: 10px;
    font-size: 16px;
    transition: border-color 0.18s ease, box-shadow 0.18s ease, background-color 0.18s ease;
}

.form-field input:hover,
.form-field select:hover {
    border-color: #94a3b8;
}

.form-field input::placeholder {
    color: #64748b;
    font-size: var(--signup-placeholder-size);
    font-weight: 500;
    letter-spacing: -0.25px;
    opacity: 1;
}

:global(.admin-layout .admin-signup-card .form-field input::placeholder) {
    color: var(--admin-muted) !important;
    font-size: var(--signup-placeholder-size) !important;
    font-weight: 600;
    opacity: 1 !important;
}

.form-field select:invalid {
    color: #64748b;
    font-size: var(--signup-placeholder-size);
    font-weight: 500;
}

:global(.admin-layout .admin-signup-card .form-field select:invalid) {
    color: var(--admin-muted) !important;
    font-size: var(--signup-placeholder-size) !important;
}

.form-field select {
    width: 100%;
    height: 48px;
    padding: 0 14px;
    border: 1px solid var(--border-color);
    border-radius: 10px;
    outline: none;
    font-size: 16px;
    color: var(--text-color);
    background: #f8fafc;
}

.form-field select:focus {
    border-color: var(--primary);
    box-shadow: 0 0 0 3px rgba(14, 165, 233, 0.15);
}

.password-row {
    grid-template-columns: repeat(2, minmax(0, 1fr));
    align-items: start;
}

.password-field {
    align-content: start;
}

.password-field input {
    transition: border-color 0.2s ease, box-shadow 0.2s ease, background-color 0.2s ease;
}

.password-field input.password-match {
    border-color: #22c55e;
    background: #f0fdf4;
    box-shadow: 0 0 0 3px rgba(34, 197, 94, 0.12);
}

.password-field input.password-mismatch {
    border-color: #ef4444;
    background: #fef2f2;
    box-shadow: 0 0 0 3px rgba(239, 68, 68, 0.1);
}

.password-feedback {
    min-height: 18px;
    margin-top: 2px;
    display: flex;
    align-items: center;
    gap: 6px;
    font-size: 13px;
    font-weight: 700;
}

.password-feedback::before {
    content: "";
    width: 6px;
    height: 6px;
    flex: 0 0 6px;
    border-radius: 50%;
    background: currentColor;
}

.password-feedback.is-match {
    color: #15803d;
}

.password-feedback.is-mismatch {
    color: #dc2626;
}

.phone-fields {
    display: grid;
    grid-template-columns: 1fr auto 1fr auto 1fr;
    align-items: center;
    gap: 8px;
}

.phone-fields input {
    width: 100%;
    text-align: center;
}

.phone-fields span {
    color: var(--text-muted);
}

.phone-input-action {
    display: grid;
    grid-template-columns: minmax(0, 1fr) 140px;
    align-items: center;
    gap: 8px;
}

.phone-auth-button {
    width: 140px;
    height: 46px;
    min-height: 46px;
    padding: 0 12px;
    justify-self: end;
    border: 1px solid var(--primary);
    border-radius: 10px;
    cursor: pointer;
    font-size: 15px;
    font-weight: 700;
    color: var(--primary);
    background: var(--bg-header);
    transition: transform 0.18s ease, color 0.18s ease, background-color 0.18s ease;
}

.phone-auth-button:not(:disabled):hover {
    color: var(--text-white);
    background: var(--primary);
    transform: translateY(-1px);
}

.phone-auth-button:disabled {
    cursor: default;
    opacity: 0.65;
}

.phone-auth-success {
    margin-top: 2px;
    color: #16a34a;
    font-size: 14px;
    font-weight: 700;
}

.phone-code-row {
    margin-top: 8px;
}

.phone-code-input-wrap {
    position: relative;
    min-width: 0;
}

.phone-code-input-wrap input {
    width: 100%;
    padding-right: 72px;
}

.phone-auth-timer {
    position: absolute;
    top: 50%;
    right: 14px;
    transform: translateY(-50%);
    color: #2563eb;
    font-size: 14px;
    font-weight: 800;
    font-variant-numeric: tabular-nums;
    pointer-events: none;
}

.phone-auth-timer.is-expired {
    color: #dc2626;
}

:global(.admin-layout .admin-signup-card .phone-auth-timer) {
    color: var(--admin-accent) !important;
}

:global(.admin-layout .admin-signup-card .phone-auth-timer.is-expired) {
    color: #f87171 !important;
}

.contact-field {
    grid-column: 1 / -1;
}

.email-field {
    grid-column: 1 / -1;
}

.email-fields {
    display: grid;
    grid-template-columns: minmax(0, 1fr) auto minmax(0, 1fr);
    align-items: center;
    gap: 10px;
}

.email-at {
    color: var(--text-muted);
    font-size: 18px;
    font-weight: 800;
}

:global(.admin-layout .admin-signup-card .email-at) {
    color: var(--admin-ink) !important;
}

.input-action {
    display: grid;
    grid-template-columns: 1fr auto;
    gap: 8px;
    align-items: stretch;
}

.input-action button {
    width: 140px;
    height: 46px;
    padding: 0 12px;
    border: 1px solid var(--primary);
    border-radius: 10px;
    cursor: pointer;
    font-size: 15px;
    font-weight: 700;
    color: var(--primary);
    background: var(--bg-header);
    white-space: nowrap;
    transition: transform 0.18s ease, color 0.18s ease, background-color 0.18s ease;
}

.input-action button:hover {
    color: var(--text-white);
    background: var(--primary);
    transform: translateY(-1px);
}

.login-submit {
    margin-top: 4px;
    border-radius: 11px;
    letter-spacing: 0.2px;
    font-size: 17px;
    transition: transform 0.18s ease, box-shadow 0.18s ease, background-color 0.18s ease;
}

.login-submit:hover:not(:disabled) {
    transform: translateY(-1px);
    box-shadow: 0 10px 22px rgba(15, 23, 42, 0.24);
}

.login-submit:disabled {
    cursor: not-allowed;
    opacity: 0.58;
}

.signup-card .login-brand h1 {
    font-size: 30px;
}

.signup-card .login-brand p {
    font-size: 13px;
}

.signup-card .login-title {
    font-size: 22px;
}

.signup-card .signup-guide {
    font-size: 15px;
}

@media (min-width: 801px) and (max-height: 800px) {
    :global(.auth-layout) .signup-card:not(.admin-signup-card) .form-field input,
    :global(.auth-layout) .signup-card:not(.admin-signup-card) .form-field select,
    :global(.auth-layout) .signup-card:not(.admin-signup-card) .phone-fields input {
        height: 48px;
    }
}

@media (max-width: 700px) {
    .signup-card {
        padding: 28px 24px;
    }

    .admin-signup-card .signup-form {
        grid-template-columns: 1fr;
    }

    .admin-signup-card .signup-form > .password-row {
        grid-template-columns: 1fr;
    }

    .form-row {
        grid-template-columns: 1fr;
    }

    .phone-input-action {
        grid-template-columns: 1fr;
    }

    .phone-auth-button {
        width: auto;
    }

    .email-fields {
        grid-template-columns: minmax(0, 1fr) auto minmax(0, 1fr);
        gap: 7px;
    }

    .input-action {
        grid-template-columns: minmax(0, 1fr) auto;
    }

}
</style>
