import api from "@/shared/api/apiClient";

// 입주민 문의 등록
export const createInquiry = (data) => {
    return api.post("/inquiries", data);
};

// 입주민 본인 문의 목록 조회
export const getResidentInquiries = () => {
    return api.get("/inquiries/resident");
};

// 입주민 본인 문의 상세 조회
export const getResidentInquiry = (inquiryNo) => {
    return api.get(`/inquiries/resident/${inquiryNo}`);
};

// 입주민 재문의 등록
export const createReInquiry = (inquiryNo, content) => {
    return api.post(
        `/inquiries/resident/${inquiryNo}/re-inquiry`,
        { content }
    );
};

// 관리자 상태별 문의 목록 조회
export const getAdminInquiries = (status) => {
    return api.get("/inquiries/admin", {
        params: { status }
    });
};

// 관리자 문의 상세 조회
export const getAdminInquiry = (inquiryNo) => {
    return api.get(`/inquiries/admin/${inquiryNo}`);
};

// 관리자 답변 등록
export const answerInquiry = (inquiryNo, answerContent) => {
    return api.patch(
        `/inquiries/admin/${inquiryNo}/answer`,
        { answerContent }
    );
};