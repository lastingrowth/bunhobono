<template>
    <div v-if="!widgetHidden" ref="widget" class="ai-chat-widget" :style="widgetPosition">

        <div v-if="chatOpen" class="ai-chat-backdrop" aria-hidden="true"></div>

        <!-- 챗봇 대화창 -->
        <section
            v-if="chatOpen"
            class="ai-chat-panel"
            aria-label="AI 챗봇"
        >
            <!-- 챗봇 상단 -->
            <header class="ai-chat-header">
                <div>
                    <strong>보노 AI 챗봇</strong>
                    <span>주차관리 서비스 이용 안내</span>
                </div>

                <div class="ai-chat-header-actions">
                    <button
                        type="button"
                        @click="clearChat"
                    >
                        초기화
                    </button>

                    <button
                        type="button"
                        aria-label="챗봇 닫기"
                        @click="closeChat"
                    >
                        ×
                    </button>
                </div>
            </header>

            <!-- 질문과 답변 목록 -->
            <div
                ref="messageList"
                class="ai-chat-messages"
                aria-live="polite"
            >
                <div
                    v-for="(message, index) in store.messages"
                    :key="index"
                    class="ai-chat-message"
                    :class="{
                        'ai-chat-message-user': message.role === 'user',
                        'ai-chat-message-assistant': message.role === 'assistant'
                    }"
                >
                    <span
                        v-if="message.role === 'assistant'"
                        class="ai-chat-sender"
                    >
                        보노봇
                    </span>

                    <p>{{ message.text }}</p>

                    <small
                        v-if="message.role === 'assistant'
                            && message.responseType === 'AI_UNAVAILABLE'"
                        class="ai-chat-fallback"
                    >
                        AI 연결 오류
                    </small>
                </div>

                <!-- Gemini 응답 대기 표시 -->
                <div
                    v-if="store.sending"
                    class="ai-chat-message ai-chat-message-assistant"
                >
                    <span class="ai-chat-sender">
                        보노봇
                    </span>

                    <p class="ai-chat-waiting">
                        답변을 작성하고 있습니다...
                    </p>
                </div>
            </div>

            <!-- 1:1 문의 이동 -->
            <RouterLink
                class="ai-chat-inquiry-link"
                to="/resident/inquiries/write"
                @click="closeChat"
            >
                해결되지 않았나요? 1:1 문의하기
            </RouterLink>

            <!-- 질문 입력 -->
            <form
                class="ai-chat-form"
                @submit.prevent="submitQuestion"
            >
                <input
                    ref="questionInput"
                    v-model="question"
                    type="text"
                    maxlength="500"
                    placeholder="궁금한 내용을 입력해 주세요."
                    :disabled="store.sending"
                >

                <button
                    type="submit"
                    :disabled="store.sending
                        || !question.trim()"
                >
                    {{ store.sending ? "대기 중" : "전송" }}
                </button>
            </form>
        </section>

        <!-- 챗봇 열기 버튼 -->
        <template v-else>
            <button
                type="button"
                class="ai-chat-hide"
                aria-label="AI 챗봇 숨기기"
                @click.stop="widgetHidden = true"
            >×</button>
            <button
                type="button"
                class="ai-chat-toggle"
                aria-label="AI 챗봇 열기"
                @pointerdown="startDrag"
                @pointermove="moveDrag"
                @pointerup="endDrag"
                @pointercancel="endDrag"
                @click="handleToggleClick"
            >
                <strong>AI</strong>
                <span>챗봇</span>
            </button>
        </template>
    </div>
</template>

<script setup>
import { nextTick, onMounted, onUnmounted, ref, watch } from "vue";
import { useAiChatStore } from "./aiChatStore";

const store = useAiChatStore();

const chatOpen = ref(false);
const widgetHidden = ref(false);
const question = ref("");

const messageList = ref(null);
const questionInput = ref(null);
const widget = ref(null);
const widgetPosition = ref({});
let dragStart = null;
let dragged = false;

const startDrag = (event) => {
    const rect = widget.value?.getBoundingClientRect();
    if (!rect) return;

    dragged = false;
    dragStart = {
        pointerX: event.clientX,
        pointerY: event.clientY,
        left: rect.left,
        top: rect.top,
        width: rect.width,
        height: rect.height
    };
    event.currentTarget.setPointerCapture(event.pointerId);
};

const moveDrag = (event) => {
    if (!dragStart) return;

    const deltaX = event.clientX - dragStart.pointerX;
    const deltaY = event.clientY - dragStart.pointerY;
    if (Math.abs(deltaX) + Math.abs(deltaY) > 6) dragged = true;

    const left = Math.min(Math.max(8, dragStart.left + deltaX), window.innerWidth - dragStart.width - 8);
    const top = Math.min(Math.max(8, dragStart.top + deltaY), window.innerHeight - dragStart.height - 8);
    widgetPosition.value = { left: `${left}px`, top: `${top}px`, right: "auto", bottom: "auto" };
};

