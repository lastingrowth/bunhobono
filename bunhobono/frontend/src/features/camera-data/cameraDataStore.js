import { defineStore } from "pinia";
import { computed, ref } from "vue";
import { getCameraDataDetail, getCameraDataList, searchCameraDataByCarNo } from "./cameraDataApi";
import { useCameraStore } from "../camera/cameraStore";
import { useGateStore } from "../gates/gateStore";

export const useCameraDataStore =  defineStore("camera-data", () => {

  const cameraStore = useCameraStore();
  const gateStore = useGateStore();


  const list = ref ([]);
  const searchResults = ref([]);
  const detail = ref(null);
  const searchMode = ref(false);

  // 카메라 번호로 연결된 게이트 타입 확인
  const getMovementType = (cameraData) => {
    // 카메라 데이터와 같은 카메라 번호 검색
    const camera = cameraStore.list.find((item) => {
      return Number(item.cameraNo) === Number(cameraData.cameraNo);
    });

    if(!camera) {
      return "UNKNOWN";
    }

    // 카메라에 연결된 게이트 검색
    const gate = gateStore.list.find((item) => {
      return Number(item.gateNo) === Number(camera.gateNo);
    });

    if(!gate || !gate.gateType) {
      return "UNKNOWN";
    }

    const gateType = String(gate.gateType).toUpperCase();

    if (gateType === "IN") {
      return "IN";
    }

    if (gateType === "OUT") {
      return "OUT";
    }

    return "UNKNOWN";
  };

  // 입출차 값을 화면용 한글로 변환
  const getMovementTypeText = (movementType) => {
    if (movementType === "IN") {
      return "입차";
    }

    if (movementType === "OUT") {
      return "출차";
    }

    return "확인 불가";
  }

  // 카메라 데이터에 입출차 구분값 추가
  const addMovementType = (cameraData) => {
    const movementType = getMovementType(cameraData);

    return {
      ...cameraData,
      movementType,
      movementTypeText : getMovementTypeText(movementType)
    };
  };

  // 전체 목록 또는 검색 결과에 입출차 구분값 추가
  const displayList = computed(() => {
    const source = searchMode.value ? searchResults.value : list.value;

    return source.map((cameraData) => {
      return addMovementType(cameraData);
    });
  });

  // 카메라 데이터 전체 조회
  const loadList = async () => {
  
    const res = await getCameraDataList();
    list.value = res.data;

    // 카메라와 게이트 연결 정보를 조회
    await cameraStore.loadList();
    await gateStore.loadList();

    // 전체 목록을 표시하도록 검색 상태 초기화
    searchResults.value = [];
    searchMode.value = false;
  };

  // 차량번호 검색
  const searchByCarNo = async (carNo) => {
    const res = await searchCameraDataByCarNo(carNo);

    searchResults.value = res.data;
    searchMode.value = true;
  };

  // 상세 조회
  const loadDetail = async (cameraDataNo) => {
    const res = await getCameraDataDetail(cameraDataNo);
    detail.value = res.data;
  };

  return {
    list,
    searchResults,
    detail,
    displayList,
    loadList,
    searchByCarNo,
    loadDetail,
  };

});