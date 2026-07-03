<template>
  <header>
    header쪽 뷰
    <router-link to="/main">회원가입</router-link>&nbsp;
    <RouterLink to="/members">회원 목록 조회</RouterLink>
    <RouterLink to="/members/signup" >회원 등록</RouterLink>

    <router-link v-if="!jwtStore.token" to="/login">로그인</router-link>
    <button v-else @click="jwtStore.logout()">로그아웃</button>
    <router-link v-if="jwtStore.role === 'office'" to="/admin">관리자 페이지</router-link>
    <p v-if="jwtStore.userId">로그인 아이디: {{ jwtStore.userId }}</p>
    <p v-if="jwtStore.role">권한: {{ jwtStore.role }}</p>
    <p v-if="jwtStore.memStatus">상태: {{ jwtStore.memStatus }}</p>
  </header>
</template>

<script setup>
import { useJwtStore } from '../stores/jwtStore'
const jwtStore = useJwtStore()
</script>
