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
  const carLogs = ref([]);
  const searchMode = ref(false);
  const currentPage = ref(1);
  const pageSize = ref(10);
  const totalPages = ref(1);
  const totalCount = ref(0);
  let relationsLoaded = false;
  const feedbackMessage = ref("");
  const feedbackType = ref("success");
  let feedbackTimer;

  const showFeedback = (message, type = "success") => {
    feedbackMessage.value = message;
    feedbackType.value = type;
    window.clearTimeout(feedbackTimer);
    feedbackTimer = window.setTimeout(() => {
      feedbackMessage.value = "";
    }, 2500);
  };

  const getField = (source, camelKey, snakeKey) => {
    return source?.[camelKey] ?? source?.[snakeKey];
  };

  const getCameraNo = (cameraData) => {
    return getField(cameraData, "cameraNo", "camera_no");
  };

  const getCameraDataNo = (cameraData) => {
    return getField(cameraData, "cameraDataNo", "camera_data_no");
  };

  const getVehicleCarNo = (source) => {
    return getField(source, "vehicleCarNo", "vehicle_car_no");
  };

  const getCarNo = (source) => {
    return getField(source, "carNo", "car_no");
  };

  const getCaptureTime = (cameraData) => {
    return getField(cameraData, "captureTime", "capture_time");
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
    if (relationsLoaded) return;

    await Promise.all([
      cameraStore.loadList(),
      gateStore.loadList(),
    ]);
    relationsLoaded = true;
  };

  const toTime = (value) => {
    if (!value) {
      return null;
    }

    const time = new Date(value).getTime();

    return Number.isNaN(time) ? null : time;
  };

  const isSameCaptureTime = (captureTime, logTime) => {
    const capture = toTime(captureTime);
    const log = toTime(logTime);

    if (capture === null || log === null) {
      return false;
    }

    return Math.abs(capture - log) <= 60 * 1000;
  };

  const findRelatedCarLog = (cameraData) => {
    const cameraDataNo = getCameraDataNo(cameraData);
    const vehicleCarNo = getVehicleCarNo(cameraData);
    const carNo = getCarNo(cameraData);
    const captureTime = getCaptureTime(cameraData);

    const exactLog = carLogs.value.find((log) => {
      return Number(log.cameraDataNo) === Number(cameraDataNo);
    });

    if (exactLog) {
      return exactLog;
    }

    const camera = findCamera(cameraData);
    const gate = findGateByCamera(camera);
    const gateType = String(getField(gate, "gateType", "gate_type") ?? "").toUpperCase();

    // 입차는 car_log.camera_data_no가 정확히 같은 경우만 처리 완료로 인정한다.
    // 같은 차량의 반복 촬영을 기존 입차 로그에 연결하지 않는다.
    if (gateType === "IN") {
      return null;
    }

    return carLogs.value.find((log) => {
      const sameVehicle = vehicleCarNo
        && Number(log.vehicleCarNo) === Number(vehicleCarNo);

      const sameCarNo = carNo
        && log.carNo === carNo;

      if (!sameVehicle && !sameCarNo) {
        return false;
      }

      // 출차 CameraData는 car_log에 직접 FK가 없으므로 차량과 출차 시각으로 연결한다.
      return gateType === "OUT"
        && isSameCaptureTime(captureTime, log.outTime);
    });
  };

  const getBothGateMovementType = (cameraData) => {
    const relatedLog = findRelatedCarLog(cameraData);

    if (!relatedLog) {
      return "UNKNOWN";
    }

    const captureTime = getCaptureTime(cameraData);

    if (isSameCaptureTime(captureTime, relatedLog.outTime)) {
      return "OUT";
    }

    if (isSameCaptureTime(captureTime, relatedLog.inTime)) {
      return "IN";
    }

    if (Number(relatedLog.cameraDataNo) === Number(getCameraDataNo(cameraData))) {
      return "IN";
    }

    if (!relatedLog.outTime) {
      return "IN";
    }

    return "UNKNOWN";
  };

  // 카메라 번호로 연결된 게이트 타입과 car_log를 확인
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

    if (gateType === "BOTH") {
      return getBothGateMovementType(cameraData);
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
      parkingName: getGateParkingName(gate) ?? "-",
      gateNo: getGateNo(gate) ?? null,
      gateName: getField(gate, "gateName", "gate_name") ?? "-",
      gateType: getField(gate, "gateType", "gate_type") ?? null
    };
  };

  // 카메라 데이터에 입출차 구분값과 주차장 정보를 추가
  const addMovementType = (cameraData) => {
    const movementType = getMovementType(cameraData);
    const parking = getCameraParking(cameraData);
    const relatedCarLog = findRelatedCarLog(cameraData);
    const processedMovementType = relatedCarLog
      ? (Number(relatedCarLog.cameraDataNo) === Number(getCameraDataNo(cameraData))
          ? "IN"
          : isSameCaptureTime(getCaptureTime(cameraData), relatedCarLog.outTime)
            ? "OUT"
            : null)
      : null;

    return {
      ...cameraData,
      ...parking,
      movementType,
      movementTypeText : getMovementTypeText(movementType),
      processed: Boolean(processedMovementType),
      processedMovementType,
      relatedCarLog
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

  const applyPageResponse = (data, target) => {
    target.value = data?.items ?? [];
    currentPage.value = Number(data?.page ?? 1);
    pageSize.value = Number(data?.size ?? 10);
    totalPages.value = Math.max(1, Number(data?.totalPages ?? 1));
    totalCount.value = Number(data?.totalCount ?? 0);
  };

  // 카메라 데이터 서버 페이징 조회
  const loadList = async (page = 1, parkingNo = null) => {
    await ensureRelationLists();
    const res = await getCameraDataList({ page, size: pageSize.value, parkingNo });
    applyPageResponse(res.data, list);
    searchResults.value = [];
    searchMode.value = false;
  };

  // 차량번호 검색도 서버에서 페이지 단위로 조회
  const searchByCarNo = async (carNo, page = 1, parkingNo = null) => {
    await ensureRelationLists();
    const res = await searchCameraDataByCarNo(carNo, page, pageSize.value, parkingNo);
    applyPageResponse(res.data, searchResults);
    searchMode.value = true;
  };

  // 상세 조회
  const loadDetail = async (cameraDataNo) => {
    const res = await getCameraDataDetail(cameraDataNo);
    detail.value = res.data;
  };

  // 카메라 데이터 삭제
  const remove = async (cameraDataNo) => {
    try {
      const response = await deleteCameraData(cameraDataNo);

      if (response.data !== 1) {
        showFeedback("카메라 기록 삭제에 실패했습니다.", "error");
        return false;
      }

      list.value = list.value.filter((item) => {
        return Number(item.cameraDataNo ?? item.camera_data_no) !== Number(cameraDataNo);
      });
      searchResults.value = searchResults.value.filter((item) => {
        return Number(item.cameraDataNo ?? item.camera_data_no) !== Number(cameraDataNo);
      });

      delete detailMap.value[cameraDataNo];

      showFeedback("카메라 기록을 삭제했습니다.");
      return true;
    } catch (error) {
      console.error("카메라 기록 삭제 실패", error);
      showFeedback("카메라 기록 삭제에 실패했습니다.", "error");
      return false;
    }
  };

  return {
    list,
    searchResults,
    detail,
    detailMap,
    feedbackMessage,
    feedbackType,
    currentPage,
    pageSize,
    totalPages,
    totalCount,
    displayList,
    loadList,
    searchByCarNo,
    loadDetail,
    remove,
  };

});
