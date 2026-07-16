<template>
    <section class="signup-card">
        <div class="login-brand">
            <div class="brand-symbol">P</div>
            <h1>아파트 주차관리 시스템</h1>
            <p>SMART PARKING SYSTEM</p>
        </div>

        <h2 class="login-title">입주민 회원가입</h2>

        <p v-if="availabilityLoading" class="availability-guide">가입 가능한 세대를 확인하고 있습니다.</p>
        <p v-else-if="availabilityError" class="availability-guide availability-error">{{ availabilityError }}</p>
        <p v-else-if="!hasAvailableUnits" class="availability-guide availability-error">
            현재 가입 가능한 세대가 없습니다. 관리사무소에 문의해주세요.
        </p>

        <form class="signup-form" @submit.prevent="signupGo">
            <label class="form-field">
                <span>이름</span>
                <input v-model="member.memName" type="text" minlength="2" maxlength="20" placeholder="한글+숫자 조합 2~20자" required @blur="normalizeName">
            </label>

            <div class="form-row">
                <label class="form-field">
                    <span>동</span>
                    <!-- 서버가 확인한 가입 가능 세대의 동만 표시한다. -->
                    <select v-model.number="member.memDong" :disabled="!hasAvailableUnits" required @change="member.memHo = ''">
                        <option disabled value="">동을 선택하세요</option>
                        <option v-for="dong in availableDongs" :key="dong" :value="dong">{{ dong }}동</option>
                    </select>
                </label>

                <label class="form-field">
                    <span>호수</span>
                    <select v-model.number="member.memHo" :disabled="!member.memDong" required>
                        <option disabled value="">호수를 선택하세요</option>
                        <optgroup label="1·2라인">
                            <option v-for="ho in line12HoOptions" :key="ho" :value="ho">{{ ho }}호</option>
                        </optgroup>
                        <optgroup label="3·4라인">
                            <option v-for="ho in line34HoOptions" :key="ho" :value="ho">{{ ho }}호</option>
                        </optgroup>
                    </select>
                </label>
            </div>

            <div class="form-field">
                <span>연락처</span>
                <div class="phone-fields">
                    <input type="text" inputmode="numeric" maxlength="3" :value="phoneParts.first" required @input="handlePhoneInput($event, 'first', 3)">
                    <span>-</span>
                    <input type="text" inputmode="numeric" maxlength="4" :value="phoneParts.middle" required @input="handlePhoneInput($event, 'middle', 4)">
                    <span>-</span>
                    <input type="text" inputmode="numeric" maxlength="4" :value="phoneParts.last" required @input="handlePhoneInput($event, 'last', 4)">
                </div>
            </div>

            <label class="form-field">
                <span>아이디</span>
                <div class="input-action">
                    <input v-model="member.loginId" type="text" minlength="3" maxlength="20" placeholder="영문+숫자 3~20자" required>
                    <button type="button" @click="idCheck">중복 확인</button>
                </div>
            </label>

            <label class="form-field">
                <span>비밀번호</span>
                <input type="password" inputmode="numeric" :value="member.loginPwd" minlength="4" maxlength="20" autocomplete="new-password" placeholder="숫자 4~20자" required @input="handlePasswordInput">
            </label>

            <button class="login-submit" type="submit" :disabled="!hasAvailableUnits || availabilityLoading">회원가입</button>
        </form>

        <div class="signup-guide">
            <span>이미 계정이 있으신가요?</span>
            <RouterLink class="signup-link" to="/login">로그인</RouterLink>
        </div>
    </section>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from "vue";
import { useRouter } from "vue-router";
import { useMemStore } from "./memStore";

const router = useRouter();
const store = useMemStore();
const availabilityLoading = ref(true);
const availabilityError = ref("");

const member = ref({
    // 외부 회원가입은 관리자 승인 전까지 PENDING 역할로 고정한다.
    role: "PENDING",
    memName: "",
    memDong: "",
    memHo: "",
    memPhone: "",
    loginId: "",
    loginPwd: "",
    // 공개 입주민 회원가입의 초기 상태는 항상 거주로 고정.
    memStatus: "거주",
});
const idChecked = ref(false);
const checkedLoginId = ref("");
const phoneParts = reactive({ first: "", middle: "", last: "" });

const hasAvailableUnits = computed(() => store.availableSignupUnits.length > 0);
const availableDongs = computed(() => [
    ...new Set(store.availableSignupUnits.map((unit) => Number(unit.memDong)))
].sort((left, right) => left - right));
const selectedDongUnits = computed(() => store.availableSignupUnits.filter(
    (unit) => Number(unit.memDong) === Number(member.value.memDong)
));
const line12HoOptions = computed(() => selectedDongUnits.value
    .map((unit) => Number(unit.memHo))
    .filter((ho) => [1, 2].includes(ho % 100))
    .sort((left, right) => left - right));
