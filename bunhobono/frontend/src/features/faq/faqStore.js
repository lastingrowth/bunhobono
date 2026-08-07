import { defineStore } from "pinia";
import { ref } from "vue";
import { createFaq, deleteFaq, getFaqs, updateFaq } from "./faqApi";

export const useFaqStore = defineStore("faq", () => {

    // 자주하는 질문 목록
    const faqs = ref([]);

    const loading = ref(false);
    const saving = ref(false);
    const errorMessage = ref("");

    // 서버 오류 메시지 추출
    const message = (error, fallback) =>
        error?.response?.data?.message
        || error?.response?.data?.detail
        || fallback;

    // 자주하는 질문 목록 조회
    const loadFaqs = async () => {
        loading.value = true;
        errorMessage.value = "";

        try {
            const response = await getFaqs();

            faqs.value =
                Array.isArray(response.data)
                    ? response.data
                    : [];

            return faqs.value;
        } catch (error) {
            errorMessage.value = message(
                error,
                "자주하는 질문 목록을 불러오지 못했습니다."
            );

            throw error;
        } finally {
            loading.value = false;
        }
    };

    // 관리자 자주하는 질문 등록
    const addFaq = async (data) => {
        saving.value = true;
        errorMessage.value = "";

        try {
            const response = await createFaq(data);

            await loadFaqs();

            return response.data;
        } catch (error) {
            errorMessage.value = message(
                error,
                "자주하는 질문을 등록하지 못했습니다."
            );

            throw error;
        } finally {
            saving.value = false;
        }
    };

    // 관리자 자주하는 질문 수정
    const editFaq = async (faqNo, data) => {
        saving.value = true;
        errorMessage.value = "";

        try {
            const response = await updateFaq(
                faqNo,
                data
            );

            await loadFaqs();

            return response.data;
        } catch (error) {
            errorMessage.value = message(
                error,
                "자주하는 질문을 수정하지 못했습니다."
            );

            throw error;
        } finally {
            saving.value = false;
        }
    };

    // 관리자 자주하는 질문 삭제
    const removeFaq = async (faqNo) => {
        saving.value = true;
        errorMessage.value = "";

        try {
            const response = await deleteFaq(faqNo);

            await loadFaqs();

            return response.data;
        } catch (error) {
            errorMessage.value = message(
                error,
                "자주하는 질문을 삭제하지 못했습니다."
            );

            throw error;
        } finally {
            saving.value = false;
        }
    };

    return {
        faqs,
        loading,
        saving,
        errorMessage,

        loadFaqs,
        addFaq,
        editFaq,
        removeFaq
    };
});