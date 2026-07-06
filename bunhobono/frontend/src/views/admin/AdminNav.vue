<template>
  <div class="admin-nav">
    관리자 nav
    <router-link to="/admin/carlog">CarlogView</router-link>
    <router-link to="/vehicle">VehicleView</router-link>
    <router-link to="/gates/list">게이트목록보기</router-link>
    <router-link to="/camera/list">카메라정보보기</router-link>
    <router-link to="/cameraData/list">카메라데이터정보조회</router-link>
    <router-link to="/parkings/list2">주차장정보보기</router-link>
    <router-link to="/admin/notice">알림목록</router-link>
  </div>
</template>

<style scoped>
.admin-nav a {
  display: block;
  margin-bottom: 8px;
}
</style>
