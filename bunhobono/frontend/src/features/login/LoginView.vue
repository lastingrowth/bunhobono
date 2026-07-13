<template>
  <section class="login-card">
    <div class="login-brand">
      <div class="brand-symbol">
        P
      </div>

      <h1>아파트 주차관리 시스템</h1>
      <p>SMART PARKING SYSTEM</p>
    </div>

    <h2 class="login-title">
      로그인
    </h2>
  
    <form class="login-form"
      @submit.prevent="loginGo">
      
      <label class="form-field">
        <span>아이디</span>

        <input v-model="loginId"
          type="text"
          placeholder="아이디를 입력하세요"/>

      </label>

      <label class="form-field">
        <span>비밀번호</span>

        <input v-model="pw"
          type="password"
          placeholder="비밀번호를 입력하세요">

      </label>

      <p v-if="jwtStore.errorMessage" class="login-error">
        {{ jwtStore.errorMessage }}
      </p>

      <button class="login-submit" type="submit">
        로그인
      </button>
    </form>

    <div class="signup-guide">
      <span>아직 계정이 없으신가요?</span>

      <RouterLink
        class="signup-link"
        to="/resident/signup">
        회원가입
    </RouterLink>
    </div>

  </section>
</template>

<script setup>

import { useJwtStore } from '@/features/login/jwtStore';
import { ref } from 'vue';

const jwtStore = useJwtStore()

const loginId = ref('')
const pw = ref('')

async function loginGo() {
  await jwtStore.loginGo(loginId.value, pw.value)
}
</script>
