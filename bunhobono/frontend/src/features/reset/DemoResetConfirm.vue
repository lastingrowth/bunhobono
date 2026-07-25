<template>
  <Teleport to="body">
    <Transition name="reset-modal">
      <div
        v-if="open"
        class="reset-modal-backdrop"
        @click.self="!resetting && $emit('cancel')">
        <section
          class="reset-modal"
          role="dialog"
          aria-modal="true"
          aria-labelledby="demo-reset-title">
          <div class="reset-modal-icon" aria-hidden="true">↻</div>
          <h2 id="demo-reset-title">시연 초기화</h2>
          <p>
            현재 데이터를 초기 더미 데이터로 되돌리고<br>
            모든 영상을 첫 화면에서 대기시킵니다.
          </p>
          <small>현재 시연 중 생성된 데이터는 삭제됩니다.</small>
          <div class="reset-modal-actions">
            <button
              type="button"
              class="cancel"
              :disabled="resetting"
              @click="$emit('cancel')">
              취소
            </button>
            <button
              type="button"
              class="confirm"
              :disabled="resetting"
              @click="$emit('confirm')">
              {{ resetting ? '초기화 중...' : '초기화' }}
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
  resetting: { type: Boolean, default: false },
})

defineEmits(['cancel', 'confirm'])
</script>

<style scoped>
.reset-modal-backdrop { position: fixed; z-index: 3000; inset: 0; display: grid; place-items: center; padding: 20px; background: rgba(10,12,14,.7); backdrop-filter: blur(2px); }
.reset-modal { box-sizing: border-box; width: min(390px,calc(100vw - 32px)); padding: 25px 25px 21px; border: 1px solid #626a70; border-radius: 8px; color: #e4e7e9; text-align: center; background: #30363b; box-shadow: 0 24px 70px rgba(0,0,0,.52); }
.reset-modal-icon { width: 42px; height: 42px; margin: 0 auto 12px; display: grid; place-items: center; border: 1px solid #747d83; border-radius: 50%; color: #e6e9eb; background: #444b50; font-size: 25px; font-weight: 900; }
.reset-modal h2 { margin: 0; color: #f0f2f3; font-size: 20px; }
.reset-modal p { margin: 14px 0 7px; color: #c6ccd0; font-size: 14px; line-height: 1.65; }
.reset-modal small { color: #9da5aa; font-size: 11px; }
.reset-modal-actions { margin-top: 21px; display: grid; grid-template-columns: 1fr 1fr; gap: 8px; }
.reset-modal-actions button { height: 38px; display: inline-flex; align-items: center; justify-content: center; border-radius: 7px; cursor: pointer; font-size: 13px; font-weight: 800; }
.reset-modal-actions .cancel { border: 1px solid #697178; color: #d6dade; background: #3c4348; }
.reset-modal-actions .confirm { border: 1px solid #888f94; color: #fff; background: #555d63; }
.reset-modal-actions button:disabled { cursor: wait; opacity: .6; }
.reset-modal-enter-active,.reset-modal-leave-active { transition: opacity .16s ease; }
.reset-modal-enter-active .reset-modal,.reset-modal-leave-active .reset-modal { transition: transform .16s ease; }
.reset-modal-enter-from,.reset-modal-leave-to { opacity: 0; }
.reset-modal-enter-from .reset-modal,.reset-modal-leave-to .reset-modal { transform: translateY(8px) scale(.98); }
</style>
