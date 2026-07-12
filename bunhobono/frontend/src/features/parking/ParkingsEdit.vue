<template>
  <div class="parking-edit">
    <h2>주차장 수정</h2>
    <form @submit.prevent="updateGo">
      <div class="form-group">
        <label for="parkingName">주차장 이름</label>
        <input id="parkingName" v-model="parking.parkingName" type="text" />
      </div>

      <div class="form-group">
        <label for="parkingSpaces">주차 가능 대수</label>
        <input id="parkingSpaces" v-model="parking.parkingSpaces" type="number" />
      </div>

      <div class="form-group">
        <label for="parkingLocation">위치</label>
        <input id="parkingLocation" v-model="parking.parkingLocation" type="text" />
      </div>

      <div class="form-actions">
        <button type="submit">수정하기</button>
        <button type="button" @click="router.push('/admin/parkings')">취소</button>
      </div>
    </form>
  </div>
</template>

<script setup>
import { ref } from "vue";
import { useRoute, useRouter } from "vue-router";
import { useParkingsStore } from "@/features/parking/parkingsStore";

const route = useRoute();
const router = useRouter();
const store = useParkingsStore();

const parking = ref({
  parkingNo: route.params.parkingNo,
  parkingName: "",
  parkingSpaces: 0,
  parkingLocation: "",
});

const updateGo = async () => {
  await store.update(parking.value.parkingNo, parking.value, router);
};
</script>
