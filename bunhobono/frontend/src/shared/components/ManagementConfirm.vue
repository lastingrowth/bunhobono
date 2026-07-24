<template>
  <Teleport to="body">
    <Transition name="management-confirm">
      <div
        v-if="open"
        class="management-confirm-backdrop"
        @click.self="$emit('cancel')"
      >
        <section
          class="management-confirm"
          role="dialog"
          aria-modal="true"
          aria-labelledby="management-confirm-title"
        >
          <div class="management-confirm-icon" aria-hidden="true">{{ icon }}</div>
          <h2 id="management-confirm-title">{{ title }}</h2>
          <p>
            <strong v-if="itemName">{{ itemName }}</strong>
            <br v-if="itemName">
            {{ message }}
          </p>
          <small v-if="caution">{{ caution }}</small>

          <div class="management-confirm-actions">
            <button
              type="button"
              class="cancel"
              :disabled="processing"
              @click="$emit('cancel')"
            >
              {{ cancelText }}
            </button>
            <button
              type="button"
              class="confirm"
              :disabled="processing"
              @click="$emit('confirm')"
            >
              {{ processing ? processingText : confirmText }}
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
  icon: { type: String, default: "!" },
  title: { type: String, default: "확인" },
  itemName: { type: String, default: "" },
  message: { type: String, default: "계속 진행하시겠습니까?" },
  caution: { type: String, default: "" },
  processing: { type: Boolean, default: false },
  cancelText: { type: String, default: "취소" },
  confirmText: { type: String, default: "확인" },
  processingText: { type: String, default: "처리 중" },
});

// 실제 기능은 부모 화면에서 처리하고, 이 컴포넌트는 선택 결과만 전달합니다.
defineEmits(["cancel", "confirm"]);
</script>

<style scoped>
/* 기존 admin 삭제 Dialog와 동일한 분위기의 범용 확인창입니다. */
.management-confirm-backdrop { position: fixed; z-index: 3000; inset: 0; display: grid; place-items: center; padding: 20px; background: rgba(10,12,14,.7); backdrop-filter: blur(2px); }
.management-confirm { box-sizing: border-box; width: min(390px,calc(100vw - 32px)); padding: 25px 25px 21px; border: 1px solid #626a70; border-radius: 8px; color: #e4e7e9; text-align: center; background: #30363b; box-shadow: 0 24px 70px rgba(0,0,0,.52); }
.management-confirm-icon { width: 42px; height: 42px; margin: 0 auto 12px; display: grid; place-items: center; border: 1px solid #747d83; border-radius: 50%; color: #e6e9eb; background: #444b50; font-size: 24px; font-weight: 900; }
.management-confirm h2 { margin: 0; color: #f0f2f3; font-size: 20px; }
.management-confirm p { margin: 14px 0 7px; color: #c6ccd0; font-size: 14px; line-height: 1.65; }
.management-confirm p strong { color: #fff; font-size: 16px; }
.management-confirm small { color: #9da5aa; font-size: 11px; }
.management-confirm-actions { margin-top: 21px; display: grid; grid-template-columns: 1fr 1fr; gap: 8px; }
.management-confirm-actions button { height: 38px; display: inline-flex; align-items: center; justify-content: center; border-radius: 7px; cursor: pointer; font-size: 13px; font-weight: 800; }
.management-confirm-actions .cancel { border: 1px solid #697178; color: #d6dade; background: #3c4348; }
.management-confirm-actions .confirm { border: 1px solid #888f94; color: #fff; background: #555d63; }
.management-confirm-actions .cancel:hover { border-color: #858d93; background: #474f54; }
.management-confirm-actions .confirm:hover { border-color: #a1a7ab; background: #656d73; }
.management-confirm-actions button:disabled { cursor: wait; opacity: .6; }
.management-confirm-enter-active,.management-confirm-leave-active { transition: opacity .16s ease; }
.management-confirm-enter-active .management-confirm,.management-confirm-leave-active .management-confirm { transition: transform .16s ease; }
.management-confirm-enter-from,.management-confirm-leave-to { opacity: 0; }
.management-confirm-enter-from .management-confirm,.management-confirm-leave-to .management-confirm { transform: translateY(8px) scale(.98); }
</style>
