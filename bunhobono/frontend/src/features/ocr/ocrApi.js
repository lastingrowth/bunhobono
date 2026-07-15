import axios from "axios";

// 선택한 사진과 카메라 번호를 FastAPI에 전달
export const uploadOcrImage = (file, cameraNo) => {
    const formData = new FormData();

    formData.append("file", file);
    formData.append("cameraNo", cameraNo);

    return axios.post("/fastapi/ocr", formData);
};