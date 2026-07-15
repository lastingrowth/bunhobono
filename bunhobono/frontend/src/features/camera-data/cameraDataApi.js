import api from "@/shared/api/apiClient";

// 회원 전체 조회
export const getCameraDataList = () => {
  return api.get("/camera-data", {
    params: {
      _t: Date.now()
    }
  });
};

// 차량번호 검색
export const searchCameraDataByCarNo = (carNo) => {
  return api.get(`/camera-data/search`, {
    params : {
      carNo: carNo
    }
  });
};

// 디테일 조회
export const getCameraDataDetail = (cameraDataNo) => {
  return api.get(`/camera-data/${cameraDataNo}/detail`, {
    params: {
      _t: Date.now()
    }
  });
};

//이미지 추가 
export const getCameraDataImage = (cameraDataNo) => {
  return api.get(`/camera-data/${cameraDataNo}/image`, {
    params: {
      _t: Date.now()
    },
    responseType: 'blob',
  })
}
