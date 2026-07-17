import { computed, ref } from "vue";
import { defineStore } from "pinia";

import { useNoticeStore } from "@/features/notice/noticeStore";
import { useVehicleStore } from "@/features/vehicle/vehicleStore";
import { useParkingsStore } from "@/features/parking/parkingsStore";
import { getCarLogs } from "@/features/carlog/carlogApi";
import { toCarLogView } from "@/features/carlog/carlogFormat";
import { useCameraDataStore } from "@/features/camera-data/cameraDataStore";
import { useCameraStore } from "@/features/camera/cameraStore";
import { useGateStore } from "@/features/gates/gateStore";
import { getCameraDataDetail, getCameraDataImage } from "@/features/camera-data/cameraDataApi";


export const useAdminDashboardStore = defineStore("adminDashboard", () => {
  
    const noticeStore = useNoticeStore();
    const vehicleStore = useVehicleStore();
    const parkingStore = useParkingsStore();
    const cameraDataStore = useCameraDataStore();
    const cameraStore = useCameraStore();
    const gateStore = useGateStore();

    const loading = ref(false);
    const errorMessage = ref("");
    const ocrImageUrls = ref({});
    const ocrDetails = ref({});

    // 입출차 목록의 현재 페이지
    const currentCarlogPage = ref(1);

    // 한 페이지에 표시할 입출차 기록 수
    const carlogPageSize = 5;

    // 관리자 대시보드에서만 사용할 입출차 기록
    // carlog 페이지의 검색 조건과 섞이지 않도록 별도로 관리한다
    const dashboardCarlogs = ref([]);

    // 확인하지 않은 알림 수
    const unresolvedNoticeCount = computed(() => {
        return noticeStore.notices.filter((notice) => {
            const status = notice.alertStat ?? notice.alert_stat;

            return status === "Unresolved";
        }).length;
    });

    // 승인 대기 차량 수
    const waitingVehicleCount = computed(() => {
        return vehicleStore.vehicleList.filter((vehicle) => {
            return vehicle.vehicleStatus === "WAITING";
        }).length;
    });

    // 오늘 방문 차량 수
    const todayVisitVehicleCount = computed(() => {
        const today = new Date();

        return vehicleStore.vehicleList.filter((vehicle) => {
            if (vehicle.vehicleType !== "visit") {
                return false;
            }

            const startDate = new Date(vehicle.startDate);

            if (Number.isNaN(startDate.getTime())) {
                return false;
            }

            return startDate.getFullYear() === today.getFullYear()
                && startDate.getMonth() === today.getMonth()
                && startDate.getDate() === today.getDate();
        }).length;
    })


    // 주차장별 전체 면수, 사용 면수, 가능 면수와 사용률
    const parkingStatusList = computed(() => {
        return parkingStore.list.map((parking) => {
            const total = Math.max(
                Number(parking.parkingSpaces ?? 0),
                0
            );

            const available = Math.min(
                Math.max(Number(parking.availableSpaces ?? 0), 0), total);

                const occupied = Math.max(total - available, 0);

                const usageRate = total === 0
            ? 0
            : Math.round(occupied / total * 100);

            return {
                ...parking,
                total,
                available,
                occupied,
                usageRate
            };
        });
    });

    // 전체 OCR 데이터 수
    const ocrTotalCount = computed(() => {
        return cameraDataStore.list.length;
    });

    // 차량번호가 인식된 OCR 데이터 수
    const ocrSuccessCount = computed(() => {
        return cameraDataStore.list.filter((data) => {
            if (typeof data.recognitionState === "boolean") {
                return data.recognitionState;
            }

            return Boolean(data.carNo);
        }).length;
    });

    // 차량번호를 인식하지 못한 OCR 데이터 수
    const ocrFailCount = computed(() => {
        return ocrTotalCount.value - ocrSuccessCount.value;
    });

    // 전체 OCR 데이터 대비 성공률
    const ocrSuccessRate = computed(() => {
        if (ocrTotalCount.value === 0) {
            return 0;
        }

        return Math.round(
            ocrSuccessCount.value
            / ocrTotalCount.value
            * 100
        );
    });

    const getCameraParkingNo = (cameraNo) => {
        const camera = cameraStore.list.find((item) => {
            return Number(item.cameraNo) === Number(cameraNo);
        });

        const gate = gateStore.list.find((item) => {
            return Number(item.gateNo) === Number(camera?.gateNo);
        });

        return gate?.parkingNo ?? null;
    };

    const getLatestCameraDataByParkingNo = (parkingNo) => {
        return [...cameraDataStore.displayList]
            .filter((data) => {
                return data.cameraDataNo
                    && Number(getCameraParkingNo(data.cameraNo)) === Number(parkingNo);
            })
            .sort((a, b) => {
                const aTime = new Date(a.captureTime ?? 0).getTime();
                const bTime = new Date(b.captureTime ?? 0).getTime();

                return bTime - aTime;
            })[0];
    };

    const getParkingOcrCard = (parking) => {
        const data = getLatestCameraDataByParkingNo(parking.parkingNo);

        if (!data) {
            return {
                cameraDataNo: null,
                imageUrl: "",
                carNoText: "데이터 없음",
                movementText: "-",
                confidenceText: "-"
            };
        }

        const detail = ocrDetails.value[data.cameraDataNo] ?? {};
        const confidenceScore = detail.confidenceScore ?? data.confidenceScore;
        const vehicleStatus = detail.vehicleStatus ?? data.vehicleStatus;
        const vehicleCarNo = detail.vehicleCarNo ?? data.vehicleCarNo;
        const waitingForApproval = data.movementType === "IN"
            && !data.processed
            && (!vehicleCarNo || vehicleStatus === "WAITING" || vehicleStatus === "UNKNOWN");

        return {
            ...data,
            ...detail,
            imageUrl: ocrImageUrls.value[data.cameraDataNo] ?? "",
            carNoText: detail.carNo || data.carNo || "미인식",
            movementText: waitingForApproval
                ? "대기중"
                : data.movementTypeText ?? "확인 불가",
            confidenceText: confidenceScore == null
                ? "-"
                : `${Number(confidenceScore).toFixed(1)}%`
        };
    };

    // 주차장 현황 카드 안에 함께 표시할 주차장별 최신 OCR 정보
    const parkingStatusWithOcr = computed(() => {
        return parkingStatusList.value.slice(0, 4).map((parking) => {
            return {
                ...parking,
                ocr: getParkingOcrCard(parking)
            };
        });
    });

    // 입차 카메라에 감지됐지만 자동 통과하지 않은 수동 승인 대상 차량
    const manualApprovalVehicles = computed(() => {
        return [...cameraDataStore.displayList]
            .filter((data) => {
                if (!data.cameraDataNo || data.movementType !== "IN" || data.processed) {
                    return false;
                }

                if (!data.vehicleCarNo) {
                    return true;
                }

                return data.vehicleStatus !== "APPROVED";
            })
            .sort((a, b) => {
                return new Date(b.captureTime ?? 0).getTime()
                    - new Date(a.captureTime ?? 0).getTime();
            });
    });

    const clearOcrImageUrls = () => {
        Object.values(ocrImageUrls.value).forEach((url) => {
            if (url) {
                URL.revokeObjectURL(url);
            }
        });

        ocrImageUrls.value = {};
        ocrDetails.value = {};
    };

    const loadOcrImages = async () => {
        clearOcrImageUrls();

        const latestItems = parkingStatusList.value
            .slice(0, 4)
            .map((parking) => getLatestCameraDataByParkingNo(parking.parkingNo))
            .filter(Boolean);

        const results = await Promise.all(
            latestItems.map(async (data) => {
                const result = {
                    cameraDataNo: data.cameraDataNo,
                    detail: {},
                    imageUrl: ""
                };

                try {
                    const detailResponse = await getCameraDataDetail(data.cameraDataNo);
                    result.detail = detailResponse.data ?? {};
                } catch (error) {
                    console.error("OCR 상세 정보 조회 실패", error);
                }

                try {
                    const imageResponse = await getCameraDataImage(data.cameraDataNo);
                    result.imageUrl = URL.createObjectURL(imageResponse.data);
                } catch (error) {
                    console.error("OCR 차량 이미지 조회 실패", error);
                }

                return result;
            })
        );

        ocrDetails.value = Object.fromEntries(
            results.map((result) => [result.cameraDataNo, result.detail])
        );
        ocrImageUrls.value = Object.fromEntries(
            results.map((result) => [result.cameraDataNo, result.imageUrl])
        );
    };

    // 새로 등록된 OCR 데이터와 사진만 다시 조회
    const refreshOcrCards = async () => {
        await cameraDataStore.loadList();
        await loadOcrImages();
    }

    // 날짜를 YYYY-MM-DD 형식으로 변환
    const getDateKey = (date) => {
        const year = date.getFullYear();
        const month = String(
            date.getMonth() + 1
        ).padStart(2, "0");

        const day = String(
            date.getDate()
        ).padStart(2, "0");

        return `${year}-${month}-${day}`;
    };

    // 관리자 대시보드는 항상 최신 입출차 기록을 기준으로 조회한다.
    // carlogStore.search를 사용하지 않기 때문에 carlog 페이지의 검색/정렬 상태에 영향을 받지 않는다.
    const loadDashboardCarlogs = async () => {
        const res = await getCarLogs({
            gateNo: null,
            parkingNo: null,
            parkingState: "",
            carKind: "",
            carNo: "",
            sort: "latest"
        });

        dashboardCarlogs.value = res.data.map(toCarLogView);
    };

    // 오늘을 포함한 최근 7일 입차 건수
    const weeklyEntryStats = computed(() => {
        const weekNames = [
            "일",
            "월",
            "화",
            "수",
            "목",
            "금",
            "토"
        ];

        const days = [];

        // 6일 전부터 오늘까지 날짜 생성
        for (let index = 6; index >= 0; index -= 1) {
            const date = new Date();

            date.setHours(0, 0, 0, 0);
            date.setDate(date.getDate() - index);

            const dateKey = getDateKey(date);

            // 해당 날짜에 입차한 기록 수
            const dayLogs = dashboardCarlogs.value.filter((log) => {
                if (!log.inTime) {
                    return false;
                }

                const inTime = new Date(log.inTime);

                if (Number.isNaN(inTime.getTime())) {
                    return false;
                }

                return getDateKey(inTime) === dateKey;
            });

            // 입주민 차량 입차 수
            const residentCount = dayLogs.filter((log) => {
                return log.carKind === "normal" || log.vehicleType === "normal";
            }).length;

            // 방문 차량 입차 수
            const visitCount = dayLogs.filter((log) => {
                return log.carKind === "visit" || log.vehicleType === "visit";
            }).length;

            days.push({
                dateKey,
                dateLabel: `${date.getMonth() + 1}/${date.getDate()}`,
                dayLabel: weekNames[date.getDay()],
                residentCount,
                visitCount
            });
        }

        // 입주민 / 방문객 중 입차량이 가장 많은 날을 막대 높이 100%로 사용
        const maximumCount = Math.max(
            ...days.map((day) => { return Math.max(day.residentCount, day.visitCount);}), 1
        );

        return days.map((day) => {
            return {
                ...day,
                residentBarHeight : day.residentCount === 0 ? 0 : Math.max(Math.round(day.residentCount / maximumCount * 100), 8),
                visitBarHeight : day.visitCount === 0 ? 0 : Math.max(Math.round(day.visitCount / maximumCount * 100), 8)
            };
        });
    });

    // 입출차 목록의 전체 페이지 수
    const carlogTotalPages = computed(() => {
        return Math.max(
            Math.ceil(
                dashboardCarlogs.value.length / carlogPageSize
            ),
            1
        );
    });


    // 현재 페이지에 표시할 입출차 기록
    const paginatedCarlogs = computed(() => {
        const start = (
            currentCarlogPage.value - 1
        ) * carlogPageSize;

        const end = start + carlogPageSize;

        return dashboardCarlogs.value.slice(start, end);
    });

    // 현재 페이지를 기준으로 최대 5개의 페이지 번호 표시
    const carlogPageNumbers = computed(() => {
        const pageGroupSize = 5;

        const currentGroup = Math.ceil(
            currentCarlogPage.value / pageGroupSize
        );

        let startPage = (currentGroup - 1) * pageGroupSize + 1;

        let endPage = Math.min(
            startPage + pageGroupSize - 1,
            carlogTotalPages.value
        );
        
        return Array.from(
            {
                length: endPage - startPage + 1
            },
            (_, index) => startPage + index
        );
    });

    // 입출차 목록 페이지 변경
    const setCarlogPage = (page) => {
        if (
            page < 1
            || page > carlogTotalPages.value
        ) {
            return;
        }

        currentCarlogPage.value = page;
    };


    // 주차장 현황, 입출차 기록, OCR 사진을 백그라운드로 다시 조회
    const refreshOcrImages = async () => {
        try {
            await Promise.all([
                parkingStore.loadList(),
                loadDashboardCarlogs(),
                cameraDataStore.loadList()
            ]);

            await loadOcrImages();
        } catch (error) {
            console.error("대시보드 자동 갱신 실패", error);
        }
    };

    // 대시보드에 필요한 데이터를 한 번에 조회
    const loadDashboard = async () => {
        loading.value = true;
        errorMessage.value = "";

        const results = await Promise.allSettled([
            noticeStore.loadNotices(),
            vehicleStore.loadVehicleList(),
            parkingStore.loadList(),
            loadDashboardCarlogs(),
            cameraStore.loadList(),
            gateStore.loadList(),
            cameraDataStore.loadList()
        ]);

        const failed = results.some((result) => {
            return result.status === "rejected";
        });

        if (failed) {
            errorMessage.value = "일부 현황을 불러오지 못했습니다.";
        }

        await loadOcrImages();

        // 새로 조회한 목록은 첫 페이지부터 표시
        currentCarlogPage.value = 1;
        loading.value = false;
    };

    return {
        loading,
        errorMessage,
        unresolvedNoticeCount,
        waitingVehicleCount,
        todayVisitVehicleCount,
        parkingStatusList,
        ocrTotalCount,
        ocrSuccessCount,
        ocrFailCount,
        ocrSuccessRate,
        parkingStatusWithOcr,
        manualApprovalVehicles,
        weeklyEntryStats,
        currentCarlogPage,
        carlogTotalPages,
        carlogPageNumbers,
        paginatedCarlogs,
        setCarlogPage,
        refreshOcrCards,
        refreshOcrImages,
        loadDashboard
    };
});
