<template>
  <h1>로그인</h1>
  아이디 : <input v-model="loginId" type="text"><br/>
  비밀번호 : <input v-model="pw" type="password"><br/>
  <button @click="loginGo">로그인</button>
  <p v-if="jwtStore.errorMessage" style="color:red">{{ jwtStore.errorMessage }}</p>
</template>

<script setup>
import { useJwtStore } from '@/stores/jwtStore'
import { ref } from 'vue'
import { useRouter } from 'vue-router'

const jwtStore = useJwtStore()
const loginId = ref('')
const pw = ref('')
const router = useRouter()

async function loginGo() {
  await jwtStore.loginGo(loginId.value, pw.value)
  if (!jwtStore.errorMessage) {
    router.push('/')   // 로그인 성공 시 메인 페이지로 이동
  }
}
</script>
