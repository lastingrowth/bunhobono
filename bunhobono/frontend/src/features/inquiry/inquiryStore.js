import { defineStore } from "pinia";
import { ref } from "vue";

import {
    answerInquiry,
    createInquiry,
    createReInquiry,
    deleteResidentInquiry,
    getAdminInquiries,
    getAdminInquiry,
    getResidentArchivedInquiries,
    getResidentInquiries,
    getResidentInquiry
} from "./inquiryApi";

export const useInquiryStore = defineStore("inquiry", () => {

    // 입주민 본인 문의 목록
    const residentInquiries = ref([]);

    // 관리자 문의 목록
    const adminInquiries = ref([]);

    // 현재 조회 중인 문의
    const inquiry = ref(null);

    // 관리자 목록의 현재 상태 조건
    const adminStatus = ref("WAITING");

    const loading = ref(false);
    const saving = ref(false);
    const deletingInquiryNo = ref(null);
    const errorMessage = ref("");

    // 서버 오류 메시지 추출
    const message = (error, fallback) =>
        error?.response?.data?.message
        || error?.response?.data?.detail
        || fallback;

    // 입주민 본인 문의 목록 조회
    const loadResidentInquiries = async () => {
        loading.value = true;
        errorMessage.value = "";

        try {
            const [currentResponse, archivedResponse] = await Promise.all([
                getResidentInquiries(),
                getResidentArchivedInquiries()
            ]);

            const currentInquiries = Array.isArray(currentResponse.data)
                ? currentResponse.data
                : [];
            const archivedInquiries = Array.isArray(archivedResponse.data)
                ? archivedResponse.data
                : [];

            residentInquiries.value = [
                ...currentInquiries,
                ...archivedInquiries
            ].sort(
                (left, right) =>
                    new Date(right.createdAt).getTime()
                    - new Date(left.createdAt).getTime()
            );

            return residentInquiries.value;
        } catch (error) {
            errorMessage.value = message(
                error,
                "문의 목록을 불러오지 못했습니다."
            );

            throw error;
        } finally {
            loading.value = false;
        }
    };

    // 입주민 본인 문의 상세 조회
    const loadResidentInquiry = async (inquiryNo) => {
        loading.value = true;
        errorMessage.value = "";

        try {
            const response =
                await getResidentInquiry(inquiryNo);

            inquiry.value = response.data;

            return inquiry.value;
        } catch (error) {
            inquiry.value = null;

            errorMessage.value = message(
                error,
                "문의사항을 불러오지 못했습니다."
            );

            throw error;
        } finally {
            loading.value = false;
        }
    };

    // 입주민 문의 등록
    const addInquiry = async (data) => {
        saving.value = true;
        errorMessage.value = "";

        try {
            const response = await createInquiry(data);

            return response.data;
        } catch (error) {
            errorMessage.value = message(
                error,
                "문의사항을 등록하지 못했습니다."
            );

            throw error;
        } finally {
            saving.value = false;
        }
    };

    // 입주민 재문의 등록
    const addReInquiry = async (
        inquiryNo,
        content
    ) => {
        saving.value = true;
        errorMessage.value = "";

        try {
            const response = await createReInquiry(
                inquiryNo,
                content
            );

            return response.data;
        } catch (error) {
            errorMessage.value = message(
                error,
                "재문의를 등록하지 못했습니다."
            );

            throw error;
        } finally {
            saving.value = false;
        }
    };

    // 자동 보관된 문의 상세 조회
    // 목록 API에 상세 내용이 포함되므로 별도 상세 API 없이 해당 항목을 사용한다.
    const loadArchivedInquiry = async (trashNo) => {
        loading.value = true;
        errorMessage.value = "";

        try {
            const response = await getResidentArchivedInquiries();
            const archivedInquiries = Array.isArray(response.data)
                ? response.data
                : [];

            inquiry.value = archivedInquiries.find(
                (item) => item.trashNo === trashNo
            ) ?? null;

            if (!inquiry.value) {
                throw new Error("보관된 문의를 찾을 수 없습니다.");
            }

            return inquiry.value;
        } catch (error) {
            inquiry.value = null;
            errorMessage.value = message(
                error,
                error.message || "문의사항을 불러오지 못했습니다."
            );

            throw error;
        } finally {
            loading.value = false;
        }
    };

    // 입주민 본인 문의 삭제
    const removeResidentInquiry = async (inquiryNo) => {
        deletingInquiryNo.value = inquiryNo;
        errorMessage.value = "";

        try {
            const response = await deleteResidentInquiry(inquiryNo);

            residentInquiries.value = residentInquiries.value.filter(
                (item) => item.inquiryNo !== inquiryNo
            );

            return response.data;
        } catch (error) {
            errorMessage.value = message(
                error,
                "문의사항을 삭제하지 못했습니다."
            );

            throw error;
        } finally {
            deletingInquiryNo.value = null;
        }
    };

    // 관리자 상태별 문의 목록 조회
    const loadAdminInquiries = async (
        status = "WAITING"
    ) => {
        loading.value = true;
        errorMessage.value = "";

        adminStatus.value = status;

        try {
            const response =
                await getAdminInquiries(status);

            adminInquiries.value =
                Array.isArray(response.data)
                    ? response.data
                    : [];

            return adminInquiries.value;
        } catch (error) {
            errorMessage.value = message(
                error,
                "문의 목록을 불러오지 못했습니다."
            );

            throw error;
        } finally {
            loading.value = false;
        }
    };

    // 관리자 문의 상세 조회
    const loadAdminInquiry = async (inquiryNo) => {
        loading.value = true;
        errorMessage.value = "";

        try {
            const response =
                await getAdminInquiry(inquiryNo);

            inquiry.value = response.data;

            return inquiry.value;
        } catch (error) {
            inquiry.value = null;

            errorMessage.value = message(
                error,
                "문의사항을 불러오지 못했습니다."
            );

            throw error;
        } finally {
            loading.value = false;
        }
    };

    // 관리자 답변 등록
    const submitAnswer = async (
        inquiryNo,
        answerContent
    ) => {
        saving.value = true;
        errorMessage.value = "";

        try {
            const response = await answerInquiry(
                inquiryNo,
                answerContent
            );

            // 답변이 반영된 상세 내용 다시 조회
            await loadAdminInquiry(inquiryNo);

            return response.data;
        } catch (error) {
            errorMessage.value = message(
                error,
                "답변을 등록하지 못했습니다."
            );

            throw error;
        } finally {
            saving.value = false;
        }
    };

    return {
        residentInquiries,
        adminInquiries,
        inquiry,
        adminStatus,
        loading,
        saving,
        deletingInquiryNo,
        errorMessage,

        loadResidentInquiries,
        loadResidentInquiry,
        loadArchivedInquiry,
        addInquiry,
        addReInquiry,
        removeResidentInquiry,
        loadAdminInquiries,
        loadAdminInquiry,
        submitAnswer
    };
});
