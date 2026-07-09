<template>
  <div class="resident-nav">
    입주민 nav
    <router-link to="/">home</router-link>
    <router-link to="/parkings/list">주차장정보보기</router-link>
    <router-link to="/res/vehicles">내 차량 관리</router-link>
    <router-link to="/resident/mypage">마이페이지</router-link>
  </div>
</template>

<style scoped>
.resident-nav a {
  display: block;
  margin-bottom: 8px;
}
</style>
