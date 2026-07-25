<template>
  <RouterView />
  <maintenanceOverlay/>
  <AppDialog />
</template>

<script setup>
import MaintenanceOverlay from './components/MaintenanceOverlay.vue';
import AppDialog from "./shared/alert/AppDialog.vue";
</script>