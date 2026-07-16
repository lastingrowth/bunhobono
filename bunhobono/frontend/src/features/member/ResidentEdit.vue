<template>
    <h2>회원 정보 수정</h2>

    <div>
        <button @click="goHome">홈</button>
        <button @click="update">수정완료</button>
    </div>

    <table border="">
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
                    <!-- 버튼을 누른 경우에만 새 비밀번호 입력란을 노출한다. -->
                    <button type="button" @click="togglePasswordChange">
                        {{ showPasswordField ? "변경 취소" : "비밀번호 변경" }}
                    </button>
                    <input
                        v-if="showPasswordField"
                        type="password"
                        :value="member.loginPwd"
                        inputmode="numeric"
                        minlength="4"
                        maxlength="20"
                        autocomplete="off"
                        placeholder="숫자 4~20자"
                        @input="handlePasswordInput"
                    />
                </td>
            </tr>
            <tr>
                <th>상태</th>
                <td>{{ member.memStatus || "-" }}</td>
            </tr>
        </tbody>
    </table>
</template>

<script setup>
import { onMounted, reactive, ref } from "vue";
import { useRouter } from "vue-router";
import { useMemStore } from "./memStore";
import { useJwtStore } from "@/features/login/jwtStore";


const router = useRouter();
const store = useMemStore();
const jwtStore = useJwtStore();

const loginId = jwtStore.userId;
const showPasswordField = ref(false);
const phoneParts = reactive({ first: "", middle: "", last: "" });

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

// 새 비밀번호에는 숫자만 입력한다.
const handlePasswordInput = (event) => {
    const numericValue = event.target.value.replace(/\D/g, "").slice(0, 20);
    event.target.value = numericValue;
    member.loginPwd = numericValue;
};

const goHome = () => {
    router.push("/resident");
};

const togglePasswordChange = () => {
    // 변경 취소 시 입력 중인 새 비밀번호가 서버로 전달되지 않도록 초기화한다.
    showPasswordField.value = !showPasswordField.value;
    if (!showPasswordField.value) {
        member.loginPwd = "";
    }
};

const update = async () => {
    if (phoneParts.first.length !== 3 || phoneParts.middle.length !== 4 || phoneParts.last.length !== 4) {
        alert("연락처를 정확히 입력하세요.");
        return;
    }
    member.memPhone = `${phoneParts.first}-${phoneParts.middle}-${phoneParts.last}`;

    if (showPasswordField.value && !/^\d{4,20}$/.test(member.loginPwd)) {
        alert("비밀번호는 숫자 4~20자로 입력하세요.");
        return;
    }

    const passwordChanged = showPasswordField.value;
    
    // 입주민 본인은 연락처와 새 비밀번호만 수정 요청으로 전달한다.
    await store.editResident({
        memPhone: member.memPhone,
        loginPwd: passwordChanged ? member.loginPwd : null
    });

    if(passwordChanged){
        alert("비밀번호가 변경되었습니다. 다시 로그인해주세요.");
        jwtStore.logout();
        return;
    }
    alert("수정되었습니다.");
    router.push("/resident/mypage");
};
</script>

<style scoped>
.phone-fields {
    display: flex;
    align-items: center;
    gap: 6px;
}

.phone-fields input {
    width: 72px;
    text-align: center;
}
</style>
