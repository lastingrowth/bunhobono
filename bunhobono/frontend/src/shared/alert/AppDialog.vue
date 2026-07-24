<template>
    <!-- 어떤 레이아웃에서도 잘리지 않도록 body 아래에 Dialog를 표시합니다. -->
    <Teleport to="body">
        <Transition name="app-dialog-fade">
            <div
                v-if="dialogState.open"
                class="app-dialog-backdrop"
                @keydown.esc="cancelDialogAction"
            >
                <section
                    class="app-dialog"
                    :class="[
                        `app-dialog--${resolvedTheme}`,
                        `app-dialog--${dialogState.type}`
                    ]"
                    role="dialog"
                    aria-modal="true"
                    :aria-labelledby="titleId"
                    :aria-describedby="messageId"
                >
                    <!-- 우측 상단 닫기 버튼 -->
                    <button
                        type="button"
                        class="app-dialog-close"
                        aria-label="Dialog 닫기"
                        @click="cancelDialogAction"
                    >
                        ×
                    </button>

                    <!-- 성공, 안내, 경고, 오류 상태를 구분하는 아이콘 -->
                    <div
                        class="app-dialog-icon"
                        aria-hidden="true"
                    >
                        {{ dialogIcon }}
                    </div>

                    <h2
                        :id="titleId"
                        class="app-dialog-title"
                    >
                        {{ dialogState.title }}
                    </h2>

                    <p
                        :id="messageId"
                        class="app-dialog-message"
                    >
                        {{ dialogState.message }}
                    </p>

                    <!-- 삭제나 전출처럼 추가 경고가 필요한 경우에만 표시합니다. -->
                    <small
                        v-if="dialogState.caution"
                        class="app-dialog-caution"
                    >
                        {{ dialogState.caution }}
                    </small>

                    <div
                        class="app-dialog-actions"
                        :class="{
                            'app-dialog-actions--single':
                                dialogState.mode === 'alert'
                        }"
                    >
                        <!-- confirm Dialog에서만 취소 버튼을 표시합니다. -->
                        <button
                            v-if="dialogState.mode === 'confirm'"
                            type="button"
                            class="app-dialog-button app-dialog-button--cancel"
                            @click="cancelDialogAction"
                        >
                            {{ dialogState.cancelText }}
                        </button>

                        <button
                            ref="confirmButton"
                            type="button"
                            class="app-dialog-button app-dialog-button--confirm"
                            @click="confirmDialogAction"
                        >
                            {{ dialogState.confirmText }}
                        </button>
                    </div>
                </section>
            </div>
        </Transition>
    </Teleport>
</template>

<script setup>
import {
    computed,
    nextTick,
    onBeforeUnmount,
    onMounted,
    ref,
    watch
} from "vue";
import { useRoute } from "vue-router";
import { useDialog } from "./useDialog";

const route = useRoute();

const {
    dialogState,
    confirmDialogAction,
    cancelDialogAction
} = useDialog();

const confirmButton = ref(null);

const titleId = "app-dialog-title";
const messageId = "app-dialog-message";

// 호출할 때 theme을 직접 전달하면 그 값을 사용합니다.
// theme을 생략한 경우 현재 주소가 /admin인지 확인하여 자동 적용합니다.
const resolvedTheme = computed(() => {
    if (dialogState.theme) {
        return dialogState.theme;
    }

    return route.path.startsWith("/admin")
        ? "admin"
        : "resident";
});

// Dialog 종류에 따라 아이콘을 다르게 표시합니다.
const dialogIcon = computed(() => {
    switch (dialogState.type) {
        case "success":
            return "✓";

        case "warning":
        case "danger":
        case "error":
            return "!";

        default:
            return "i";
    }
});

// Dialog가 열리면 확인 버튼으로 키보드 포커스를 이동합니다.
watch(
    () => dialogState.open,
    async (open) => {
        if (!open) return;

        await nextTick();
        confirmButton.value?.focus();
    }
);

// ESC 입력은 Teleport된 영역의 포커스 상태와 관계없이 처리합니다.
const handleEscapeKey = (event) => {
    if (event.key === "Escape" && dialogState.open) {
        cancelDialogAction();
    }
};

onMounted(() => {
    window.addEventListener("keydown", handleEscapeKey);
});

onBeforeUnmount(() => {
    window.removeEventListener("keydown", handleEscapeKey);
});
</script>

<style scoped>
/* 모든 Dialog가 공유하는 반투명 배경입니다. */
.app-dialog-backdrop {
    position: fixed;
    z-index: 3000;
    inset: 0;
    padding: 20px;
    display: grid;
    place-items: center;
    background: rgba(10, 12, 14, 0.68);
    backdrop-filter: blur(2px);
}

/* Dialog의 공통 크기와 배치입니다. */
.app-dialog {
    box-sizing: border-box;
    position: relative;
    width: min(440px, calc(100vw - 32px));
    padding: 32px 28px 24px;
    text-align: center;
    border-radius: 12px;
    box-shadow: 0 24px 70px rgba(0, 0, 0, 0.35);
}

/* 우측 상단 닫기 버튼 */
.app-dialog-close {
    position: absolute;
    top: 13px;
    right: 15px;
    width: 32px;
    height: 32px;
    padding: 0;
    display: grid;
    place-items: center;
    border: none;
    border-radius: 6px;
    cursor: pointer;
    color: inherit;
    background: transparent;
    font-size: 25px;
    line-height: 1;
    opacity: 0.65;
}

.app-dialog-close:hover {
    opacity: 1;
}

