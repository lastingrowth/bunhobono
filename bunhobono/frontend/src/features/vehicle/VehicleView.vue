<template>
    <h2>Vehicle 화면</h2>

    <div v-for="v in vehicleStore.vehicleList" :key="v.vehicleCarNo">
        <div>{{ v.vehicleCarNo }}</div>
        <div>{{ v.carNo }}</div>
        <div>{{ v.vehicleType }}</div>
        <div>{{ v.memberNo }}</div>
        <div>{{ v.regDate }}</div>

    </div>
</template>

<script setup>
import { onMounted } from 'vue';
import { useVehicleStore } from './vehicleStore';

const vehicleStore = useVehicleStore()

onMounted(() => {
    vehicleStore.loadVehicleList()
})
</script>