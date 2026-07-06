import api from "@/shared/api/apiClient";

// 회원 전체 조회
export const getCameraList = () => {
    return api.get("/cameras");
};
