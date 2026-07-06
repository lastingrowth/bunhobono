import api from "@/shared/api/apiClient";

// 전체 조회
export const getCameraList = () => {
    return api.get("/cameras");
};

// 카메라 등록
export const signupCamera = (data) => {
  return api.post("/cameras/signUp", data);
};

//삭제
export function deleteCamera(cameraNo) {
  return api.delete(`/cameras/${cameraNo}/delete`);
}

//수정
export function updateCamera(cameraNo, data) {
  return api.put(`/cameras/${cameraNo}/edit`, data);
}
