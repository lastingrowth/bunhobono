import api from "@/shared/api/apiClient"

// 차량 입출차 목록 조회
export const getCarLogs = (params) => {
    return api.get("/carlog", {
        params,
    });
};

export const deleteCarLog = (carLogNo) => {
    return api.delete(`/carlog/${carLogNo}/delete`);
};