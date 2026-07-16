import api from "./apiClient";

// 로그인한 입주민의 회원, 차량, 최근 입출차 정보를 조회
export const getResidentDashboard = () => {
    return api.get("/resident/mypage/dashboard");
};