const endDrag = (event) => {
    if (event.currentTarget.hasPointerCapture(event.pointerId)) {
        event.currentTarget.releasePointerCapture(event.pointerId);
    }
    dragStart = null;
};

const handleToggleClick = () => {
    if (dragged) {
        dragged = false;
        return;
    }
    openChat();
};

const openFromMenu = () => {
    widgetHidden.value = false;
    openChat();
};

onMounted(() => window.addEventListener("open-ai-chat", openFromMenu));
onUnmounted(() => window.removeEventListener("open-ai-chat", openFromMenu));

// 챗봇 대화창 열기
const openChat = async () => {
    chatOpen.value = true;

    await nextTick();

    // 모바일에서는 창 전체를 먼저 보여주고, 입력창을 누를 때만 키보드를 연다.
    if (!window.matchMedia("(max-width: 600px)").matches) {
        questionInput.value?.focus();
    }
    scrollToBottom();
};

// 챗봇 대화창 닫기
const closeChat = () => {
    chatOpen.value = false;
};

// 사용자 질문 전송
const submitQuestion = async () => {
    const value = question.value.trim();

    if (!value || store.sending) {
        return;
    }

    question.value = "";

    await store.sendQuestion(value);
    await scrollToBottom();

    questionInput.value?.focus();
};

// 기존 대화 내용 초기화
const clearChat = async () => {
    store.clearMessages();

    await scrollToBottom();

    questionInput.value?.focus();
};

// 가장 최근 질문과 답변이 보이도록 아래로 이동
const scrollToBottom = async () => {
    await nextTick();

    if (messageList.value) {
        messageList.value.scrollTop =
            messageList.value.scrollHeight;
    }
};

// 질문 또는 답변이 추가되면 대화 목록 아래로 이동
watch(
    () => store.messages.length,
    () => {
        scrollToBottom();
    }
);
</script>

<style scoped>
.ai-chat-widget {
    position: fixed;
    z-index: 1200;
    right: 28px;
    bottom: 28px;
}

.ai-chat-toggle {
    width: 68px;
    height: 68px;
    padding: 0;
    display: flex;
    justify-content: center;
    align-items: center;
    flex-direction: column;
    gap: 1px;
    border: none;
    border-radius: 50%;
    cursor: pointer;
    color: #ffffff;
    background: #315c86;
    box-shadow: 0 10px 28px rgba(31, 68, 103, .32);
    touch-action: none;
    user-select: none;
}

.ai-chat-hide {
    position: absolute;
    z-index: 2;
    top: -7px;
    right: -7px;
    width: 24px;
    height: 24px;
    padding: 0;
    border: 2px solid #ffffff;
    border-radius: 50%;
    color: #ffffff;
    font-size: 17px;
    font-weight: 900;
    line-height: 20px;
    cursor: pointer;
    background: #263746;
    box-shadow: 0 3px 9px rgba(22, 43, 62, .28);
}

.ai-chat-toggle:hover {
    background: #24496d;
    transform: translateY(-2px);
}

.ai-chat-toggle strong {
    font-size: 19px;
    line-height: 1;
}

.ai-chat-toggle span {
    font-size: 11px;
    font-weight: 700;
}

.ai-chat-panel {
    width: min(390px, calc(100vw - 32px));
    height: min(570px, calc(100vh - 100px));
    display: flex;
    overflow: hidden;
    flex-direction: column;
    border: 1px solid #cbddeb;
    border-radius: 18px;
    background: #ffffff;
    box-shadow: 0 18px 48px rgba(31, 68, 103, .28);
}

.ai-chat-header {
    min-height: 72px;
    padding: 15px 17px;
    display: flex;
    justify-content: space-between;
    align-items: center;
    gap: 12px;
    color: #ffffff;
    background: #315c86;
}

.ai-chat-header > div:first-child {
    min-width: 0;
    display: flex;
    flex-direction: column;
    gap: 3px;
}

.ai-chat-header strong {
    font-size: 17px;
}

.ai-chat-header span {
    font-size: 11px;
    color: rgba(255, 255, 255, .76);
}

.ai-chat-header-actions {
    display: flex;
    align-items: center;
    gap: 5px;
}

.ai-chat-header-actions button {
    min-width: 34px;
    height: 32px;
    padding: 0 8px;
    border: 1px solid rgba(255, 255, 255, .28);
    border-radius: 8px;
    cursor: pointer;
    color: #ffffff;
    background: rgba(255, 255, 255, .1);
    font-size: 12px;
    font-weight: 700;
}

.ai-chat-header-actions button:last-child {
    padding: 0;
    font-size: 22px;
    line-height: 1;
}

