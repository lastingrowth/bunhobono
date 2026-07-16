<template>
    <section class="admin-signup-page">
        <div class="signup-card">
            <div class="signup-header">
                <div>
                    <p>MEMBER MANAGEMENT</p>
                    <h2>관리자 회원 추가</h2>
                </div>
                <button class="back-button" type="button" @click="router.push('/admin/members')">목록</button>
            </div>

            <form class="signup-form" @submit.prevent="signupGo">
                <div class="form-row">
                    <label class="form-field">
                        <span>가입유형</span>
                        <!-- 관리자는 입주민과 관리자 계정을 모두 등록할 수 있다. -->
                        <select v-model="member.role" @change="syncStatusWithRole">
                            <option value="RESIDENT">RESIDENT</option>
                            <option value="ADMIN">ADMIN</option>
                        </select>
                    </label>

                    <label class="form-field">
                        <span>상태</span>
                        <!-- 회원 상태도 허용된 값 안에서만 선택한다. -->
                        <select v-model="member.memStatus">
                            <option v-for="status in statusOptions" :key="status" :value="status">{{ status }}</option>
                        </select>
                    </label>
                </div>

                <label class="form-field">
                    <span>이름</span>
                    <input v-model="member.memName" type="text" minlength="2" maxlength="20" placeholder="한글+숫자 조합 2~20자" required @blur="normalizeName">
                </label>

                <div class="form-row address-row">
                    <label class="form-field">
                        <span>동</span>
                        <!-- 관리자 계정은 0동, 입주민 계정은 101~108동을 선택한다. -->
                        <select v-model.number="member.memDong" required>
                            <option disabled value="">동을 선택하세요</option>
                            <option v-for="dong in dongOptions" :key="dong" :value="dong">{{ dong }}</option>
                        </select>
                    </label>

                    <label class="form-field">
                        <span>호수</span>
                        <input type="text" inputmode="numeric" :value="member.memHo" placeholder="숫자만 입력하세요" required @input="handleHoInput">
                    </label>
                    <p class="address-guide">※ 관리자 계정은 0동 0호로 입력해주세요.</p>
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
                    <input :value="member.loginPwd" type="password" inputmode="numeric" minlength="4" maxlength="20" autocomplete="new-password" placeholder="숫자 4~20자" required @input="handlePasswordInput">
                </label>

                <button class="login-submit" type="submit">회원 추가</button>
            </form>
        </div>
    </section>
</template>

<script setup>
import { computed, reactive, ref } from "vue";
import { useRouter } from "vue-router";
import { useMemStore } from "./memStore";

const router = useRouter();
const store = useMemStore();
const idChecked = ref(false);
const checkedLoginId = ref("");
const dongOptions = [0, 101, 102, 103, 104, 105, 106, 107, 108];
const phoneParts = reactive({ first: "", middle: "", last: "" });

// =====
// 아이디는 영문과 숫자를 모두 포함하고 비밀번호는 숫자만 허용한다.
const loginIdPattern = /^(?=.*[A-Za-z])(?=.*\d)[A-Za-z\d]{3,20}$/;
const passwordPattern = /^\d{4,20}$/;
const namePattern = /^(?=.*[가-힣])(?=.*\d)[가-힣\d]{2,20}$/;

const validateSignupFields = () => {
    if (!namePattern.test(member.memName)) {
        alert("이름은 한글과 숫자를 조합해 2~20자로 입력하세요.");
        return false;
    }
    if (phoneParts.first.length !== 3 || phoneParts.middle.length !== 4 || phoneParts.last.length !== 4) {
        alert("연락처를 정확히 입력하세요.");
        return false;
    }
    if (!loginIdPattern.test(member.loginId)) {
        alert("아이디는 영문과 숫자를 조합해 3~20자로 입력하세요.");
        return false;
    }
    if (!passwordPattern.test(member.loginPwd)) {
        alert("비밀번호는 숫자 4~20자로 입력하세요.");
        return false;
    }
    return true;
};
// =====

