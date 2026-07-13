<template>
  <div class="camera-signup">
    <h2>카메라 등록</h2>
    <form @submit.prevent="signupGo">

      <div class="form-group">
        <label for="parkingNo">주차장 선택</label>
        <select id="parkingNo" v-model="camera.parkingNo" required>
          <option disabled value="">-- 주차장 선택 --</option>
          <option v-for="p in pStore.list" :key="p.parkingNo" :value="p.parkingNo">
            {{ p.parkingName }} (번호: {{ p.parkingNo }})
          </option>
        </select>
      </div>


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
        </select>
      </div>


      <div class="form-group">
        <label for="installDate">설치일</label>
        <input id="installDate" v-model="camera.installDate" type="date" required />
      </div>

      <button type="submit">등록하기</button>
    </form>
  </div>
</template>

<script setup>
import { ref, onMounted } from "vue";
import { useRouter } from "vue-router";
import { useCameraStore } from "./cameraStore";
import { useParkingsStore } from "@/features/parking/parkingsStore";
import { useGateStore } from "@/features/gates/gateStore";

const router = useRouter();
const cStore = useCameraStore();
const pStore = useParkingsStore();
const gStore = useGateStore();

const camera = ref({
  parkingNo: "",
  gateNo: "",
  cameraName: "",
  cameraType: "In",
  installDate: "",
});

onMounted(() => {
  pStore.loadParkings();
  gStore.loadGates();
});

const signupGo = async () => {
  await cStore.signup(camera.value, router);
};

</script>
