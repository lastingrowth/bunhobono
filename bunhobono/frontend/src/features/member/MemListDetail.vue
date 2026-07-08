<template>
    <h2>회원상세</h2>

    <table border="">
        <tbody>
            <tr>
                <th>가입유형</th>
                <td>{{ store.member.memRole }}</td>
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
                <td>{{ store.member.memLoginId }}</td>
            </tr>
            <tr>
                <th>비밀번호</th>
                <td>{{ store.member.memLoginPwd }}</td>
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
import { useRoute } from 'vue-router';
import { useMemStore } from './memStore';
import { onMounted } from 'vue';

const route = useRoute();
const store = useMemStore();

onMounted(() => {
    store.loadMember(route.params.memberNo);
});

</script>