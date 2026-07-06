import { defineStore } from "pinia";
import { ref } from "vue";
import { getCameraDataDetail, getCameraDataList, searchCameraDataByCarNo } from "./cameraDataApi";
export const useCameraDataStore =  defineStore("camera-data", () => {

  const list = ref ([]);
  const searchResults = ref([]);
  const detail = ref(null);

  // 전체 조회
  const loadList = async () => {
    const res = await getCameraDataList();
    list.value = res.data;
  };

  // 차량번호 검색
  const searchByCarNo = async (carNo) => {
    const res = await searchCameraDataByCarNo(carNo);
    searchResults.value = res.data;
  };

  // 디테일 조회
  const loadDetail = async (cameraDataNo) => {
    const res = await getCameraDataDetail(cameraDataNo);
    detail.value = res.data;
  };

  return {
    loadList,
    list,
    searchByCarNo,
    searchResults,
    loadDetail,
    detail
  };

});