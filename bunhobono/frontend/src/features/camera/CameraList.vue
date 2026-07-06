<template>
  <div>
    <h2>카메라 목록</h2>
     <button @click="goSignup">카메라 등록</button>
    <table border="1">
      <thead>
        <tr>
          <td>Camera No</td>
          <td>Parking No</td>
          <td>Gate No</td>
          <td>Camera Name</td>
          <td>Camera Type</td>
          <td>Install Date</td>
        </tr>
      </thead>
      <tbody>
        <tr v-for="c in cStore.list" :key="c.cameraNo">
          <td>{{ c.cameraNo }}</td>
          <td>{{ c.parkingNo }}</td>
          <td>{{ c.gateNo }}</td>
          <td>{{ c.cameraName }}</td>
          <td>{{ c.cameraType }}</td>
          <td>{{ c.installDate }}</td>
          <td>
            <button @click="cStore.remove(c.cameraNo)">삭제</button>
            <button @click="goEdit(c.cameraNo)">수정</button>
          </td>
        </tr>
      </tbody>
    </table>
  </div>
</template>

<script setup>
import { onMounted } from 'vue';
import { useCameraStore } from './cameraStore';
import { useRouter } from 'vue-router';

const cStore = useCameraStore();
const router = useRouter();
onMounted(() => {
  cStore.loadList();
});

const goEdit = (cameraNo) => {
  router.push(`/cameras/${cameraNo}/edit`);
};

const goSignup = () => {
  router.push("/cameras/signup");
};


</script>
