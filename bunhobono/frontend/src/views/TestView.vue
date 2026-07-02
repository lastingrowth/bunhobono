<template>
    <div>
        <h1>vue + spring 연결 테스트</h1>
        <p>{{ msg }}</p>
    </div>
</template>

<script setup>
import { onMounted, ref } from 'vue';

const msg = ref("")

onMounted(async () => {
    try {
        const res = await fetch("/api/test");
        const data = await res.text();
        msg.value = data;
    } catch (e) {
        msg.value = "연결 실패";
    }
});
</script>