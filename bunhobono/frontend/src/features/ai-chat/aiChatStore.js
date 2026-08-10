import { defineStore } from "pinia";
import { ref } from "vue";
import { sendAiChat } from "./aiChatApi";

export const useAiChatStore = defineStore("aiChat", () => {

    // 화면에 표시할 사용자 질문과 챗봇 답변
    const messages = ref([
        {
            role: "assistant",
            text: "안녕하세요. 주차 및 방문차량 이용 방법을 질문해 주세요.",
            fallback: false
        }
    ]);

    // 챗봇 답변을 기다리는 중인지 여부
    const sending = ref(false);

    // API 요청 실패 안내 메시지
    const errorMessage = ref("");

    // 사용자 질문 전송
    const sendQuestion = async (question) => {
        const trimmedQuestion = question.trim();

        if (!trimmedQuestion || sending.value) {
            return null;
        }

        // 사용자가 입력한 질문을 대화 목록에 추가
        messages.value.push({
            role: "user",
            text: trimmedQuestion,
            fallback: false
        });

        sending.value = true;
        errorMessage.value = "";

        try {
            const response = await sendAiChat({
                question: trimmedQuestion
            });

            // 서버에서 받은 답변을 대화 목록에 추가
            messages.value.push({
                role: "assistant",
                text: response.data.answer,
                fallback: response.data.fallback
            });

            return response.data;

        } catch (error) {
            errorMessage.value =
                error?.response?.data?.message
                || "챗봇 답변을 불러오지 못했습니다.";

            // 요청 자체가 실패한 경우에도 대화창에 안내 표시
            messages.value.push({
                role: "assistant",
                text: errorMessage.value,
                fallback: true
            });

            return null;

        } finally {
            sending.value = false;
        }
    };

    // 기존 대화 내용을 지우고 처음 안내 문구로 초기화
    const clearMessages = () => {
        messages.value = [
            {
                role: "assistant",
                text: "안녕하세요. 주차 및 방문차량 이용 방법을 질문해 주세요.",
                fallback: false
            }
        ];

        errorMessage.value = "";
    };

    return {
        messages,
        sending,
        errorMessage,

        sendQuestion,
        clearMessages
    };
});