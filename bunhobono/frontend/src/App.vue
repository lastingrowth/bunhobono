<template>
    <router-view/>
</template>
<script setup>
import { onMounted } from 'vue';
import { useJwtStore } from './features/login/jwtStore';

const jwtStore = useJwtStore()

onMounted(() => {
    jwtStore.loadFromStorage()
})
</script>