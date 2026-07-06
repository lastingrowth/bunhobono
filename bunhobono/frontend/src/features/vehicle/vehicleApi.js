import api from "@/shared/api/apiClient"

// 차량 목록
export const getVehicleList = () => {
    return api.get("/vehicles");
};

// 차량 검색 (vehicleNo)
export const getVehicleByNo = (vehicleNo) => {
    return api.get(`/api/vehicles/${vehicleNo}`);
};

// 차량 상세 
export const getVehicleDetail = (vehicleNo) => {
    return api.get(`api/vehicles/${vehicleNo}/detail`);
};

// 차량 등록
export const createVehicle = (data) => {
    return api.post("/api/vehicles/insert", data);
};

// 차량 수정
export const updateVehicle = (vehicleNo, data) => {
    return api.put(`/api/vehicles/${vehicleNo}/edit`, data);
};

// 차량 삭제
export const deleteVehicle = (vehicleNo) => {
    return api.delete(`/api/vehicles/${vehicleNo}/delete`);
};


// 승인 대기 목록
export const getVehicleApproveList = () => {
    return api.get("/api/vehicles/approve");
};

// 승인 대기 상세
export const getVehicleApproveDetail = (vehicleNo) => {
    return api.get(`/api/vehicles/approve/${vehicleNo}`);
};

// 승인 상태 처리
export const updateVehicleApproveStatus = (vehicleNo, data) => {
    return api.put(`/api/vehicles/approve/${vehicleNo}/status`, data);
};