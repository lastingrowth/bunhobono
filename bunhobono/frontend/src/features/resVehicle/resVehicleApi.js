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

// 로그인한 입주민 세대의 이번 달 방문차량 등록 현황
export const getMonthlyVisitRegistration = () => {
    return api.get("/vehicles/resident/visit/monthly-registration");
};

// 입차 전 방문차량 등록 취소
export const cancelResVisitVehicle = (vehicleCarNo) => {
    return api.delete(`/vehicles/resident/visit/${vehicleCarNo}`);
};

// 입차 전 방문차량 방문시간 수정
export const updateResVisitVehicleTime = (vehicleCarNo, data) => {
    return api.patch(`/vehicles/resident/visit/${vehicleCarNo}/time`, data);
};

// 입차 중인 방문차량 1일 연장
export const extendResVisitVehicleOneDay = (vehicleCarNo) => {
    return api.patch(`/vehicles/resident/visit/${vehicleCarNo}/extend-one-day`);
};

// 본인 일반차량 등록기간 연장
export const extendResNormalVehicle = (vehicleCarNo, endDate) => {
    return api.patch(`/vehicles/resident/normal/${vehicleCarNo}/end-date`, {
        endDate
    });
};

// 로그인한 입주민의 알림 목록
export const getResVehicleNotifications = () => {
    return api.get("/mem-notices/resident");
};

// 선택한 알림 한 건 읽음 처리
export const markResVehicleNotificationRead = (memNoticeNo) => {
    return api.patch(`/mem-notices/resident/${memNoticeNo}/read`);
};

export const deleteResVehicleNotification = (memNoticeNo) => {
    return api.delete(`/mem-notices/resident/${memNoticeNo}`);
};
