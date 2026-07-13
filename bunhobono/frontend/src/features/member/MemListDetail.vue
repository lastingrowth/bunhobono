<template>
    <h2>회원 상세</h2>
    <div>
        <button @click="goList">목록</button>
        <button @click="goEdit">수정</button>
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
            <tr>
                <th>승인여부</th>
                <td>
                    <!-- 미승인 신규 회원은 승인 대기를 기본값으로 표시한다. -->
                    <select v-model="approvalStatus">
                        <option value="PENDING">승인 대기</option>
                        <option value="APPROVED">승인 완료</option>
                        <option value="REJECTED">승인 거절</option>
                    </select>
                    <button type="button" @click="saveApprovalStatus">승인여부 저장</button>
                </td>
            </tr>
        </tbody>
    </table>
</template>

<script setup>
import { useRoute, useRouter } from 'vue-router';
import { useMemStore } from './memStore';
import { onMounted, ref } from 'vue';

const route = useRoute();
const router = useRouter();
const store = useMemStore();
const memberNo = route.params.memberNo;
const approvalStatus = ref('PENDING');

function goList() { router.push('/admin/members'); }
function goEdit() { router.push(`/admin/members/${memberNo}/edit`); }

async function saveApprovalStatus() {
    await store.editMember(memberNo, { ...store.member, approvalStatus: approvalStatus.value });
    alert('승인여부가 저장되었습니다.');
}

async function goDelte() {
    if (!confirm('정말 삭제하시겠습니까?')) return;
    await store.removeMember(memberNo);
    alert('삭제되었습니다.');
    router.push('/admin/members');
}

onMounted(async () => {
    await store.loadMember(memberNo);
    approvalStatus.value = store.member.approvalStatus || 'PENDING';
});
</script>
