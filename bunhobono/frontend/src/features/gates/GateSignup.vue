<template>
    <div>
        <h2>게이트 등록</h2>
        <form @submit.prevent="registerGate">
            <div>
                <label>주차장 번호:</label>
                <select v-model="gate.parkingNo" required>
                    <option disabled value="">-- 주차장 선택 --</option>
                    <option v-for="p in pStore.list" :key="p.parkingNo" :value="p.parkingNo">
                        {{ p.parkingName }} (번호: {{ p.parkingNo }})
                    </option>
                </select>
            </div>

            <div>
                <label>게이트 이름:</label>
                <input v-model="gate.gateName" type="text" required />
            </div>

            <div>
                <label>게이트 타입:</label>
                <select v-model="gate.gateType">
                    <option value="In">In</option>
                    <option value="Out">Out</option>
                </select>
            </div>

            <button type="submit">등록</button>
        </form>

        <p v-if="pStore.errorMessage" style="color:red">{{ pStore.errorMessage }}</p>
        <p v-if="gStore.errorMessage" style="color:red">{{ gStore.errorMessage }}</p>
    </div>
</template>


<script setup>
import { reactive, onMounted } from "vue";
import { useRouter } from "vue-router";
import { useGateStore } from "./gateStore";
import { useParkingsStore } from "@/features/parking/parkingsStore";

const router = useRouter();
const gStore = useGateStore();
const pStore = useParkingsStore();

const gate = reactive({
    parkingNo: "",
    gateName: "",
    gateType: "In"
});

// 페이지 진입 시 주차장 목록 불러오기
onMounted(() => {
    pStore.loadParkings();
});

const registerGate = async () => {
    await gStore.gateSignUp(gate);
    if (!gStore.errorMessage) {
        router.push("/gates/list"); // 등록 성공 시 목록 화면으로 이동
    }
};
</script>