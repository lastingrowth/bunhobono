<template>
    <h2>마이페이지</h2>

    <div>
        <button @click="goHome">홈</button>
        <button @click="goModify">회원정보수정</button>
        <button @click="goDelete">회원탈퇴</button>
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
import { onMounted } from "vue";
import { useRouter } from "vue-router";
import { useMemStore } from "./memStore";
import { useJwtStore } from "../login/jwtStore";


const router = useRouter();
const store = useMemStore();
const jwtStore = useJwtStore();

// 로그인한 사용자 아이디
const loginId = jwtStore.userId;

onMounted(async () => {
    await store.loadMypage(loginId);
});

// 홈
const goHome = () => {
    router.push("/resident");
};

// 수정
const goModify = () => {
    router.push(`/resident/mypage/edit`);
};

// 삭제
const goDelete = async () => {

    const check = confirm("정말 탈퇴하시겠습니까?");

    if (!check) return;

    await store.removeResident(loginId);

    // 로그인 정보 삭제
    localStorage.removeItem("token");
    sessionStorage.clear();

    // Pinia 로그인 상태 초기화
    jwtStore.token = null;
    jwtStore.userId = null;
    jwtStore.role = null;
    jwtStore.memStatus = null;

    alert("탈퇴되었습니다.");

    // 뒤로가기 방지를 위해 replace 사용
    router.replace("/login");
};
</script>