const line34HoOptions = computed(() => selectedDongUnits.value
    .map((unit) => Number(unit.memHo))
    .filter((ho) => [3, 4].includes(ho % 100))
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

// =====
// 아이디는 영문과 숫자를 모두 포함하고 비밀번호는 숫자만 허용한다.
const loginIdPattern = /^(?=.*[A-Za-z])(?=.*\d)[A-Za-z\d]{3,20}$/;
const passwordPattern = /^\d{4,20}$/;
const namePattern = /^(?=.*[가-힣])(?=.*\d)[가-힣\d]{2,20}$/;

const validateSignupFields = () => {
    if (!namePattern.test(member.value.memName)) {
        alert("이름은 한글과 숫자를 조합해 2~20자로 입력하세요.");
        return false;
    }
    if (phoneParts.first.length !== 3 || phoneParts.middle.length !== 4 || phoneParts.last.length !== 4) {
        alert("연락처를 정확히 입력하세요.");
        return false;
    }
    if (!loginIdPattern.test(member.value.loginId)) {
        alert("아이디는 영문과 숫자를 조합해 3~20자로 입력하세요.");
        return false;
    }
    if (!passwordPattern.test(member.value.loginPwd)) {
        alert("비밀번호는 숫자 4~20자로 입력하세요.");
        return false;
    }
    return true;
};
// =====

// 연락처 세 칸에는 정해진 길이만큼 숫자만 입력한다.
const handlePhoneInput = (event, part, maxLength) => {
    const numericValue = event.target.value.replace(/\D/g, "").slice(0, maxLength);
    event.target.value = numericValue;
    phoneParts[part] = numericValue;
};

// 한글 조합이 끝난 뒤 이름에서 한글과 숫자 이외의 문자를 제거한다.
const normalizeName = (event) => {
    const nameValue = event.target.value.replace(/[^가-힣0-9]/g, "").slice(0, 20);
    event.target.value = nameValue;
    member.value.memName = nameValue;
};

// 비밀번호에는 숫자만 입력한다.
const handlePasswordInput = (event) => {
    const numericValue = event.target.value.replace(/\D/g, "").slice(0, 20);
    event.target.value = numericValue;
    member.value.loginPwd = numericValue;
};

// 아이디 중복확인
const idCheck = async () => {
    if (!loginIdPattern.test(member.value.loginId)) {
        alert("아이디는 영문과 숫자를 조합해 3~20자로 입력하세요.");
        return;
    }

    const exists = await store.idCheck(member.value.loginId);

    if (exists) {
        // true = 이미 존재
        idChecked.value = false;
        checkedLoginId.value = "";
        alert("이미 사용중인 아이디입니다.");
    } else {
        // false = 사용 가능
        idChecked.value = true;
        checkedLoginId.value = member.value.loginId;
        alert("사용 가능한 아이디입니다.");
    }
    
}


const signupGo = async () => {

    if (!validateSignupFields()) return;

    member.value.memPhone = `${phoneParts.first}-${phoneParts.middle}-${phoneParts.last}`;

    if(!idChecked.value||checkedLoginId.value !== member.value.loginId){
        alert("아이디 중복확인을 해주세요")
        return;
    }

    try {
        await store.signup(member.value);

        alert("회원등록 성공");

        router.push("/login");
    } catch (e) {
        console.error(e);
        alert(e.response?.data?.message || e.response?.data?.error || "회원등록 실패");
        member.value.memDong = "";
        member.value.memHo = "";
        await loadAvailableUnits();
    }
};
</script>

<style scoped>
.signup-card {
    width: 100%;
    max-width: 560px;
    padding: 40px;
    border-radius: 16px;
    background: var(--bg-header);
    box-shadow: 0 12px 30px rgba(15, 23, 42, 0.12);
}

.signup-form {
    display: flex;
    flex-direction: column;
    gap: 18px;
}

.availability-guide {
    margin: 0 0 18px;
    padding: 12px 14px;
    border-radius: 8px;
    color: var(--text-muted);
    background: #f8fafc;
}

.availability-error {
    color: #dc2626;
    background: #fef2f2;
}

.form-row {
    display: grid;
    grid-template-columns: 1fr 1fr;
    gap: 14px;
}

.form-field select {
    width: 100%;
    height: 48px;
    padding: 0 14px;
    border: 1px solid var(--border-color);
    border-radius: 8px;
    outline: none;
    font-size: 15px;
    color: var(--text-color);
    background: var(--bg-header);
}

.form-field select:focus {
    border-color: var(--primary);
    box-shadow: 0 0 0 3px rgba(14, 165, 233, 0.15);
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

.input-action {
    display: grid;
    grid-template-columns: 1fr auto;
    gap: 8px;
}

.input-action button {
    padding: 0 16px;
    border: 1px solid var(--primary);
    border-radius: 8px;
    cursor: pointer;
    font-weight: 700;
    color: var(--primary);
    background: var(--bg-header);
}

.input-action button:hover {
    color: var(--text-white);
    background: var(--primary);
}

@media (max-width: 700px) {
    .signup-card {
        padding: 28px 24px;
    }

    .form-row {
        grid-template-columns: 1fr;
    }
}
</style>
