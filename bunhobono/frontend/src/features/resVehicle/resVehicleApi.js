import api from "@/shared/api/apiClient";

// 로그인한 입주민 정보 조회
export const getResVehicleMemberInfo = () => {
    return api.get("/res/vehicles/me");
};

// 내 차량 목록 조회
export const getResVehicleList = () => {
    return api.get("/res/vehicles");
};

// 내 차량 상세 조회
export const getResVehicleDetail = (vehicleNo) => {
    return api.get(`/res/vehicles/${vehicleNo}`);
};

// 차량 등록 신청
export const createResVehicle = (data) => {
    return api.post("/res/vehicles/insert", data);
};

// 차량 수정 신청
export const updateResVehicle = (vehicleNo, data) => {
    return api.put(`/res/vehicles/${vehicleNo}/edit`, data);
};

// 차량 삭제
export const deleteResVehicle = (vehicleNo) => {
    return api.delete(`/res/vehicles/${vehicleNo}/delete`);
};

