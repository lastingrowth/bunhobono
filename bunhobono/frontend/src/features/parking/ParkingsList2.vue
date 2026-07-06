<template>
  <div>
    <h2>주차장 목록(관리자용)</h2>
    <table border="1">
      <thead>
        <tr>
          <th>Parking No</th>
          <th>display No</th>
          <th>Parking Name</th>
          <th>Parking Spaces</th>
          <th>Parking Location</th>
          <th>주차중</th>
        </tr>
      </thead>
      <tbody>
        <tr v-for="p in pStore.list" :key="p.parkingNo">
          <td>{{ p.parkingNo }}</td>
          <td>{{ p.displayNo }}</td>
          <td>{{ p.parkingName }}</td>
          <td>{{ p.parkingSpaces }}</td>
          <td>{{ p.parkingLocation }}</td>
          <td>
            {{ p.parkingSpaces - p.availableSpaces }}/{{ p.parkingSpaces }}
          </td>
          <td>
            <button @click="goEdit(p.parkingNo)">수정</button>
          </td>
          <td>
            <button @click="pStore.remove(p.parkingNo)">삭제</button>
          </td>
        </tr>
      </tbody>
    </table>
    <button @click="goSignUp">주차장 등록</button>
  </div>
</template>

<script setup>
import { onMounted } from 'vue';
import { useParkingsStore } from './parkingsStore';
import { useRouter } from 'vue-router';

const pStore = useParkingsStore();
const router = useRouter();

onMounted(() => {
  pStore.loadList();
});

const goSignUp = () => {
  router.push("/parkings/signUp"); // 등록 컴포넌트 라우트로 이동
};

const goEdit = (parkingNo) => {
  router.push(`/parkings/${parkingNo}/edit`);
};
</script>
