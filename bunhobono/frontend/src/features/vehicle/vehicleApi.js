import api from "@/shared/api/apiClient";

// 차량 목록
export const getVehicleList = () => {
    return api.get("/vehicles");
};

// 차량 등록
export const createVehicle = (data) => {
    return api.post("/vehicles/signUp", data);
};

// 차량 수정
export const updateVehicle = (vehicleCarNo, data) => {
    return api.put(`/vehicles/${vehicleCarNo}/edit`, data);
};

// 차량 삭제
export const deleteVehicle = (vehicleCarNo) => {
    return api.delete(`/vehicles/${vehicleCarNo}/delete`);
};

// 차량 승인 상태 변경
export const updateVehicleStatus = (vehicleCarNo, data) => {
    return api.patch(`/vehicles/${vehicleCarNo}/status`, data);
};
