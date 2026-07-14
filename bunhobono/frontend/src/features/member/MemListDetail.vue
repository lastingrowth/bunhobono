<template>
    <h2>회원 상세</h2>
    <div>
        <button @click="goList">목록</button>
        <button @click="goEdit">수정</button>
        <button v-if="store.member.role === 'PENDING'" type="button" @click="approveMember">승인</button>
        <button @click="goDelte">삭제</button>
    </div>
    <table border="">
        <tbody>
            <tr><th>가입유형</th><td>{{ store.member.role }}</td></tr>
            <tr><th>이름</th><td>{{ store.member.memName }}</td></tr>
            <tr><th>동</th><td>{{ store.member.memDong }}</td></tr>
            <tr><th>호수</th><td>{{ store.member.memHo }}</td></tr>
            <tr><th>연락처</th><td>{{ store.member.memPhone }}</td></tr>
            <tr><th>아이디</th><td>{{ store.member.loginId }}</td></tr>
            <tr><th>가입일</th><td>{{ store.member.memCreateAt }}</td></tr>
            <tr><th>탈퇴일</th><td>{{ store.member.memDeleteAt }}</td></tr>
            <tr><th>상태</th><td>{{ store.member.memStatus }}</td></tr>
        </tbody>
    </table>
</template>

<script setup>
import { useRoute, useRouter } from 'vue-router';
import { useMemStore } from './memStore';
import { onMounted } from 'vue';

const route = useRoute();
const router = useRouter();
const store = useMemStore();
const memberNo = route.params.memberNo;

function goList() { router.push('/admin/members'); }
function goEdit() { router.push(`/admin/members/${memberNo}/edit`); }

async function approveMember() {
    await store.approveMembers([Number(memberNo)]);
    await store.loadMember(memberNo);
    alert('입주민으로 승인되었습니다.');
}

async function goDelte() {
    if (!confirm('정말 삭제하시겠습니까?')) return;
    await store.removeMember(memberNo);
    alert('삭제되었습니다.');
    router.push('/admin/members');
}

onMounted(async () => {
    await store.loadMember(memberNo);
});
</script>
