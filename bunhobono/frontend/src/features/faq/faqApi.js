import api from "@/shared/api/apiClient";

// 자주하는 질문 목록 조회
export const getFaqs = () => {
    return api.get("/faqs");
};

// 자주하는 질문 등록
export const createFaq = (data) => {
    return api.post("/faqs/signup", data);
};

// 자주하는 질문 수정
export const updateFaq = (faqNo, data) => {
    return api.put(`/faqs/edit/${faqNo}`, data);
};

// 자주하는 질문 삭제
export const deleteFaq = (faqNo) => {
    return api.delete(`/faqs/delete/${faqNo}`);
};