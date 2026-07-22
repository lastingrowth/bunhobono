<template>
  <Teleport to="body">
    <Transition name="restore-modal">
      <div v-if="open" class="restore-modal-backdrop" @click.self="$emit('cancel')">
        <section class="restore-modal" role="dialog" aria-modal="true" aria-labelledby="trash-restore-title">
          <div class="restore-modal-icon" aria-hidden="true">↻</div>
          <h2 id="trash-restore-title">지난기록 복원</h2>
          <p>
            <strong>{{ carNo || "차량번호 없음" }}</strong> 기록을<br>
            원래 데이터로 복원하시겠습니까?
          </p>
          <small>복원된 기록은 지난기록 목록에서 제거됩니다.</small>

          <div class="restore-modal-actions">
            <button type="button" class="cancel" :disabled="restoring" @click="$emit('cancel')">취소</button>
            <button type="button" class="confirm" :disabled="restoring" @click="$emit('confirm')">
              {{ restoring ? "복원 중" : "복원" }}
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
  restoring: { type: Boolean, default: false },
});
defineEmits(["cancel", "confirm"]);
</script>

<style scoped>
.restore-modal-backdrop { position: fixed; z-index: 3000; inset: 0; display: grid; place-items: center; padding: 20px; background: rgba(10,12,14,.7); backdrop-filter: blur(2px); }
.restore-modal { box-sizing: border-box; width: min(390px,calc(100vw - 32px)); padding: 25px 25px 21px; border: 1px solid #626a70; border-radius: 8px; color: #e4e7e9; text-align: center; background: #30363b; box-shadow: 0 24px 70px rgba(0,0,0,.52); }
.restore-modal-icon { width: 42px; height: 42px; margin: 0 auto 12px; display: grid; place-items: center; border: 1px solid #747d83; border-radius: 50%; color: #e6e9eb; background: #444b50; font-size: 24px; font-weight: 900; }
.restore-modal h2 { margin: 0; color: #f0f2f3; font-size: 20px; }
.restore-modal p { margin: 14px 0 7px; color: #c6ccd0; font-size: 14px; line-height: 1.65; }
.restore-modal p strong { color: #fff; font-size: 16px; }
.restore-modal small { color: #9da5aa; font-size: 11px; }
.restore-modal-actions { margin-top: 21px; display: grid; grid-template-columns: 1fr 1fr; gap: 8px; }
.restore-modal-actions button { height: 38px; display: inline-flex; align-items: center; justify-content: center; border-radius: 7px; cursor: pointer; font-size: 13px; font-weight: 800; }
.restore-modal-actions .cancel { border: 1px solid #697178; color: #d6dade; background: #3c4348; }
.restore-modal-actions .confirm { border: 1px solid #888f94; color: #fff; background: #555d63; }
.restore-modal-actions .cancel:hover { border-color: #858d93; background: #474f54; }
.restore-modal-actions .confirm:hover { border-color: #a1a7ab; background: #656d73; }
.restore-modal-actions button:disabled { cursor: wait; opacity: .6; }
.restore-modal-enter-active,.restore-modal-leave-active { transition: opacity .16s ease; }
.restore-modal-enter-active .restore-modal,.restore-modal-leave-active .restore-modal { transition: transform .16s ease; }
.restore-modal-enter-from,.restore-modal-leave-to { opacity: 0; }
.restore-modal-enter-from .restore-modal,.restore-modal-leave-to .restore-modal { transform: translateY(8px) scale(.98); }
</style>
