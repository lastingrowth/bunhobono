import api from "@/shared/api/apiClient";

// 로그인한 입주민 정보
export const getResVehicleMemberInfo = () => {
    return api.get("/resident/mypage");
};

// 입주민 본인 차량 목록
// 백엔드: GET /api/vehicles/resident
export const getResVehicleList = () => {
    return api.get("/vehicles/resident");
};

// 입주민 방문차량 등록 신청
// 백엔드: POST /api/vehicles/resident/visit
export const createResVehicle = (data) => {
    return api.post("/vehicles/resident/visit", data);
};

// 현재 백 구조에서는 입주민 차량 수정/삭제는 우선 사용하지 않는다.
// normal 차량은 조회만 가능하고, visit 차량 신청도 승인대기 구조이기 때문.
// 필요해지면 vehicle_p에 resident 전용 수정/삭제 API를 따로 추가해야 한다.