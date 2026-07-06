import api from "@/shared/api/apiClient";

// 회원 전체 조회
export const getParkingsList = () => {
    return api.get("/parkings");
};

// 회원 등록
export const signupParkings = (data) => {
    return api.post("/parkings/signUp", data);
};

//삭제
export function deleteParking(parkingNo) {
  return api.delete(`/parkings/${parkingNo}/delete`);
}

//수정
export function updateParking(parkingNo, data) {
  return api.put(`/parkings/${parkingNo}/edit`, data);
}
