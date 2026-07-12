import api from "@/shared/api/apiClient";

// 게이트 목록
export const getList = () => {
    return api.get("/gates");
};

// 게이트 등록
export const signUpGate = (data) => {
  return api.post("/gates/signUp", data);
};

// 게이트 상세 조회
export const getGateDetail = (gateNo) => {
  return api.get(`/gates/${gateNo}/detail`);
};

// 게이트 수정
export const updateGate = (gateNo, data) => {
  return api.put(`/gates/${gateNo}/edit`, data);
};

// 게이트 삭제
export const deleteGate = (gateNo) => {
  return api.delete(`/gates/${gateNo}/delete`);
};