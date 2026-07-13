<template>
    <section>
        <h2>관리자 회원 추가</h2>

        <div>
            <button type="button" @click="router.push('/admin/members')">목록</button>
            <button type="button" @click="signupGo">등록</button>
        </div>

        <table border="">
            <tbody>
                <tr>
                    <th>가입유형</th>
                    <td>
                        <!-- 관리자는 입주민과 관리자 계정을 모두 등록할 수 있다. -->
                        <select v-model="member.role" @change="syncStatusWithRole">
                            <option value="RESIDENT">RESIDENT</option>
                            <option value="ADMIN">ADMIN</option>
                        </select>
                    </td>
                </tr>
                <tr><th>이름</th><td><input v-model="member.memName" type="text" required></td></tr>
                <tr>
                    <th>동</th>
                    <td>
                        <!-- 관리자 회원 추가에서도 101~108동만 선택할 수 있다. -->
                        <select v-model.number="member.memDong">
                            <option disabled value="">동을 선택하세요</option>
                            <option v-for="dong in dongOptions" :key="dong" :value="dong">{{ dong }}</option>
                        </select>
                    </td>
                </tr>
                <tr>
                    <th>호수</th>
                    <td>
                        <input
                            type="text"
                            inputmode="numeric"
                            :value="member.memHo"
                            placeholder="숫자만 입력하세요"
                            @input="handleHoInput"
                        >
                    </td>
                </tr>
                <tr><th>연락처</th><td><input v-model="member.memPhone" type="text" required></td></tr>
                <tr>
                    <th>아이디</th>
                    <td>
                        <input v-model="member.loginId" type="text" required>
                        <button type="button" @click="idCheck">아이디 중복 확인</button>
                    </td>
                </tr>
                <tr><th>비밀번호</th><td><input v-model="member.loginPwd" type="password" autocomplete="off" required></td></tr>
                <tr>
                    <th>상태</th>
                    <td>
                        <!-- 회원 상태도 허용된 값 안에서만 선택한다. -->
                        <select v-model="member.memStatus">
                            <option v-for="status in statusOptions" :key="status" :value="status">{{ status }}</option>
                        </select>
                    </td>
                </tr>
            </tbody>
        </table>
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
const dongOptions = [101, 102, 103, 104, 105, 106, 107, 108];

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

const idCheck = async () => {
    if (!member.loginId) {
        alert("아이디를 입력하세요.");
        return;
    }
    const exists = await store.idCheck(member.loginId);
    idChecked.value = !exists;
    checkedLoginId.value = exists ? "" : member.loginId;
    alert(exists ? "이미 사용 중인 아이디입니다." : "사용 가능한 아이디입니다.");
};

const signupGo = async () => {
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
