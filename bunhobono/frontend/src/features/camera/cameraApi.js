import api from "@/shared/api/apiClient";

// 카메라 목록
export const getCameraList = () => {
    return api.get("/cameras");
};

// 카메라 등록
export const signupCamera = (data) => {
    return api.post("/cameras/signUp",data);
};

// 카메라 수정
export const updateCamera = (cameraNo, data) => {
    return api.put(`/cameras/${cameraNo}/edit`, data);
};

// 카메라 삭제
export const deleteCamera = (cameraNo) => {
    return api.delete(`/cameras/${cameraNo}/delete`);
};


