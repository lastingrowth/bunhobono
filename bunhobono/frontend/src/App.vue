<template>
    <router-view/>
</template>
<script setup>
import { onMounted } from 'vue';
import { useJwtStore } from './stores/jwtStore';

const jwtStore = useJwtStore()

onMounted(() => {
    jwtStore.loadFromStorage()
})
</script>