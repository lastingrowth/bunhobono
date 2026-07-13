<template>
    <h2>회원 수정</h2>

    <div>
        <button @click="goList">목록</button>
        <button @click="goDetail">상세</button>
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
                <td><input type="password" v-model="member.loginPwd" /></td>
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

onMounted(async () => {
    await store.loadMember(memberNo);
    Object.assign(member, store.member);
});

const goList = () => {
    router.push("/admin/members");
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