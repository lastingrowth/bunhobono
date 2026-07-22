<template>
  <footer class="resident-footer">
    <div class="resident-footer-inner">
      <div class="resident-footer-brand">
        <strong>아파트 주차관리 시스템</strong>
      </br>
        <span>입주민의 편리하고 안전한 주차 생활을 지원합니다. <br/> 입주민 문의 : 051-777-7777</span>
      

      </div>


      <small>© {{ currentYear }} Smart Parking System. All rights reserved.</small>
    </div>
  </footer>
</template>

<script setup>
const currentYear = new Date().getFullYear()
</script>

<style scoped>
.resident-footer {
  flex-shrink: 0;
  width: 100%;
  border-top: 1px solid #d8e3ec;
  color: #61778a;
  background: rgba(248, 251, 253, .97);
}

.resident-footer-inner {
  width: min(760px, calc(100% - 200px));
  margin: 0 auto;
  padding: 42px 0 38px;
  text-align: center;
}

.resident-footer-brand {
  display: grid;
  gap: 5px;
}

.resident-footer-brand strong {
  color: #294761;
  font-size: 15px;
}

.resident-footer-brand span {
  font-size: 12px;
}

.resident-footer-links {
  display: flex;
  justify-content: center;
  gap: 22px;
  margin: 16px 0;
}

.resident-footer-links a {
  color: #49677f;
  font-size: 12px;
  font-weight: 700;
  text-decoration: none;
}

.resident-footer-links a:hover {
  color: #287fd5;
  text-decoration: underline;
  text-underline-offset: 4px;
}

.resident-footer small {
  color: #8a9ba9;
  font-size: 10px;
}

@media (max-width: 900px) {
  .resident-footer-inner { width: calc(100% - 36px); }
}

@media (max-width: 600px) {
  .resident-footer-inner { width: calc(100% - 24px); padding: 34px 0 30px; }
  .resident-footer-links { flex-wrap: wrap; gap: 12px 18px; }
}
</style>
