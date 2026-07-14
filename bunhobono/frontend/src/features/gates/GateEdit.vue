<template>
  <div class="gate-edit">
    <h2>게이트 수정</h2>

    <form @submit.prevent="updateGo">
      <div class="form-group">
        <label for="parkingNo">주차장 선택</label>
        <select id="parkingNo" v-model="gate.parkingNo" required>
          <option disabled value="">-- 주차장 선택 --</option>
          <option
            v-for="p in pStore.list"
            :key="p.parkingNo"
            :value="p.parkingNo"
          >
            {{ p.parkingName }} (번호: {{ p.parkingNo }})
          </option>
        </select>
      </div>

      <div class="form-group">
        <label for="gateName">게이트 이름</label>
        <input id="gateName" v-model="gate.gateName" type="text" required />
      </div>

      <div class="form-group">
        <label for="gateType">게이트 타입</label>
        <select id="gateType" v-model="gate.gateType">
          <option value="In">In</option>
          <option value="Out">Out</option>
          <option value="Both">Both</option>
        </select>
      </div>

      <div class="form-actions">
        <button type="submit">수정하기</button>
        <button type="button" @click="router.push('/admin/gates')">취소</button>
      </div>
    </form>
  </div>
</template>

<script setup>
import { ref, onMounted } from "vue";
import { useRoute, useRouter } from "vue-router";
import { useGateStore } from "@/features/gates/gateStore";
import { useParkingsStore } from "@/features/parking/parkingsStore";

const route = useRoute();
const router = useRouter();
const gStore = useGateStore();
const pStore = useParkingsStore();

const gate = ref({
  gateNo: route.params.gateNo,
  gateName: "",
  gateType: "In",
  parkingNo: "",
});

onMounted(async () => {
  await pStore.loadList();
  await gStore.loadList();

  const selectedGate = gStore.list.find(
    item => Number(item.gateNo) === Number(route.params.gateNo)
  );

  if (!selectedGate) {
    alert("게이트 정보를 찾을 수 없습니다.");
    router.push("/admin/gates");
    return;
  }

  gate.value = {
    gateNo: selectedGate.gateNo,
    gateName: selectedGate.gateName,
    gateType: selectedGate.gateType,
    parkingNo: selectedGate.parkingNo,
  };
});

const updateGo = async () => {
  await gStore.update(gate.value.gateNo, gate.value, router);
};
</script>
