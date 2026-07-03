<template>
    <h2>회원목록</h2>
    <table border="">
        <thead>
            <tr>
                <th>가입유형</th>
                <th>이름</th>
                <th>동</th>
                <th>호수</th>
                <th>연락처</th>
                <th>아이디</th>
                <th>비밀번호</th>
                <th>가입일</th>
                <th>탈퇴일</th>
                <th>상태</th>
            </tr>
        </thead>

        <tbody>
            <tr v-for="mem in memberList" :key="mem.memNo">
                <td>{{ mem.memRole }}</td>
                <td>
                    <router-link :to="`/members/detail/${mem.memberNo}`">
                        {{ mem.memName }}
                    </router-link>
                </td>
                <td>{{ mem.memDong }}</td>
                <td>{{ mem.memHo }}</td>
                <td>{{ mem.memPhone }}</td>
                <td>{{ mem.memLoginId }}</td>
                <td>{{ mem.memLoginPwd }}</td>
                <td>{{ mem.memCreateAt }}</td>
                <td>{{ mem.memDeleteAt }}</td>
                <td>{{ mem.memStatus }}</td>
            </tr>
        </tbody>
    </table>
</template>

<script setup>
import { ref, onMounted } from "vue";

const memberList = ref([]);

onMounted(async () => {
    const res = await fetch("http://localhost/api/members");
    console.log("상태코드:", res.status);
    memberList.value = await res.json();
});
</script>