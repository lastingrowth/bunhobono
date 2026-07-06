<template>
    <h2>회원상세</h2>
    
    
    <div>
        <button @click="goList">목록</button>
        <button @click="goModify">수정</button>
        <button @click="goDelete">삭제</button>
    </div>

    <table border="">
        <tbody>
            <tr>
                <th>가입유형</th>
                <td>{{ store.member.role }}</td>
            </tr>
            <tr>
                <th>이름</th>
                <td>{{ store.member.memName }}</td>
            </tr>
            <tr>
                <th>동</th>
                <td>{{ store.member.memDong }}</td>
            </tr>
            <tr>
                <th>호수</th>
                <td>{{ store.member.memHo }}</td>
            </tr>
            <tr>
                <th>연락처</th>
                <td>{{ store.member.memPhone }}</td>
            </tr>
            <tr>
                <th>아이디</th>
                <td>{{ store.member.loginId }}</td>
            </tr>
            <tr>
                <th>비밀번호</th>
                <td>{{ store.member.loginPwd }}</td>
            </tr>
            <tr>
                <th>가입일</th>
                <td>{{ store.member.memCreateAt }}</td>
            </tr>
            <tr>
                <th>탈퇴일</th>
                <td>{{ store.member.memDeleteAt }}</td>
            </tr>
            <tr>
                <th>상태</th>
                <td>{{ store.member.memStatus }}</td>
            </tr>
        </tbody>
    </table>
</template>

<script setup>
import { useRoute, useRouter } from 'vue-router';
import { useMemStore } from './memStore';
import { onMounted } from 'vue';

const router = useRouter();
const route = useRoute();
const store = useMemStore();

const memberNo = route.params.memberNo;

onMounted(() => {
    store.loadMember(memberNo);
});

const goList = () => {
    router.push("/admin/members");
};

const goModify = () => {
    router.push(`/admin/members/${memberNo}/modify`);
};

const goDelete = async () => {
    const check = confirm("정말 삭제하시겠습니까?");

    if (!check) {
        return;
    }

    await store.remove(memberNo);
    alert("삭제되었습니다.");
    router.push("/admin/members");
};
</script>