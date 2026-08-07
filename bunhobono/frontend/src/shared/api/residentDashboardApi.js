import api from "./apiClient";

// 로그인한 입주민의 회원, 차량, 최근 입출차 정보를 조회
export const getResidentDashboard = () => {
    return api.get("/resident/mypage/dashboard");
};

// 로그인한 입주민 세대의 이번 달 방문차량 등록 현황
export const getMonthlyVisitRegistration = () => {
    return api.get("/vehicles/resident/visit/monthly-registration");
};

// 부산 현재 날씨 조회
export const getTodayWeather = () => {
    return api.get("/weather/today");
};
