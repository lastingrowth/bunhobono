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
                <td><input type="text" v-model="member.role" /></td>
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
                <td><input type="text" v-model="member.loginPwd" /></td>
            </tr>
            <tr>
                <th>상태</th>
                <td><input type="text" v-model="member.memStatus" /></td>
            </tr>
        </tbody>
    </table>
</template>

<script setup>
import { onMounted, reactive } from "vue";
import { useRouter } from "vue-router";
import { useMemStore } from "./memStore";
import { useJwtStore } from "@/features/login/jwtStore";


const router = useRouter();
const store = useMemStore();
const jwtStore = useJwtStore();

const loginId = jwtStore.userId;

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
});

const goHome = () => {
    router.push("/resident");
};

const update = async () => {
    await store.editResident(member);
    alert("수정되었습니다.");
    router.push("/resident/mypage");
};
</script>