/* 상태 아이콘 */
.app-dialog-icon {
    width: 52px;
    height: 52px;
    margin: 0 auto 18px;
    display: grid;
    place-items: center;
    border: 1px solid currentColor;
    border-radius: 50%;
    font-size: 28px;
    font-weight: 800;
}

/* 제목과 메시지 */
.app-dialog-title {
    margin: 0;
    font-size: 23px;
    font-weight: 800;
    line-height: 1.35;
}

.app-dialog-message {
    margin: 16px 0 0;
    font-size: 15px;
    line-height: 1.7;
    white-space: pre-line;
}

.app-dialog-caution {
    margin-top: 10px;
    display: block;
    font-size: 12px;
    line-height: 1.6;
}

/* 하단 버튼 영역 */
.app-dialog-actions {
    margin-top: 26px;
    display: grid;
    grid-template-columns: repeat(2, minmax(0, 1fr));
    gap: 10px;
}

.app-dialog-actions--single {
    grid-template-columns: 1fr;
}

.app-dialog-button {
    min-height: 46px;
    padding: 10px 18px;
    border-radius: 8px;
    cursor: pointer;
    font-size: 14px;
    font-weight: 800;
    transition:
        border-color 0.15s ease,
        background-color 0.15s ease,
        box-shadow 0.15s ease;
}

/* =========================
   Resident 밝은 테마
   ========================= */

.app-dialog--resident {
    border: 1px solid #d9dee3;
    color: #171c21;
    background: #ffffff;
}

.app-dialog--resident .app-dialog-message {
    color: #5f6972;
}

.app-dialog--resident .app-dialog-caution {
    color: #717b84;
}

.app-dialog--resident .app-dialog-button--cancel {
    border: 1px solid #d9dee3;
    color: #3f4b56;
    background: #ffffff;
}

.app-dialog--resident .app-dialog-button--cancel:hover {
    background: #f3f5f7;
}

.app-dialog--resident .app-dialog-button--confirm {
    border: 1px solid #3f4b56;
    color: #ffffff;
    background: #3f4b56;
}

.app-dialog--resident .app-dialog-button--confirm:hover {
    border-color: #202830;
    background: #202830;
}

.app-dialog--resident .app-dialog-button:focus-visible {
    outline: none;
    box-shadow: 0 0 0 3px rgba(63, 75, 86, 0.25);
}

/* =========================
   Admin 어두운 테마
   기존 ManagementDeleteConfirm과
   같은 색상 분위기를 사용합니다.
   ========================= */

.app-dialog--admin {
    border: 1px solid #626a70;
    color: #f0f2f3;
    background: #30363b;
}

.app-dialog--admin .app-dialog-message {
    color: #c6ccd0;
}

.app-dialog--admin .app-dialog-caution {
    color: #9da5aa;
}

.app-dialog--admin .app-dialog-button--cancel {
    border: 1px solid #697178;
    color: #d6dade;
    background: #3c4348;
}

.app-dialog--admin .app-dialog-button--cancel:hover {
    border-color: #858d93;
    background: #474f54;
}

.app-dialog--admin .app-dialog-button--confirm {
    border: 1px solid #888f94;
    color: #ffffff;
    background: #555d63;
}

.app-dialog--admin .app-dialog-button--confirm:hover {
    border-color: #a1a7ab;
    background: #656d73;
}

.app-dialog--admin .app-dialog-button:focus-visible {
    outline: none;
    box-shadow: 0 0 0 3px rgba(198, 204, 208, 0.25);
}

/* =========================
   Dialog 상태별 아이콘 색상
   ========================= */

.app-dialog--resident.app-dialog--success .app-dialog-icon {
    color: #4f7d59;
    background: #f2f8f3;
}

.app-dialog--resident.app-dialog--warning .app-dialog-icon,
.app-dialog--resident.app-dialog--danger .app-dialog-icon,
.app-dialog--resident.app-dialog--error .app-dialog-icon {
    color: #9a5151;
    background: #fff5f5;
}

.app-dialog--resident.app-dialog--info .app-dialog-icon {
    color: #586977;
    background: #f3f6f8;
}

.app-dialog--admin .app-dialog-icon {
    color: #e6e9eb;
    background: #444b50;
}

.app-dialog--admin.app-dialog--success .app-dialog-icon {
    color: #cbd8cd;
    background: #414a44;
}

/* danger일 때 확인 버튼을 조금 더 분명하게 표시합니다. */
.app-dialog--admin.app-dialog--danger .app-dialog-button--confirm,
.app-dialog--admin.app-dialog--error .app-dialog-button--confirm {
    border-color: #92989c;
    background: #62696e;
}

/* 열고 닫을 때 부드러운 전환 효과 */
.app-dialog-fade-enter-active,
.app-dialog-fade-leave-active {
    transition: opacity 0.16s ease;
}

.app-dialog-fade-enter-active .app-dialog,
.app-dialog-fade-leave-active .app-dialog {
    transition: transform 0.16s ease;
}

.app-dialog-fade-enter-from,
.app-dialog-fade-leave-to {
    opacity: 0;
}

.app-dialog-fade-enter-from .app-dialog,
.app-dialog-fade-leave-to .app-dialog {
    transform: translateY(8px) scale(0.98);
}

/* 작은 화면 대응 */
@media (max-width: 480px) {
    .app-dialog {
        padding: 30px 20px 20px;
    }

    .app-dialog-title {
        font-size: 20px;
    }

    .app-dialog-actions {
        gap: 8px;
    }
}
</style>