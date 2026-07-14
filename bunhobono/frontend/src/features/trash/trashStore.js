import { defineStore } from "pinia";
import { ref } from "vue";
import { getTrashDetail, getTrashList } from "./trashApi";

export const useTrashStore = defineStore("trash", () => {

    const trashList = ref([]);
    const trashDetail = ref(null);
    const selectedDataType = ref("");

    // 휴지통 목록 조회
    const loadTrashList = async () => {
        const response = await getTrashList(selectedDataType.value);

        trashList.value = Array.isArray(response.data)
            ? response.data
            : [];
    };

    // 휴지통 상세 조회
    const loadTrashDetail = async (trashNo) => {
        const response = await getTrashDetail(trashNo);

        trashDetail.value = response.data ?? null;
    };

    // 데이터 종류 변경
    const changeDataType = async (dataType) => {
        selectedDataType.value = dataType;

        await loadTrashList();
    };

    

    return {
        trashList,
        trashDetail,
        selectedDataType,

        loadTrashList,
        loadTrashDetail,
        changeDataType,
    };
});