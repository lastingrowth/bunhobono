import api from "@/shared/api/apiClient";

// 휴지통 목록
export const getTrashList = (dataType) => {
    return api.get("/trash", {
        params: {
            dataType: dataType || undefined,
        },
    });
};

// 휴지통 상세
export const getTrashDetail = (trashNo) => {
    return api.get(`/trash/${trashNo}`);
};