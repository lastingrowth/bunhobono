import api from "@/shared/api/apiClient";

// 카메라 기록 서버 페이징 조회
export const getCameraDataList = ({
  page = 1,
  size = 10,
  keyword = "",
  parkingNo = null,
} = {}) => {
  return api.get("/camera-data", {
    params: {
      page,
      size,
      keyword: keyword || undefined,
      parkingNo: parkingNo || undefined,
      _t: Date.now(),
    },
  });
};

// 기존 호출부 호환용 차량번호 검색
export const searchCameraDataByCarNo = (carNo, page = 1, size = 10, parkingNo = null) => {
  return getCameraDataList({ page, size, keyword: carNo, parkingNo });
};

// 디테일 조회
export const getCameraDataDetail = (cameraDataNo) => {
  return api.get(`/camera-data/${cameraDataNo}/detail`, {
    params: {
      _t: Date.now()
    }
  });
};

export const editCameraDataCarNo = (cameraDataNo, carNo, saveAlias = true) => {
  return api.patch(`/camera-data/${cameraDataNo}/edit`, {
    carNo,
    saveAlias,
  });
};

export const editCameraDataNote = (cameraDataNo, camNote) => {
  return api.patch(`/camera-data/${cameraDataNo}/note`, {
    camNote,
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

export const deleteCameraData = (cameraDataNo) => {
  return api.delete(`/camera-data/${cameraDataNo}/delete`);
};

// 관리자 수동 게이트 열기
// 자동 통과되지 않은 OCR 데이터를 관리자가 확인한 뒤 게이트를 열 때 사용
export const openGateByCameraData = (cameraDataNo) => {
  return api.post(`/camera-data/${cameraDataNo}/open-gate`)
}
