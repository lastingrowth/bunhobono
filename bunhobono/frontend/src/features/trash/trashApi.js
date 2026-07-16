import api from "@/shared/api/apiClient";

// 휴지통 목록
export const getTrashList = (dataType) => {
    return api.get("/trash", {
        params: {
            dataType: dataType || undefined,
        },
    });
};

export const searchTrashByCarNo = (carNo) => {
    return api.get("/trash/search", {
        params: {
            carNo,
        },
    });
};

// 휴지통 상세
export const getTrashDetail = (trashNo) => {
    return api.get(`/trash/${trashNo}`);
};
