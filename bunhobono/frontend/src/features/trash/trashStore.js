import { defineStore } from "pinia";
import { ref } from "vue";
import {
    getTrashDetail,
    getTrashList,
    restoreTrash,
    searchTrashByCarNo,
} from "./trashApi";

export const useTrashStore = defineStore("trash", () => {

    const trashList = ref([]);
    const trashDetail = ref(null);
    const selectedDataType = ref("");
    const searchCarNo = ref("");

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
        searchCarNo.value = "";

        await loadTrashList();
    };

    const searchByCarNo = async () => {
        const carNo = searchCarNo.value.trim();

        if (!carNo) {
            await loadTrashList();
            return;
        }

        const response = await searchTrashByCarNo(carNo);
        trashList.value = Array.isArray(response.data)
            ? response.data
            : [];
    };

    const resetTrashList = async () => {
        searchCarNo.value = "";
        selectedDataType.value = "";

        await loadTrashList();
    };

    const restoreTrashItem = async (trashNo) => {
        const response = await restoreTrash(trashNo);

        if (response.data?.success) {
            trashList.value = trashList.value.filter(
                (item) => item.trashNo !== trashNo
            );

            if (trashDetail.value?.trashNo === trashNo) {
                trashDetail.value = null;
            }
        }

        return response.data;
    };

    

    return {
        trashList,
        trashDetail,
        selectedDataType,
        searchCarNo,

        loadTrashList,
        loadTrashDetail,
        changeDataType,
        searchByCarNo,
        resetTrashList,
        restoreTrashItem,
    };
});
