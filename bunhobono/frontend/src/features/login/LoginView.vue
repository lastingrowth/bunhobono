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

  <!-- 로그인 하지 않고 OCR 카메라 시연 화면으로 이동 -->
  <RouterLink class="ocr-demo-link"
              to="/ocr-upload?cameraNo=1">
    <span aria-hidden>📷</span>
    OCR 시연 카메라
  </RouterLink>

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

<style scoped>
/* 로그인 화면 오른쪽 아래에 고정되는 OCR 시연 버튼 */
.ocr-demo-link {
  position: fixed;
  right: 24px;
  bottom: 24px;
  z-index: 10;
  padding: 9px 14px;
  display: inline-flex;
  align-items: center;
  gap: 7px;
  border: 1px solid #cbd5e1;
  border-radius: 10px;
  color: #475569;
  background: rgba(255, 255, 255, 0.92);
  box-shadow: 0 6px 18px rgba(15, 23, 42, 0.08);
  font-size: 13px;
  font-weight: 700;
  text-decoration: none;
  transition: 0.2s ease;
}

.ocr-demo-link:hover {
  color: #0284c7;
  border-color: #38bdf8;
  background: #ffffff;
  transform: translateY(-2px);
  box-shadow: 0 9px 22px rgba(14, 165, 233, 0.16);
}

/* 작은 화면에서는 화면 가장자리 간격을 줄인다. */
@media (max-width: 700px) {
  .ocr-demo-link {
    right: 16px;
    bottom: 16px;
  }
}
</style>
