import api from "@/shared/api/apiClient";

// 챗봇 질문 전송
export const sendAiChat = (data) => {
    return api.post(
        "/ai-chat",
        data,
        {
            timeout: 30000
        });
};