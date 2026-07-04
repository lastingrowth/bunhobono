<template>
  <header>

    <template v-if="!jwtStore.token">
      <RouterLink to="/signup">회원 가입</RouterLink>&nbsp;
      <RouterLink to="/login">로그인</RouterLink>
    </template>

    <template v-else>
      <span style="margin-right: 10px">
        {{ jwtStore.userId }}님
      </span>

      <button @click="logout">로그아웃</button>
    </template>

  </header>
</template>

<script setup>
import { useJwtStore } from '../../features/login/jwtStore'
const jwtStore = useJwtStore()

function logout() {
  jwtStore.logout()
}
</script>
