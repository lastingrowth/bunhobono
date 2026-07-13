<template>
  <div class="camera-edit">
    <h2>카메라 수정</h2>
    <form @submit.prevent="updateGo">

      <div class="form-group">
        <label for="gateNo">게이트 선택</label>
        <select id="gateNo" v-model="camera.gateNo" required>
          <option disabled value="">-- 게이트 선택 --</option>
          <option v-for="g in gStore.list" :key="g.gateNo" :value="g.gateNo">
            {{ g.gateName }} (번호: {{ g.gateNo }})
          </option>
        </select>
      </div>


      <div class="form-group">
        <label for="cameraName">카메라 이름</label>
        <input id="cameraName" v-model="camera.cameraName" type="text" required />
      </div>


      <div class="form-group">
        <label for="cameraType">카메라 타입</label>
        <select id="cameraType" v-model="camera.cameraType">
          <option value="In">In</option>
          <option value="Out">Out</option>
          <option value="Both">Both</option>
        </select>
      </div>


      <div class="form-group">
        <label for="installDate">설치일</label>
        <input id="installDate" v-model="camera.installDate" type="date" required />
      </div>


      <div class="form-actions">
        <button type="submit">수정하기</button>
        <button type="button" @click="router.push('/admin/cameras')">취소</button>
      </div>
    </form>
  </div>
</template>

<script setup>
import { ref, onMounted } from "vue";
import { useRoute, useRouter } from "vue-router";
import { useCameraStore } from "./cameraStore";
import { useGateStore } from "@/features/gates/gateStore";

const route = useRoute();
const router = useRouter();
const cStore = useCameraStore();
const gStore = useGateStore();

const camera = ref({
  cameraNo: route.params.cameraNo,
  gateNo: "",
  cameraName: "",
  cameraType: "In",
  installDate: "",
});

onMounted(async () => {
  await gStore.loadList();
  await cStore.loadList();


  const target = cStore.list.find((item) => {
    return Number(item.cameraNo) === Number(route.params.cameraNo);
  })

  camera.value = {
    cameraNo: target.cameraNo,
    gateNo: target.gateNo,
    cameraName: target.cameraName,
    cameraType: target.cameraType,
    installDate: target.installDate
      ? String(target.installDate).slice(0, 10)
      : "",
  };
      
});

const updateGo = async () => {
  await cStore.update(camera.value.cameraNo, camera.value, router);
};
</script>
