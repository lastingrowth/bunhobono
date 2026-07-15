import { computed, ref } from "vue";
import { defineStore } from "pinia";

import { useNoticeStore } from "@/features/notice/noticeStore";
import { useVehicleStore } from "@/features/vehicle/vehicleStore";
import { useParkingsStore } from "@/features/parking/parkingsStore";
import { useCarlogStore } from "@/features/carlog/carlogStore";
import { useCameraDataStore } from "@/features/camera-data/cameraDataStore";
import { getCameraDataDetail, getCameraDataImage } from "@/features/camera-data/cameraDataApi";

export const useAdminDashboardStore = defineStore("adminDashboard", () => {
  
    const noticeStore = useNoticeStore();
    const vehicleStore = useVehicleStore();
    const parkingStore = useParkingsStore();
    const carlogStore = useCarlogStore();
    const cameraDataStore = useCameraDataStore();

    const loading = ref(false);
    const errorMessage = ref("");
    const ocrImageUrls = ref({});
    const ocrDetails = ref({});

    // 입출차 목록의 현재 페이지
    const currentCarlogPage = ref(1);

    // 한 페이지에 표시할 입출차 기록 수
    const carlogPageSize = 4;

    // 확인하지 않은 알림 수
    const unresolvedNoticeCount = computed(() => {
        return noticeStore.notices.filter((notice) => {
            const status = notice.alertStat ?? notice.alert_stat;

            return status === "Unresolved";
        }).length;
    });

    // 전체 등록 차량 수
    const registeredVehicleCount = computed(() => {
        return vehicleStore.vehicleList.length;
    });

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

    // OCR 성공률 영역에 표시할 최신 인식 차량 4건
    const latestOcrCards = computed(() => {
        return [...cameraDataStore.list]
            .filter((data) => data.cameraDataNo)
            .sort((a, b) => {
                const aTime = new Date(a.captureTime ?? 0).getTime();
                const bTime = new Date(b.captureTime ?? 0).getTime();

                return bTime - aTime;
            })
            .slice(0, 4)
            .map((data) => {
                const detail = ocrDetails.value[data.cameraDataNo] ?? {};
                const confidenceScore = detail.confidenceScore ?? data.confidenceScore;

                return {
                    ...data,
                    ...detail,
                    imageUrl: ocrImageUrls.value[data.cameraDataNo] ?? "",
                    carNoText: detail.carNo || data.carNo || "미인식",
                    confidenceText: confidenceScore == null
                        ? "-"
                        : `${Number(confidenceScore).toFixed(1)}%`
                };
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

        const latestItems = [...cameraDataStore.list]
            .filter((data) => data.cameraDataNo)
            .sort((a, b) => {
                const aTime = new Date(a.captureTime ?? 0).getTime();
                const bTime = new Date(b.captureTime ?? 0).getTime();

                return bTime - aTime;
            })
            .slice(0, 4);

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
            const count = carlogStore.carLogs.filter((log) => {
                if (!log.inTime) {
                    return false;
                }

                const inTime = new Date(log.inTime);

                if (Number.isNaN(inTime.getTime())) {
                    return false;
                }

                return getDateKey(inTime) === dateKey;
            }).length;

            days.push({
                dateKey,
                dateLabel: `${date.getMonth() + 1}/${date.getDate()}`,
                dayLabel: weekNames[date.getDay()],
                count
            });
        }

        // 입차량이 가장 많은 날을 막대 높이 100%로 사용
        const maximumCount = Math.max(
            ...days.map((day) => day.count), 1
        );

        return days.map((day) => {
            const barHeight = day.count === 0
                ? 0
                : Math.max(
                    Math.round(day.count / maximumCount * 100), 8
                );

            return {
                ...day,
                barHeight
            };
        });
    });

    // 입출차 목록의 전체 페이지 수
    const carlogTotalPages = computed(() => {
        return Math.max(
            Math.ceil(
                carlogStore.carLogs.length / carlogPageSize
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

        return carlogStore.carLogs.slice(start, end);
    });

    // 현재 페이지를 기준으로 최대 5개의 페이지 번호 표시
    const carlogPageNumbers = computed(() => {
        const totalPages = carlogTotalPages.value;

        let startPage = Math.max(
            currentCarlogPage.value - 2,
            1
        );

        let endPage = Math.min(
            startPage + 4,
            totalPages
        );

        startPage = Math.max(
            endPage - 4,
            1
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


    // 대시보드에 필요한 데이터를 한 번에 조회
    const loadDashboard = async () => {
        loading.value = true;
        errorMessage.value = "";

        const results = await Promise.allSettled([
            noticeStore.loadNotices(),
            vehicleStore.loadVehicleList(),
            parkingStore.loadList(),
            carlogStore.loadCarLogs(),
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
        registeredVehicleCount,
        parkingStatusList,
        ocrTotalCount,
        ocrSuccessCount,
        ocrFailCount,
        ocrSuccessRate,
        latestOcrCards,
        weeklyEntryStats,
        currentCarlogPage,
        carlogTotalPages,
        carlogPageNumbers,
        paginatedCarlogs,
        setCarlogPage,
        refreshOcrCards,
        loadDashboard
    };
});