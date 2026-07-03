<template>
    <h2>회원목록</h2>
    <h2>회원 목록 조회</h2>

        <div>
        <select v-model="searchType">
            <option value="all">전체출력</option>
            <option value="role">가입유형</option>
            <option value="name">이름</option>
            <option value="dongHo">동호수</option>
        </select>

<div v-if="searchType === 'dongHo'">
    <input type="text" v-model="dong" placeholder="동">
    <input type="text" v-model="ho" placeholder="호수">
</div>

<input
    v-else-if="searchType !== 'all'"
    type="text"
    v-model="searchKeyword"
    placeholder="검색어 입력"
/>

        <button @click="searchGo">검색</button>
    </div>

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
            <tr v-for="mem in memberList" :key="mem.memberNo">
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
const searchType = ref("all");
const searchKeyword = ref("");

const dong = ref("");
const ho = ref("");

const getMemberList = async () => {
    const res = await fetch("http://localhost/api/members");
    console.log("상태코드:", res.status);
    memberList.value = await res.json();
};

const searchGo = async () => {
    if (searchType.value === "all") {
        await getMemberList();
        return;
    }

    let url = "";

    if (searchType.value === "dongHo") {
        url = `http://localhost/api/members/search?type=dongHo&dong=${dong.value}&ho=${ho.value}`;
    }
    
    else {
        url = `http://localhost/api/members/search?type=${searchType.value}&keyword=${searchKeyword.value}`;
    }

    const res = await fetch(url);
    memberList.value = await res.json();
};

onMounted(() => {
    getMemberList();
});
</script>