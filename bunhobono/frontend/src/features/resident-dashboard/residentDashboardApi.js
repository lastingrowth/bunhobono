import api from "@/shared/api/apiClient";

// 로그인한 입주민의 회원·차량·주차 현황을 한 번에 조회한다.
export const getResidentDashboard = () => {
    return api.get("/resident/dashboard");
};
