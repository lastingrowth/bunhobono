import api from "@/shared/api/apiClient";

// 로그인한 입주민 정보
export const getResVehicleMemberInfo = () => {
    return api.get("/resident/mypage");
};

// 전체 차량 목록
export const getResVehicleList = () => {
    return api.get("/resident/dashboard/vehicles");
};

// 입주민 차량 등록 신청
export const createResVehicle = (data) => {
    return api.post("/resident/dashboard/vehicles", data);
};

// 입주민 차량 수정
export const updateResVehicle = (vehicleCarNo, data) => {
    return api.put(`/resident/dashboard/vehicles/${vehicleCarNo}`, data);
};

// 입주민 차량 삭제
export const deleteResVehicle = (vehicleCarNo) => {
    return api.delete(`/resident/dashboard/vehicles/${vehicleCarNo}`);
};
