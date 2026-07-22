<template>
  <Teleport to="body">
    <Transition name="delete-modal">
      <div v-if="open" class="delete-modal-backdrop" @click.self="$emit('cancel')">
        <section class="delete-modal" role="dialog" aria-modal="true" aria-labelledby="carlog-delete-title">
          <div class="delete-modal-icon" aria-hidden="true">!</div>
          <h2 id="carlog-delete-title">입출차 기록 삭제</h2>
          <p>
            <strong>{{ carNo || "미인식 차량" }}</strong> 차량의 입출차 기록을<br>
            삭제하시겠습니까?
          </p>
          <small>삭제된 기록은 휴지통에서 복원할 수 있습니다.</small>

          <div class="delete-modal-actions">
            <button type="button" class="cancel" :disabled="deleting" @click="$emit('cancel')">취소</button>
            <button type="button" class="confirm" :disabled="deleting" @click="$emit('confirm')">
              {{ deleting ? "삭제 중" : "삭제" }}
            </button>
          </div>
        </section>
      </div>
    </Transition>
  </Teleport>
</template>

<script setup>
defineProps({
  open: { type: Boolean, default: false },
  carNo: { type: String, default: "" },
  deleting: { type: Boolean, default: false },
});

defineEmits(["cancel", "confirm"]);
</script>

<style scoped>
.delete-modal-backdrop { position: fixed; z-index: 3000; inset: 0; display: grid; place-items: center; padding: 20px; background: rgba(10,12,14,.7); backdrop-filter: blur(2px); }
.delete-modal { box-sizing: border-box; width: min(390px,calc(100vw - 32px)); padding: 25px 25px 21px; border: 1px solid #626a70; border-radius: 8px; color: #e4e7e9; text-align: center; background: #30363b; box-shadow: 0 24px 70px rgba(0,0,0,.52); }
.delete-modal-icon { width: 42px; height: 42px; margin: 0 auto 12px; display: grid; place-items: center; border: 1px solid #747d83; border-radius: 50%; color: #e6e9eb; background: #444b50; font-size: 24px; font-weight: 900; }
.delete-modal h2 { margin: 0; color: #f0f2f3; font-size: 20px; }
.delete-modal p { margin: 14px 0 7px; color: #c6ccd0; font-size: 14px; line-height: 1.65; }
.delete-modal p strong { color: #fff; font-size: 16px; }
.delete-modal small { color: #9da5aa; font-size: 11px; }
.delete-modal-actions { margin-top: 21px; display: grid; grid-template-columns: 1fr 1fr; gap: 8px; }
.delete-modal-actions button { height: 38px; border-radius: 7px; cursor: pointer; font-size: 13px; font-weight: 800; }
.delete-modal-actions .cancel { border: 1px solid #697178; color: #d6dade; background: #3c4348; }
.delete-modal-actions .confirm { border: 1px solid #888f94; color: #fff; background: #555d63; }
.delete-modal-actions .cancel:hover { border-color: #858d93; background: #474f54; }
.delete-modal-actions .confirm:hover { border-color: #a1a7ab; background: #656d73; }
.delete-modal-actions button:disabled { cursor: wait; opacity: .6; }
.delete-modal-enter-active,.delete-modal-leave-active { transition: opacity .16s ease; }
.delete-modal-enter-active .delete-modal,.delete-modal-leave-active .delete-modal { transition: transform .16s ease; }
.delete-modal-enter-from,.delete-modal-leave-to { opacity: 0; }
.delete-modal-enter-from .delete-modal,.delete-modal-leave-to .delete-modal { transform: translateY(8px) scale(.98); }
</style>
