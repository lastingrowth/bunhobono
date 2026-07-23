import { getCarLogs } from "@/features/carlog/carlogApi";
import { toCarLogView } from "@/features/carlog/carlogFormat";
import { openGateByCameraData } from "@/features/camera-data/cameraDataApi";
import { getCameraDataList } from "@/features/camera-data/cameraDataApi";
import { useGateStore } from "@/features/gates/gateStore";
import { useMemStore } from "@/features/member/memStore";
import { useNoticeStore } from "@/features/notice/noticeStore";
import { useParkingsStore } from "@/features/parking/parkingsStore";
import { useVehicleStore } from "@/features/vehicle/vehicleStore";
import { defineStore } from "pinia";
import { computed, ref } from "vue";

export const useAdminDashboardStore = defineStore("adminDashboard", () => {

    const noticeStore = useNoticeStore();
    const vehicleStore = useVehicleStore();
    const memberStore = useMemStore();

    const gateStore = useGateStore();
    const parkingStore = useParkingsStore();

    const loading = ref(false);
    const errorMessage = ref("");
    const vehicleFeedbackMessage = ref("");
    const vehicleFeedbackType = ref("success");
    let vehicleFeedbackTimer;

    const showVehicleFeedback = (message, type = "success") => {
        vehicleFeedbackMessage.value = message;
        vehicleFeedbackType.value = type;
        window.clearTimeout(vehicleFeedbackTimer);
        vehicleFeedbackTimer = window.setTimeout(() => {
            vehicleFeedbackMessage.value = "";
        }, 3000);
    };

    // 관리자 대시보드 상단 차량 등록 입력값
    const quickVehicle = ref({
        carNo: "",
        vehicleType: "normal",
        role: "RESIDENT",
        periodValue: 1,
        memberNo: null,
    });

    // 등록차량이면 개월, 방문차량이면 시간 선택지를 보여준다.
    const quickPeriodOptions = computed(() => {
        if (quickVehicle.value.vehicleType === "normal") {
            return [
                { value: 1, text: "1개월" },
                { value: 3, text: "3개월" },
                { value: 6, text: "6개월" },
                { value: 12, text: "12개월" },
            ];
        }

        return [
            { value: 2, text: "2시간" },
            { value: 4, text: "4시간" },
            { value: 6, text: "6시간" },
            { value: 8, text: "8시간" },
            { value: 12, text: "12시간" },
        ];
    });

    // /vehicles/search 로 가져온 등록 가능한 회원 목록
    const quickRegisterMembers = computed(() => {
        return vehicleStore.registerMembers;
    });

    // 차량종류/회원구분에 맞는 회원 목록 조회
    const loadQuickRegisterMembers = async () => {
        await vehicleStore.loadRegisterMembers({
            vehicleType: quickVehicle.value.vehicleType,
            role: quickVehicle.value.role,
        });
    };

    const parkingCameraModes = ref({
        "A 주차장": "IN",
        "B 주차장": "IN",
        "C 주차장": "IN",
        "D 주차장": "IN",
    });

    const selectedParkingPanel = ref(null);
    const selectedCarlog = ref(null);
    const carlogLogs = ref([]);
    const cameraDataLogs = ref([]);

    // 예: 12가3456, 123가4567, 서울12가3456
    const carNoPattern = /^([가-힣]{2})?\d{2,3}[가-힣]\d{4}$/;

    const longParkingNoticeCount = computed(() => {
        return noticeStore.notices.filter((notice) => {
            const status = notice.alertStat ?? notice.alert_stat;
            return status === "Unresolved";
        }).length;
    });

    const waitingVisitVehicleCount = computed(() => {
        return vehicleStore.vehicleList.filter((vehicle) => {
            return vehicle.vehicleType === "visit"
                && vehicle.vehicleStatus === "WAITING";
        }).length;
    });

    const waitingMemberCount = computed(() => {
        return memberStore.memberList.filter((member) => {
            return member.role === "PENDING";
        }).length;
    });

    const dashboardAlerts = computed(() => {
        return [
            {
                key: "long-parking",
                title: "장기주차",
                count: longParkingNoticeCount.value,
                path: "/admin/notice",
            },
            {
                key: "visit-approve",
                title: "방문승인",
                count: waitingVisitVehicleCount.value,
                path: "/admin/vehicles?mode=approve",
            },
            {
                key: "member-approve",
                title: "회원승인",
                count: waitingMemberCount.value,
                path: "/admin/members?section=pending",
            },
        ];
    });

    const findGateByParking = (parkingName, gateType) => {
        return gateStore.list.find((gate) => {
            return gate.parkingName === parkingName
                && String(gate.gateType).toUpperCase() === gateType;
        });
    };

    const parkingMonitorPanels = computed(() => {
        return parkingStore.list.slice(0, 4).map((parking, index) => {
            const mode = parkingCameraModes.value[parking.parkingName] ?? "IN";
            const gate = findGateByParking(parking.parkingName, mode);
            const cameraNo = (index * 2) + (mode === "IN" ? 1 : 2);

            return {
                parkingNo: parking.parkingNo,
                parkingName: parking.parkingName,
                parkingLocation: parking.parkingLocation,
                mode,
                modeText: mode === "IN" ? "입차" : "출차",
                modeClass: mode === "IN" ? "in" : "out",
                cameraNo,
                gate,
            };
        });
    });

    const refreshSelectedParkingPanel = () => {
        if (!selectedParkingPanel.value) {
            return;
        }

        const latestPanel = parkingMonitorPanels.value.find((panel) => {
            return Number(panel.parkingNo) === Number(selectedParkingPanel.value.parkingNo);
        });

        selectedParkingPanel.value = latestPanel ?? selectedParkingPanel.value;
    };

    const refreshGateStatuses = async () => {
        await gateStore.loadList();
        refreshSelectedParkingPanel();
    };

    const recentCarlogs = computed(() => {
        return [...carlogLogs.value]
            .sort((left, right) => {
                const leftTime = new Date(left.outTime ?? left.inTime ?? 0).getTime();
                const rightTime = new Date(right.outTime ?? right.inTime ?? 0).getTime();
                return rightTime - leftTime;
            })
            .slice(0, 10);
    });

    const recentCameraData = computed(() => {
        return cameraDataLogs.value.slice(0, 10);
    });

    const toggleParkingCamera = (parkingName) => {
        const currentMode = parkingCameraModes.value[parkingName] ?? "IN";

        parkingCameraModes.value = {
            ...parkingCameraModes.value,
            [parkingName]: currentMode === "IN" ? "OUT" : "IN",
        };

        refreshSelectedParkingPanel();
    };

    const selectParkingPanel = (panel) => {
        selectedParkingPanel.value = panel;
    };

    const closeParkingPanel = () => {
        selectedParkingPanel.value = null;
    };

    const selectCarlog = (log) => {
        selectedCarlog.value = log;
    };

    const refreshCarlogs = async () => {
        const selectedNo = selectedCarlog.value?.carLogNo;

        const [carlogResponse, cameraDataResponse] = await Promise.all([
            getCarLogs({ sort: "activity" }),
            getCameraDataList(),
        ]);

        carlogLogs.value = Array.isArray(carlogResponse.data)
            ? carlogResponse.data.map(toCarLogView)
            : [];
        cameraDataLogs.value = Array.isArray(cameraDataResponse.data)
            ? cameraDataResponse.data
            : [];

        selectedCarlog.value = carlogLogs.value.find((log) => {
            return Number(log.carLogNo) === Number(selectedNo);
        }) ?? recentCarlogs.value[0] ?? null;
    };

    const parkingStateClass = (log) => {
        if (log.parkingState === "PARKING") {
            return "parking";
        }

        if (log.parkingState === "OUT") {
            return "out";
        }

        return "unknown";
    };

    const openManualGate = async (gate, cameraDataNo = null) => {
        if (!gate) {
            alert("연결된 게이트가 없습니다");
            return false;
        }

        let opened = false;

        if (cameraDataNo) {
            const response = await openGateByCameraData(cameraDataNo);
            opened = response.data === 1;
        } else {
            opened = await gateStore.open(gate.gateNo);
        }

        if (!opened) {
            return false;
        }

        await Promise.all([
            gateStore.loadList(),
            refreshCarlogs(),
        ]);
        refreshSelectedParkingPanel();

        setTimeout(() => {
            gateStore.loadList()
                .then(refreshSelectedParkingPanel)
                .catch((error) => {
                    console.error("게이트 닫힘 상태 갱신 실패", error);
                });
        }, 5500);

        return true;
    };

    const dongText = (dong) => {
        if (String(dong) === "0") {
            return "관리동";
        }

        return `${dong}동`;
    };

    const hoText = (ho) => {
        if (String(ho) === "0") {
            return "관리실";
        }

        return `${ho}호`;
    };

    const memberLabel = (member) => {
        return `${dongText(member.memDong)} ${hoText(member.memHo)} / ${member.memName}`;
    };

    const resetQuickVehicle = () => {
        quickVehicle.value = {
            carNo: "",
            vehicleType: "normal",
            role: "RESIDENT",
            periodValue: 1,
            memberNo: null,
        };
    };

    const submitQuickVehicle = async () => {
        const normalizedCarNo = quickVehicle.value.carNo.trim().replace(/\s/g, "");

        if (normalizedCarNo === "") {
            showVehicleFeedback("차량번호를 입력해주세요.", "error");
            return;
        }

        if (!carNoPattern.test(normalizedCarNo)) {
            showVehicleFeedback("차량번호 형식이 올바르지 않습니다. 예: 12가3456", "error");
            return;
        }

        if (!quickVehicle.value.memberNo) {
            showVehicleFeedback("등록할 회원을 선택해주세요.", "error");
            return;
        }

        const startDate = new Date();
        const endDate = new Date(startDate);

        if (quickVehicle.value.vehicleType === "normal") {
            endDate.setMonth(endDate.getMonth() + Number(quickVehicle.value.periodValue));
        } else {
            endDate.setHours(endDate.getHours() + Number(quickVehicle.value.periodValue));
        }

        try {
            await vehicleStore.addVehicle({
                carNo: normalizedCarNo,
                vehicleType: quickVehicle.value.vehicleType,
                vehicleStatus: "APPROVED",
                memberNo: quickVehicle.value.memberNo,
                startDate: formatDateTimeValue(startDate),
                endDate: formatDateTimeValue(endDate),
            });

            await vehicleStore.loadVehicleList();
            await loadQuickRegisterMembers();

            showVehicleFeedback(`${normalizedCarNo} 차량을 등록했습니다.`);
            resetQuickVehicle();
        } catch (e) {
            console.error(e);
            const responseMessage = e.response?.data?.message;
            const fallbackMessage = e.response?.status === 409
                ? "이미 등록된 차량이거나 선택한 회원의 등록 가능 대수를 초과했습니다."
                : "차량 등록 중 오류가 발생했습니다.";
            showVehicleFeedback(responseMessage || fallbackMessage, "error");
        }
    };

    function formatDateTimeValue(date) {
        const yyyy = date.getFullYear();
        const mm = String(date.getMonth() + 1).padStart(2, "0");
        const dd = String(date.getDate()).padStart(2, "0");
        const hh = String(date.getHours()).padStart(2, "0");
        const mi = String(date.getMinutes()).padStart(2, "0");
        const ss = String(date.getSeconds()).padStart(2, "0");

        return `${yyyy}-${mm}-${dd}T${hh}:${mi}:${ss}`;
    }

    const loadDashboard = async () => {
        loading.value = true;
        errorMessage.value = "";

        try {
            await Promise.all([
                noticeStore.loadNotices(),
                vehicleStore.loadVehicleList(),
                memberStore.loadmemberList(),
                parkingStore.loadList(),
                gateStore.loadList(),
                loadQuickRegisterMembers(),
                refreshCarlogs(),
            ]);

            selectedCarlog.value = recentCarlogs.value[0] ?? null;
        } catch (e) {
            console.log(e);
            errorMessage.value = "대시보드 정보를 불러오지 못했습니다";
        } finally {
            loading.value = false;
        }
    };

    return {
        loading,
        errorMessage,
        vehicleFeedbackMessage,
        vehicleFeedbackType,
        showVehicleFeedback,

        quickVehicle,
        quickPeriodOptions,
        quickRegisterMembers,

        dashboardAlerts,
        parkingMonitorPanels,
        selectedParkingPanel,
        recentCarlogs,
        recentCameraData,
        selectedCarlog,

        memberLabel,
        submitQuickVehicle,
        loadQuickRegisterMembers,

        toggleParkingCamera,
        selectParkingPanel,
        closeParkingPanel,
        selectCarlog,
        refreshCarlogs,
        parkingStateClass,
        openManualGate,
        refreshGateStatuses,
        loadDashboard,
    };
});
