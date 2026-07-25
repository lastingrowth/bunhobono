<template>
  <Teleport to="body">
    <Transition name="delete-modal">
      <div v-if="open" class="delete-modal-backdrop" @click.self="$emit('cancel')">
        <section class="delete-modal" role="dialog" aria-modal="true" aria-labelledby="management-delete-title">
          <div class="delete-modal-icon" aria-hidden="true">!</div>
          <h2 id="management-delete-title">{{ title }}</h2>
          <p><strong>{{ itemName }}</strong><br>{{ message }}</p>
          <small>{{ caution }}</small>
          <div class="delete-modal-actions">
            <button type="button" class="cancel" :disabled="deleting" @click="$emit('cancel')">취소</button>
            <!-- 화면 용도에 따라 확인 버튼 문구만 변경할 수 있습니다. -->
            <button
              type="button"
              class="confirm"
              :disabled="deleting"
              @click="$emit('confirm')"
            >
              {{ deleting ? processingText : confirmText }}
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
  title: { type: String, default: "기록 삭제" },
  itemName: { type: String, default: "선택한 항목" },
  message: { type: String, default: "삭제하시겠습니까?" },
  caution: { type: String, default: "삭제된 항목은 복원할 수 없습니다." },
  deleting: { type: Boolean, default: false },

  // 지정하지 않은 기존 화면은 계속 '삭제', '삭제 중'으로 표시됩니다.
  confirmText: { type: String, default: "삭제" },
  processingText: { type: String, default: "삭제 중" },
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
.delete-modal-actions button { height: 38px; display: inline-flex; align-items: center; justify-content: center; border-radius: 7px; cursor: pointer; font-size: 13px; font-weight: 800; }
.delete-modal-actions .cancel { border: 1px solid #697178; color: #d6dade; background: #3c4348; }
.delete-modal-actions .confirm { border: 1px solid #888f94; color: #fff; background: #555d63; }
.delete-modal-actions button:disabled { cursor: wait; opacity: .6; }
.delete-modal-enter-active,.delete-modal-leave-active { transition: opacity .16s ease; }
.delete-modal-enter-active .delete-modal,.delete-modal-leave-active .delete-modal { transition: transform .16s ease; }
.delete-modal-enter-from,.delete-modal-leave-to { opacity: 0; }
.delete-modal-enter-from .delete-modal,.delete-modal-leave-to .delete-modal { transform: translateY(8px) scale(.98); }
</style>
