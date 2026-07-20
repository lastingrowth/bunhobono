<template>
    <h2>회원 수정</h2>

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
                    <input
                        type="password"
                        :value="member.loginPwd"
                        inputmode="numeric"
                        minlength="4"
                        maxlength="20"
                        placeholder="숫자 4~20자"
                        autocomplete="off"
                        @input="handlePasswordInput"
                    />
                    <button type="button" @click="resetPassword">비밀번호 초기화</button>
                </td>
            </tr>
            <tr>
                <th>상태</th>
                <td>
                    <select v-model="member.memStatus">
                        <option v-for="status in statusOptions" :key="status" :value="status">{{ status }}</option>
                    </select>
                </td>
            </tr>
        </tbody>
    </table>

    <div class="form-actions">
        <button type="button" @click="goDetail">뒤로</button>
        <button type="button" @click="update">수정완료</button>
    </div>
</template>

<script setup>
import { computed, onMounted, reactive } from "vue";
import { useRoute, useRouter } from "vue-router";
import { useMemStore } from "./memStore";
import { useJwtStore } from "@/features/login/jwtStore";

const route = useRoute();
const router = useRouter();
const store = useMemStore();
const jwtStore = useJwtStore();

const memberNo = route.params.memberNo;
const phoneParts = reactive({ first: "", middle: "", last: "" });

const member = reactive({
    role: "",
    memName: "",
    memDong: "",
    memHo: "",
    memPhone: "",
    loginId: "",
    loginPwd: "",
    memStatus: "",
});

// RESIDENT는 거주·전출, ADMIN은 근무·휴직·퇴사 상태만 선택한다.
const isCurrentAdmin = computed(() =>
    member.role === "ADMIN" && member.loginId === jwtStore.userId
);

const statusOptions = computed(() => {
    if (member.role !== "ADMIN") return ["거주", "전출"];
    return isCurrentAdmin.value ? ["근무", "휴직"] : ["근무", "휴직", "퇴사"];
});

onMounted(async () => {
    await store.loadMember(memberNo);
    Object.assign(member, store.member);
    // 상태 선택지를 구분할 수 있도록 가입유형 형식을 통일한다.
    member.role = String(member.role || "").toUpperCase();
    // 기존 암호화 비밀번호는 수정 화면에 노출하지 않는다.
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

// 관리자가 초기화 버튼을 누르면 저장할 새 비밀번호를 0000으로 지정한다.
const resetPassword = () => {
    member.loginPwd = "0000";
    alert("수정완료를 누르면 비밀번호가 0000으로 초기화됩니다.");
};

const goDetail = () => {
    router.push(`/admin/members/${memberNo}/detail`);
};

const update = async () => {
    if (phoneParts.first.length !== 3 || phoneParts.middle.length !== 4 || phoneParts.last.length !== 4) {
        alert("연락처를 정확히 입력하세요.");
        return;
    }
    member.memPhone = `${phoneParts.first}-${phoneParts.middle}-${phoneParts.last}`;

    if (member.loginPwd && !/^\d{4,20}$/.test(member.loginPwd)) {
        alert("비밀번호는 숫자 4~20자로 입력하세요.");
        return;
    }

    // 관리자는 연락처, 새 비밀번호, 회원 상태만 수정 요청으로 전달한다.
    await store.editMember(memberNo, {
        memPhone: member.memPhone,
        loginPwd: member.loginPwd,
        memStatus: member.memStatus
    });
    alert("수정되었습니다.");
    if (["전출", "퇴사"].includes(member.memStatus)) {
        router.push({ path: "/admin/members", query: { section: "withdrawn" } });
        return;
    }
    router.push(`/admin/members/${memberNo}/detail`);
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

.form-actions {
    display: flex;
    justify-content: center;
    gap: 12px;
    margin-top: 16px;
}

.form-actions button {
    min-width: 120px;
    height: 44px;
    padding: 0 20px;
    font-size: 16px;
    font-weight: 700;
}
</style>
