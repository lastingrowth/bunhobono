<template>
  <h1>로그인</h1>
  
  아이디 : <input v-model="loginId" type="text"><br/>
  비밀번호 : <input v-model="pw" type="password"><br/>
  
  <button @click="loginGo">로그인</button>
  
  <p v-if="jwtStore.errorMessage" style="color:red">{{ jwtStore.errorMessage }}</p>
</template>

<script setup>
import { useJwtStore } from '@/features/login/jwtStore'
import { ref } from 'vue'

const jwtStore = useJwtStore()
const loginId = ref('')
const pw = ref('')

async function loginGo() {
  await jwtStore.loginGo(loginId.value, pw.value)
}
</script>
