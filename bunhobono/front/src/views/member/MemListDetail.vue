<template>
    <h2>회원상세</h2>

    <table border="">
        <tbody>
            <tr>
                <th>가입유형</th>
                <td>{{ member.memRole }}</td>
            </tr>
            <tr>
                <th>이름</th>
                <td>{{ member.memName }}</td>
            </tr>
            <tr>
                <th>동</th>
                <td>{{ member.memDong }}</td>
            </tr>
            <tr>
                <th>호수</th>
                <td>{{ member.memHo }}</td>
            </tr>
            <tr>
                <th>연락처</th>
                <td>{{ member.memPhone }}</td>
            </tr>
            <tr>
                <th>아이디</th>
                <td>{{ member.memLoginId }}</td>
            </tr>
            <tr>
                <th>비밀번호</th>
                <td>{{ member.memLoginPwd }}</td>
            </tr>
            <tr>
                <th>가입일</th>
                <td>{{ member.memCreateAt }}</td>
            </tr>
            <tr>
                <th>탈퇴일</th>
                <td>{{ member.memDeleteAt }}</td>
            </tr>
            <tr>
                <th>상태</th>
                <td>{{ member.memStatus }}</td>
            </tr>
        </tbody>
    </table>
</template>

<script setup>
import { ref, onMounted } from "vue";
import { useRoute } from "vue-router";

const route = useRoute();

const member = ref({});

onMounted(async () => {
    const memberNo = route.params.memberNo;

    const res = await fetch(`http://localhost/api/members/detail/${memberNo}`);

    console.log("상태코드:", res.status);

    member.value = await res.json();
});
</script>