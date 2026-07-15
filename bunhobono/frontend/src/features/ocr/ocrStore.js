import { defineStore } from "pinia";
import { ref } from "vue";
import { uploadOcrImage } from "./ocrApi";

export const useOcrStore = defineStore("ocr", () => {

    const loading = ref(false);
    const result = ref(null);
    const errorMessage = ref("");

    // 선택한 사진을 FastAPI에 전달하고 OCR 결과 저장
    const uploadImage = async (file, cameraNo) => {
        loading.value = true;
        result.value = null;
        errorMessage.value = "";

        try {
            const res = await uploadOcrImage(file, cameraNo);

            // 임시 로그
            console.log("FastAPI 응답:", res.data);
            console.log("Spring 상태:", res.data.spring_status);
            console.log("Spring 결과:", res.data.spring_result);

            result.value = res.data;

            return res.data;
        } catch (e) {
            console.error(e);

            errorMessage.value = "사진 분석 중 오류가 발생했습니다.";

            return null;
        } finally {
            loading.value = false;
        }
    };

    return {
        loading,
        result,
        errorMessage,
        uploadImage
    };
});