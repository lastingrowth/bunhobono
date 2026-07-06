<template>
    <div>
        <h2>주차 요금 정책 목록</h2>
        <div>
            <table border="">
                <thead>
                    <tr>
                        <th>정책 번호</th>
                        <th>주차장 번호</th>
                        <th>차량 유형</th>
                        <th>무료 시간 (분)</th>
                        <th>단위 시간 (분)</th>
                        <th>단위 요금 (원)</th>
                        <th>일 최대 요금 (원)</th>
                        <th>사용 여부</th>
                    </tr>
                </thead>

                <tbody>
                    <tr v-for="p in policyStore.policyList" :key="p.feePolicyNo">
                        <td>{{ p.feePolicyNo }}</td>
                        <td>{{ p.parkingNo }}</td>
                        <td>{{ p.vehicleType }}</td>
                        <td>{{ p.freeMinutes }}</td>
                        <td>{{ p.unitMinutes }}</td>
                        <td>{{ p.unitFee }}</td>
                        <td>{{ p.dailyMaxFee }}</td>
                        <td>{{ p.isActive ? "활성" : "비활성" }}</td>
                    </tr>
                </tbody>
            </table>

        </div>
    </div>
</template>

<script setup>
import { onMounted } from 'vue';
import { usePolicyStore } from './policyStore';

const policyStore = usePolicyStore();

onMounted(() => {
    policyStore.loadPolicyList();
});
</script>