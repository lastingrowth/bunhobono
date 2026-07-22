<template>
  <Transition name="trash-toast">
    <div v-if="message" class="trash-feedback-toast" :class="type" role="status">
      {{ message }}
    </div>
  </Transition>
</template>

<script setup>
defineProps({
  message: { type: String, default: "" },
  type: { type: String, default: "success" },
});
</script>

<style scoped>
.trash-feedback-toast { position: fixed; z-index: 1200; top: 24px; right: 24px; padding: 11px 16px; border: 1px solid #9fcfb0; border-radius: 8px; color: #1f6840; background: #ecf8f0; box-shadow: 0 8px 24px rgba(23,45,34,.18); font-size: 13px; font-weight: 800; }
.trash-feedback-toast.error { border-color: #e3adad; color: #9f2f2f; background: #fff0f0; }
.trash-toast-enter-active,.trash-toast-leave-active { transition: opacity .18s ease, transform .18s ease; }
.trash-toast-enter-from,.trash-toast-leave-to { opacity: 0; transform: translateY(-8px); }
</style>
