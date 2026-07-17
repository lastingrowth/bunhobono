import api from "@/shared/api/apiClient";

// 게이트 목록
export const getList = () => {
    return api.get("/gates");
};

// 게이트 등록
export const signUpGate = (data) => {
  return api.post("/gates/signUp", data);
};

// 게이트 수정
export const updateGate = (gateNo, data) => {
  return api.put(`/gates/${gateNo}/edit`, data);
};

// 게이트 삭제
export const deleteGate = (gateNo) => {
  return api.delete(`/gates/${gateNo}/delete`);
};

// 게이트 상태 변경
export const updateGateStatus = (gateNo, gateStatus) => {
  return api.put(`/gates/${gateNo}/status`, {gateStatus});
};

// 게이트 열기
// 백엔드에서 게이트를 열고 5초 뒤 자동 닫힘을 예약한다
export const openGate = (gateNo) => {
  return api.put(`/gates/${gateNo}/open`);
};
