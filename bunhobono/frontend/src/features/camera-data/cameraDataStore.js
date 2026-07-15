import { defineStore } from "pinia";
import { computed, ref } from "vue";
import { deleteCameraData, getCameraDataDetail, getCameraDataList, searchCameraDataByCarNo } from "./cameraDataApi";
import { useCameraStore } from "../camera/cameraStore";
import { useGateStore } from "../gates/gateStore";

export const useCameraDataStore =  defineStore("camera-data", () => {

  const cameraStore = useCameraStore();
  const gateStore = useGateStore();


  const list = ref ([]);
  const searchResults = ref([]);
  const detail = ref(null);
  const detailMap = ref({});
  const searchMode = ref(false);

  const getField = (source, camelKey, snakeKey) => {
    return source?.[camelKey] ?? source?.[snakeKey];
  };

  const getCameraNo = (cameraData) => {
    return getField(cameraData, "cameraNo", "camera_no");
  };

  const getCameraGateNo = (camera) => {
    return getField(camera, "gateNo", "gate_no");
  };

  const getGateNo = (gate) => {
    return getField(gate, "gateNo", "gate_no");
  };

  const getGateParkingNo = (gate) => {
    return getField(gate, "parkingNo", "parking_no");
  };

  const getGateParkingName = (gate) => {
    return getField(gate, "parkingName", "parking_name");
  };

  const findCamera = (cameraData) => {
    const cameraNo = getCameraNo(cameraData);

    return cameraStore.list.find((item) => {
      return Number(getField(item, "cameraNo", "camera_no")) === Number(cameraNo);
    });
  };

  const findGateByCamera = (camera) => {
    const gateNo = getCameraGateNo(camera);

    return gateStore.list.find((item) => {
      return Number(getGateNo(item)) === Number(gateNo);
    });
  };

  const ensureRelationLists = async () => {
    await Promise.all([
      cameraStore.loadList(),
      gateStore.loadList()
    ]);
  };

  // 카메라 번호로 연결된 게이트 타입 확인
  const getMovementType = (cameraData) => {
    // 카메라 데이터와 같은 카메라 번호 검색
    const camera = findCamera(cameraData);

    if(!camera) {
      return "UNKNOWN";
    }

    // 카메라에 연결된 게이트 검색
    const gate = findGateByCamera(camera);

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

  const getCameraParking = (cameraData) => {
    const camera = findCamera(cameraData);
    const gate = findGateByCamera(camera);

    return {
      parkingNo: getGateParkingNo(gate) ?? null,
      parkingName: getGateParkingName(gate) ?? "-"
    };
  };

  // 카메라 데이터에 입출차 구분값과 주차장 정보를 추가
  const addMovementType = (cameraData) => {
    const movementType = getMovementType(cameraData);
    const parking = getCameraParking(cameraData);

    return {
      ...cameraData,
      ...parking,
      movementType,
      movementTypeText : getMovementTypeText(movementType)
    };
  };

  const addDetailInfo = (cameraData) => {
    const detailInfo = detailMap.value[cameraData.cameraDataNo] ?? {};

    return {
      ...cameraData,
      confidenceScore: cameraData.confidenceScore ?? detailInfo.confidenceScore,
      recognitionState: cameraData.recognitionState ?? detailInfo.recognitionState
    };
  };

  const loadDetailMap = async (source) => {
    const targets = source.filter((cameraData) => {
      return cameraData.cameraDataNo
        && detailMap.value[cameraData.cameraDataNo] === undefined;
    });

    const details = await Promise.all(
      targets.map(async (cameraData) => {
        try {
          const res = await getCameraDataDetail(cameraData.cameraDataNo);
          return [cameraData.cameraDataNo, res.data ?? {}];
        } catch (error) {
          console.error("카메라 데이터 상세 조회 실패", error);
          return [cameraData.cameraDataNo, {}];
        }
      })
    );

    detailMap.value = {
      ...detailMap.value,
      ...Object.fromEntries(details)
    };
  };

  // 전체 목록 또는 검색 결과에 상세 인식률, 입출차 구분값 추가
  const displayList = computed(() => {
    const source = searchMode.value ? searchResults.value : list.value;

    return source.map((cameraData) => {
      return addMovementType(addDetailInfo(cameraData));
    });
  });

  // 카메라 데이터 전체 조회
  const loadList = async () => {
    // cameraData를 주차장별로 정확히 분류하기 위해 연결 정보를 먼저 조회
    await ensureRelationLists();

    const res = await getCameraDataList();
    list.value = res.data;
    await loadDetailMap(list.value);

    // 전체 목록을 표시하도록 검색 상태 초기화
    searchResults.value = [];
    searchMode.value = false;
  };

  // 차량번호 검색
  const searchByCarNo = async (carNo) => {
    await ensureRelationLists();

    const res = await searchCameraDataByCarNo(carNo);

    searchResults.value = res.data;
    await loadDetailMap(searchResults.value);
    searchMode.value = true;
  };

  // 상세 조회
  const loadDetail = async (cameraDataNo) => {
    const res = await getCameraDataDetail(cameraDataNo);
    detail.value = res.data;
  };

  // 카메라 데이터 삭제
  const remove = async (cameraDataNo) => {
    const result = confirm("카메라 데이터를 삭제하시겠습니까?");

    if (!result) {
      return;
    }

    const response = await deleteCameraData(cameraDataNo);

    if (response.data === 1) {
      list.value = list.value.filter((item) => {
        return Number(item.cameraDataNo ?? item.camera_data_no) !== Number(cameraDataNo);
      });
      searchResults.value = searchResults.value.filter((item) => {
        return Number(item.cameraDataNo ?? item.camera_data_no) !== Number(cameraDataNo);
      });

      delete detailMap.value[cameraDataNo];

      alert("카메라 데이터 삭제 완료");
    } else {
      alert("카메라 데이터 삭제 실패");
    }
  };

  return {
    list,
    searchResults,
    detail,
    detailMap,
    displayList,
    loadList,
    searchByCarNo,
    loadDetail,
    remove,
  };

});