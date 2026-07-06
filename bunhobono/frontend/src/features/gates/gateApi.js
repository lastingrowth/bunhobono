import api from "@/shared/api/apiClient";

// 회원 전체 조회
export const getList = () => {
    return api.get("/gates");
};

// 게이트 등록
export const signUpGate = (gateData) => {
  return api.post("/gates/signUp", gateData);
};

// 게이트 상세 조회
export const getGateDetail = (gateNo) => {
  return api.get(`/gates/${gateNo}/detail`);
};

//  게이트 삭제
export const deleteGate = (gateNo) => {
  return api.delete(`/gates/${gateNo}/delete`);
};

// 게이트 수정
export const updateGate = (gateNo, data) => {
  return api.put(`/gates/${gateNo}/edit`, data);
};