<template>
    <h2>회원 상세</h2>
    <div>
        <button @click="goList">목록</button>
        <button v-if="canEditMember" @click="goEdit">수정</button>
        <button v-if="store.member.role === 'PENDING'" type="button" @click="approveMember">승인</button>
        <button
            v-if="isWithdrawnMember"
            type="button"
            @click="permanentlyDeleteMember"
        >
            영구삭제
        </button>
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
import { computed, onMounted } from 'vue';

const route = useRoute();
const router = useRouter();
const store = useMemStore();
const memberNo = route.params.memberNo;

const canEditMember = computed(() =>
    String(store.member.role || '').toUpperCase() !== 'PENDING'
    && !store.member.memDeleteAt
);

const isWithdrawnMember = computed(() => Boolean(store.member.memDeleteAt));

function goList() { router.push('/admin/members'); }
function goEdit() { router.push(`/admin/members/${memberNo}/edit`); }

async function approveMember() {
    const memberName = store.member.memName;
    await store.approveMembers([Number(memberNo)]);
    await store.loadMember(memberNo);
    alert(`${memberName}님이 승인되셨습니다.`);
    router.push('/admin/members');
}

async function permanentlyDeleteMember() {
    if (!confirm('이 회원을 영구 삭제하시겠습니까? 이 작업은 되돌릴 수 없습니다.')) return;

    const deletedCount = await store.removeWithdrawnMembers([Number(memberNo)]);
    if (deletedCount === 0) {
        alert('영구삭제 조건을 충족하지 않아 삭제되지 않았습니다.');
        return;
    }
    alert('회원이 영구 삭제되었습니다.');
    router.push('/admin/members');
}

onMounted(async () => {
    await store.loadMember(memberNo);
});
</script>
