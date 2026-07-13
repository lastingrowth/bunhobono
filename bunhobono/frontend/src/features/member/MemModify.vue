<template>
    <h2>회원 수정</h2>

    <div>
        <button @click="goDetail">뒤로</button>
        <button @click="update">수정완료</button>
    </div>

    <table border="">
        <tbody>
            <tr>
                <th>가입유형</th>
                <td>
                    <!-- 관리자 회원 수정에서는 허용된 권한만 셀렉트로 선택한다. -->
                    <select v-model="member.role" @change="syncStatusWithRole">
                        <option value="RESIDENT">RESIDENT</option>
                        <option value="ADMIN">ADMIN</option>
                    </select>
                </td>
            </tr>
            <tr>
                <th>이름</th>
                <td><input type="text" v-model="member.memName" /></td>
            </tr>
            <tr>
                <th>동</th>
                <td><input type="text" v-model="member.memDong" /></td>
            </tr>
            <tr>
                <th>호수</th>
                <td><input type="text" v-model="member.memHo" /></td>
            </tr>
            <tr>
                <th>연락처</th>
                <td><input type="text" v-model="member.memPhone" /></td>
            </tr>
            <tr>
                <th>아이디</th>
                <td><input type="text" v-model="member.loginId" /></td>
            </tr>
            <tr>
                <th>비밀번호</th>
                <td>
                    <input
                        type="password"
                        v-model="member.loginPwd"
                        placeholder="변경할 경우에만 입력"
                        autocomplete="off"
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
</template>

<script setup>
import { computed, onMounted, reactive } from "vue";
import { useRoute, useRouter } from "vue-router";
import { useMemStore } from "./memStore";

const route = useRoute();
const router = useRouter();
const store = useMemStore();

const memberNo = route.params.memberNo;

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
const statusOptions = computed(() => member.role === "ADMIN"
    ? ["근무", "휴직", "퇴사"]
    : ["거주", "전출"]);

const syncStatusWithRole = () => {
    if (!statusOptions.value.includes(member.memStatus)) {
        member.memStatus = statusOptions.value[0];
    }
};

onMounted(async () => {
    await store.loadMember(memberNo);
    Object.assign(member, store.member);
    // 기존 데이터가 소문자여도 현재 가입유형 셀렉트가 자동 선택되도록 통일한다.
    member.role = String(member.role || "").toUpperCase();
    syncStatusWithRole();
    // 기존 암호화 비밀번호는 수정 화면에 노출하지 않는다.
    member.loginPwd = "";
});

// 관리자가 초기화 버튼을 누르면 저장할 새 비밀번호를 0000으로 지정한다.
const resetPassword = () => {
    member.loginPwd = "0000";
    alert("수정완료를 누르면 비밀번호가 0000으로 초기화됩니다.");
};

const goDetail = () => {
    router.push(`/admin/members/${memberNo}/detail`);
};

const update = async () => {
    console.log(member);
    await store.editMember(memberNo, member);
    alert("수정되었습니다.");
    router.push(`/admin/members/${memberNo}/detail`);
};
</script>