const member = reactive({
    role: "RESIDENT",
    memName: "",
    memDong: "",
    memHo: "",
    memPhone: "",
    loginId: "",
    loginPwd: "",
    memStatus: "거주"
});

// 가입유형에 맞는 상태만 관리자 회원 등록 폼에 표시한다.
const statusOptions = computed(() => member.role === "ADMIN"
    ? ["근무", "휴직", "퇴사"]
    : ["거주", "전출"]);

const syncStatusWithRole = () => {
    member.memStatus = statusOptions.value[0];
};

// 호수 입력은 숫자만 남기고 문자 입력 시 안내한다.
const handleHoInput = (event) => {
    const inputValue = event.target.value;
    const numericValue = inputValue.replace(/\D/g, "");
    if (inputValue !== numericValue) {
        alert("숫자만 입력하세요.");
    }
    event.target.value = numericValue;
    member.memHo = numericValue === "" ? "" : Number(numericValue);
};

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
    member.memName = nameValue;
};

// 비밀번호에는 숫자만 입력한다.
const handlePasswordInput = (event) => {
    const numericValue = event.target.value.replace(/\D/g, "").slice(0, 20);
    event.target.value = numericValue;
    member.loginPwd = numericValue;
};

const idCheck = async () => {
    if (!loginIdPattern.test(member.loginId)) {
        alert("아이디는 영문과 숫자를 조합해 3~20자로 입력하세요.");
        return;
    }
    const exists = await store.idCheck(member.loginId);
    idChecked.value = !exists;
    checkedLoginId.value = exists ? "" : member.loginId;
    alert(exists ? "이미 사용 중인 아이디입니다." : "사용 가능한 아이디입니다.");
};

const signupGo = async () => {
    if (!validateSignupFields()) return;

    member.memPhone = `${phoneParts.first}-${phoneParts.middle}-${phoneParts.last}`;

    if (!idChecked.value || checkedLoginId.value !== member.loginId) {
        alert("아이디 중복 확인을 해주세요.");
        return;
    }
    if (!member.memName || !member.memPhone || !member.loginPwd) {
        alert("필수 정보를 입력해주세요.");
        return;
    }

    try {
        await store.signup({ ...member });
        alert("회원이 추가되었습니다.");
        await router.push("/admin/members");
    } catch (error) {
        console.error(error);
        alert("회원 추가에 실패했습니다.");
    }
};
</script>

<style scoped>
.admin-signup-page {
    display: flex;
    justify-content: center;
    padding: 20px 0 40px;
}

.signup-card {
    width: 100%;
    max-width: 560px;
    padding: 40px;
    border-radius: 16px;
    background: var(--bg-header);
    box-shadow: 0 12px 30px rgba(15, 23, 42, 0.12);
}

.signup-header {
    margin-bottom: 28px;
    display: flex;
    justify-content: space-between;
    align-items: center;
    gap: 16px;
}

.signup-header p {
    margin: 0 0 6px;
    font-size: 12px;
    font-weight: 700;
    letter-spacing: 2px;
    color: var(--primary);
}

.signup-header h2 {
    margin: 0;
    color: var(--heading-color);
}

.back-button {
    height: 38px;
    padding: 0 16px;
    border: 1px solid var(--border-color);
    border-radius: 8px;
    cursor: pointer;
    font-weight: 700;
    color: var(--text-color);
    background: var(--bg-header);
}

.signup-form {
    display: flex;
    flex-direction: column;
    gap: 18px;
}

.form-row {
    display: grid;
    grid-template-columns: 1fr 1fr;
    gap: 14px;
}

.address-guide {
    grid-column: 1 / -1;
    margin: -4px 2px 0;
    font-size: 12px;
    line-height: 1.4;
    color: var(--text-muted);
}

.address-row {
    row-gap: 8px;
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
