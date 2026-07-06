<template>
  <h2>주차장 등록</h2>

  <table border="1">
    <tbody>
      <tr>
        <th>주차장 이름</th>
        <td><input type="text" v-model="parking.parkingName" /></td>
      </tr>
      <tr>
        <th>주차 공간 수</th>
        <td><input type="number" v-model="parking.parkingSpaces" /></td>
      </tr>
      <tr>
        <th>주차장 위치</th>
        <td><input type="text" v-model="parking.parkingLocation" /></td>
      </tr>
      <tr>
        <td colspan="2">
          <button @click="signupGo">등록</button>
        </td>
      </tr>
    </tbody>
  </table>
</template>

<script setup>
import { ref } from "vue";
import { useRouter } from "vue-router";
import { useParkingsStore } from "./parkingsStore";

const router = useRouter();
const store = useParkingsStore();

const parking = ref({
  parkingName: "",
  parkingSpaces: "",
  parkingLocation: "",
});

const signupGo = async () => {
  try {
    await store.signup(parking.value);
    alert("주차장 등록 성공");
    router.push("/parkings/list2");
  } catch (e) {
    console.error(e);
    alert("주차장 등록 실패");
  }
};
</script>
