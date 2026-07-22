import api from "@/shared/api/apiClient";

// 로그인한 입주민 정보
export const getResVehicleMemberInfo = () => {
    return api.get("/resident/mypage");
};

// 입주민 본인 차량 목록
export const getResVehicleList = () => {
    return api.get("/vehicles/resident");
};

// 입주민 방문차량 등록 신청
export const createResVehicle = (data) => {
    return api.post("/vehicles/resident/visit", data);
};

// 로그인한 입주민의 차량 알림 목록
export const getResVehicleNotifications = () => {
    return api.get("/vehicle-nt/resident");
};

// 로그인한 입주민의 읽지 않은 차량 알림 전체 읽음 처리
export const markResVehicleNotificationsRead = () => {
    return api.patch("/vehicle-nt/resident/read");
};