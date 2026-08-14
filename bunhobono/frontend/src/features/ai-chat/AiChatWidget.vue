<template>
    <div class="ai-chat-widget">

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
        <button
            v-else
            type="button"
            class="ai-chat-toggle"
            aria-label="AI 챗봇 열기"
            @click="openChat"
        >
            <span class="ai-sparkle ai-sparkle-main" aria-hidden="true">✦</span>
            <span class="ai-sparkle ai-sparkle-left" aria-hidden="true">✦</span>
            <span class="ai-sparkle ai-sparkle-right" aria-hidden="true">✦</span>
        </button>
    </div>
</template>

<script setup>
import { nextTick, ref, watch } from "vue";
import { useAiChatStore } from "./aiChatStore";

const store = useAiChatStore();

const chatOpen = ref(false);
const question = ref("");

const messageList = ref(null);
const questionInput = ref(null);

// 챗봇 대화창 열기
const openChat = async () => {
    chatOpen.value = true;

    await nextTick();

    questionInput.value?.focus();
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
    position: relative;
    width: 72px;
    height: 72px;
    padding: 0;
    display: grid;
    place-items: center;
    border: 3px solid transparent;
    border-radius: 50%;
    cursor: pointer;
    color: #23a6d5;
    background:
        linear-gradient(#fff, #fff) padding-box,
        linear-gradient(145deg, #63cbea, #23a6d5) border-box;
    box-shadow: 0 10px 28px rgba(35, 166, 213, .26);
    transition: transform .2s ease, box-shadow .2s ease;
}

.ai-chat-toggle:hover {
    transform: translateY(-2px);
    box-shadow: 0 14px 32px rgba(35, 166, 213, .36);
}

.ai-sparkle {
    position: absolute;
    display: block;
    color: #23a6d5;
    line-height: 1;
    filter: drop-shadow(0 2px 3px rgba(35, 166, 213, .2));
}

.ai-sparkle-main {
    top: 15px;
    left: 24px;
    font-size: 33px;
    line-height: 1;
    animation: sparkle-float-main 2.8s ease-in-out infinite;
}

.ai-sparkle-left {
    bottom: 16px;
    left: 14px;
    color: #54c1e4;
    font-size: 18px;
    animation: sparkle-float-left 2.2s ease-in-out infinite .25s;
}

.ai-sparkle-right {
    right: 13px;
    bottom: 14px;
    color: #8bd8ed;
    font-size: 13px;
    animation: sparkle-float-right 2.5s ease-in-out infinite .5s;
}

.ai-chat-toggle:hover .ai-sparkle {
    animation-duration: 1.5s;
}

@keyframes sparkle-float-main {
    0%, 100% { transform: translate(0, 0) rotate(0deg) scale(1); }
    50% { transform: translate(2px, -4px) rotate(7deg) scale(1.06); }
}

@keyframes sparkle-float-left {
    0%, 100% { transform: translate(0, 0) rotate(0deg) scale(.94); }
    50% { transform: translate(-2px, -3px) rotate(-10deg) scale(1.08); }
}

@keyframes sparkle-float-right {
    0%, 100% { transform: translate(0, 0) rotate(0deg) scale(.9); }
    50% { transform: translate(2px, 3px) rotate(12deg) scale(1.12); }
}

@media (prefers-reduced-motion: reduce) {
    .ai-sparkle {
        animation: none;
    }
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
    background: #23a6d5;
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
    background: #23a6d5;
}

.ai-chat-sender {
    padding-left: 3px;
    font-size: 11px;
    font-weight: 800;
    color: #23a6d5;
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
    color: #23a6d5;
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
    border-color: #23a6d5;
    box-shadow: 0 0 0 3px rgba(35, 166, 213, .14);
}

.ai-chat-form button {
    min-width: 64px;
    height: 42px;
    padding: 0 12px;
    border: none;
    border-radius: 10px;
    cursor: pointer;
    color: #ffffff;
    background: #23a6d5;
    font-size: 12px;
    font-weight: 800;
}

.ai-chat-form button:hover {
    background: #168fbe;
}

.ai-chat-form button:disabled,
.ai-chat-form input:disabled {
    cursor: not-allowed;
    opacity: .55;
}

@media (max-width: 600px) {
    .ai-chat-widget {
        right: 12px;
        bottom: 12px;
    }

    .ai-chat-toggle {
        width: 60px;
        height: 60px;
    }

    .ai-sparkle-main { top: 11px; left: 20px; font-size: 29px; }
    .ai-sparkle-left { bottom: 13px; left: 11px; font-size: 16px; }
    .ai-sparkle-right { right: 10px; bottom: 12px; font-size: 12px; }

    .ai-chat-panel {
        width: calc(100vw - 24px);
        height: min(600px, calc(100vh - 84px));
    }
}
</style>
