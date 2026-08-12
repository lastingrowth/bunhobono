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

// 관리자가 저신뢰 OCR 차량번호를 확인하거나 수정한 뒤
// 기존 승인 차량을 다시 조회하여 입출차 처리와 게이트 개방을 요청한다.
export const confirmLowConfidenceGate = (cameraDataNo, carNo) => {
  return api.post(
    `/camera-data/${cameraDataNo}/confirm-gate`,
    { carNo },
  )
}

// 일반 미등록 차량을 관리실 방문차량으로 등록하고 게이트를 연다.
// 해당 차량은 B2 입차 시 무료시간을 적용하지 않는다.
export const openVisitGateByCameraData = (cameraDataNo) => {
  return api.post(
    `/camera-data/${cameraDataNo}/open-visit-gate`,
  )
}

// 미등록 긴급차량을 관리실 방문차량으로 등록하고 게이트를 연다.
// 해당 차량은 B2 입차 시 72시간 무료시간을 적용한다.
export const openEmergencyGateByCameraData = (cameraDataNo) => {
  return api.post(
    `/camera-data/${cameraDataNo}/open-emergency-gate`,
  )
}