.ai-chat-header-actions button:hover {
    background: rgba(255, 255, 255, .2);
}

.ai-chat-messages {
    min-height: 0;
    padding: 18px 15px;
    display: flex;
    overflow-y: auto;
    flex: 1;
    flex-direction: column;
    gap: 14px;
    background: #f5f9fc;
}

.ai-chat-message {
    max-width: 82%;
    display: flex;
    flex-direction: column;
    gap: 5px;
}

.ai-chat-message p {
    margin: 0;
    padding: 11px 13px;
    border-radius: 14px;
    line-height: 1.55;
    white-space: pre-wrap;
    word-break: keep-all;
    overflow-wrap: anywhere;
    font-size: 13px;
}

.ai-chat-message-assistant {
    align-self: flex-start;
}

.ai-chat-message-assistant p {
    border: 1px solid #d8e5ef;
    border-top-left-radius: 4px;
    color: #294966;
    background: #ffffff;
}

.ai-chat-message-user {
    align-self: flex-end;
}

.ai-chat-message-user p {
    border-top-right-radius: 4px;
    color: #ffffff;
    background: #315c86;
}

.ai-chat-sender {
    padding-left: 3px;
    font-size: 11px;
    font-weight: 800;
    color: #5c7891;
}

.ai-chat-fallback {
    padding-left: 3px;
    font-size: 10px;
    color: #8a6a2f;
}

.ai-chat-waiting {
    color: #71869a !important;
}

.ai-chat-inquiry-link {
    padding: 10px 15px;
    border-top: 1px solid #e2ebf2;
    border-bottom: 1px solid #e2ebf2;
    text-align: center;
    text-decoration: none;
    color: #315c86;
    background: #ffffff;
    font-size: 12px;
    font-weight: 700;
}

.ai-chat-inquiry-link:hover {
    background: #edf6fc;
}

.ai-chat-form {
    padding: 12px;
    display: flex;
    gap: 8px;
    background: #ffffff;
}

.ai-chat-form input {
    min-width: 0;
    height: 42px;
    padding: 0 12px;
    flex: 1;
    border: 1px solid #cbddeb;
    border-radius: 10px;
    outline: none;
    color: #294966;
    background: #ffffff;
    font-size: 13px;
}

.ai-chat-form input:focus {
    border-color: #315c86;
    box-shadow: 0 0 0 3px rgba(49, 92, 134, .12);
}

.ai-chat-form button {
    min-width: 64px;
    height: 42px;
    padding: 0 12px;
    border: none;
    border-radius: 10px;
    cursor: pointer;
    color: #ffffff;
    background: #315c86;
    font-size: 12px;
    font-weight: 800;
}

.ai-chat-form button:hover {
    background: #24496d;
}

.ai-chat-form button:disabled,
.ai-chat-form input:disabled {
    cursor: not-allowed;
    opacity: .55;
}

@media (max-width: 600px) {
    .ai-chat-backdrop {
        position: fixed;
        z-index: 1290;
        inset: 0;
        background: rgba(25, 42, 58, .18);
        backdrop-filter: blur(5px);
        -webkit-backdrop-filter: blur(5px);
    }

    .ai-chat-widget {
        right: 12px;
        bottom: 76px;
    }

    .ai-chat-toggle {
        width: 60px;
        height: 60px;
    }

    .ai-chat-panel {
        position: fixed;
        z-index: 1300;
        top: auto;
        right: 12px;
        bottom: calc(100px + env(safe-area-inset-bottom));
        left: 12px;
        width: auto;
        height: min(430px, calc(100dvh - 220px));
        max-height: 430px;
        border-radius: 16px;
    }

    .ai-chat-header { min-height: 50px; padding: 7px 11px; }
    .ai-chat-header strong { font-size: 16px; }
    .ai-chat-header span { font-size: 11px; }
    .ai-chat-messages { min-height: 0; padding: 9px 11px; overscroll-behavior: contain; }
    .ai-chat-message { max-width: 88%; }
    .ai-chat-message p { padding: 9px 11px; font-size: 13px; line-height: 1.45; overflow-wrap: anywhere; }
    .ai-chat-inquiry-link { padding: 6px 10px; font-size: 11px; }
    .ai-chat-form { flex-shrink: 0; margin: 7px; padding: 3px; gap: 4px; border: 1px solid #9eb8cc; border-radius: 13px; background:#fff; box-shadow:none; }
    .ai-chat-form input { height: 40px; padding:0 9px; border:0!important; border-radius:10px; outline:0; font-size:16px; box-shadow:none!important; }
    .ai-chat-form input:focus { border:0!important; outline:0; box-shadow:none!important; }
    .ai-chat-form button { min-width:58px; height:40px; padding:0 9px; border-radius:9px; }
}
</style>
