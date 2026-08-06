import {
    getResidentDashboard,
    getTodayWeather
} from "@/shared/api/residentDashboardApi"; 
import { getParkingsList } from "@/features/parking/parkingsApi";
import { toVehicleView } from "@/features/vehicle/vehicleFormat";
import { defineStore } from "pinia";
import { computed, ref } from "vue";

export const useResidentDashboardStore = defineStore("residentdashboard", () => {

    const loading = ref(false);
    const errorMessage = ref("");
    const weatherLoading = ref(false);
    const weatherErrorMessage = ref("");

    const weather = ref({
        temperature: null,
        humidity: null,
        precipitation: "강수 없음",
        rainfall: null,
        windSpeed: null,
        observedAt: ""
    });

    const dashboard = ref({
        member: {},
        normalVehicleCount : 0,
        visitVehicleCount: 0,
        totalParkingSpaces: 0,
        availableParkingSpaces: 0,
        vehicles: [],
        parkings: [],
        recentCarLogs: []
    });

    // 로그인한 입주민의 동·호수
    const residenceText = computed(() => {
        const member = dashboard.value.member;

        if (!member.dong || !member.ho) {
            return "-";
        }

        return `${member.dong}동 ${member.ho}호`;
    });

    // 로그인한 입주민의 본인 차량
    const normalVehicles = computed(() => {
        return dashboard.value.vehicles.filter((vehicle) => {
            return vehicle.vehicleType === "normal";
        });
    });

    // 로그인한 입주민이 신청한 방문 차량
    const visitVehicles = computed(() => {
        return dashboard.value.vehicles.filter((vehicle) => {
            return vehicle.vehicleType === "visit";
        });
    });

    // 주차장별 전체·사용·가능 면수와 사용률
    const parkingStatusList = computed(() => {
        return dashboard.value.parkings.map((parking) => {
            const total = Math.max(
                Number(parking.parkingSpaces ?? 0),
                0
            );

            const available = Math.min(
                Math.max(
                    Number(parking.availableSpaces ?? 0),
                    0
                ),
                total
            );

            const occupied = Math.max(
                total - available,
                0
            );

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

    const precipitationText = (code) => {
        const labels = {
            "0": "강수 없음",
            "1": "비",
            "2": "비 또는 눈",
            "3": "눈",
            "5": "빗방울",
            "6": "빗방울 또는 눈날림",
            "7": "눈날림"
        };

        return labels[String(code)] ?? "강수 없음";
    };

    // 기상청 원본 JSON에서 입주민 홈에 필요한 날씨 값만 꺼낸다.
    const loadWeather = async () => {
        weatherLoading.value = true;
        weatherErrorMessage.value = "";

        try {
            const response = await getTodayWeather();
            const rawData = typeof response.data === "string"
                ? JSON.parse(response.data)
                : response.data;

            const items = rawData?.response?.body?.items?.item;

            if (!Array.isArray(items) || items.length === 0) {
                throw new Error("기상청 날씨 데이터가 없습니다.");
            }

            const valueOf = (category) => {
                return items.find((item) => item.category === category)?.obsrValue;
            };

            const firstItem = items[0];

            weather.value = {
                temperature: valueOf("T1H") ?? null,
                humidity: valueOf("REH") ?? null,
                precipitation: precipitationText(valueOf("PTY")),
                rainfall: valueOf("RN1") ?? null,
                windSpeed: valueOf("WSD") ?? null,
                observedAt: firstItem
                    ? `${firstItem.baseDate} ${firstItem.baseTime}`
                    : ""
            };
        } catch (error) {
            console.error("날씨 정보를 불러오지 못했습니다.", error);
            weatherErrorMessage.value = "날씨 정보를 불러오지 못했습니다.";
        } finally {
            weatherLoading.value = false;
        }
    };

    // 입주민 대시보드에 필요한 데이터를 조회하고 조합
    const loadDashboard = async () => {
        loading.value = true;
        errorMessage.value = "";

        try {
            const [residentResponse, parkingResponse] = await Promise.all([
                getResidentDashboard(),
                getParkingsList(),
                loadWeather()
            ]);

            const residentData = residentResponse.data || {};
            const member = residentData.member || {};
            const vehicles = (residentData.vehicles || []).map(toVehicleView);
            const parkings = parkingResponse.data || [];

            const recentCarLogs = (residentData.recentCarLogs || [])
                .sort((left, right) => {
                    return new Date(right.inTime) - new Date(left.inTime);
                });

            dashboard.value = {
                member,
                normalVehicleCount: vehicles.filter((vehicle) => {
                    return vehicle.vehicleType === "normal";
                }).length,
                visitVehicleCount: vehicles.filter((vehicle) => {
                    return vehicle.vehicleType === "visit";
                }).length,
                totalParkingSpaces: parkings.reduce((sum, parking) => {
                    return sum + Number(parking.parkingSpaces || 0);
                }, 0),
                availableParkingSpaces: parkings.reduce((sum, parking) => {
                    return sum + Number(parking.availableSpaces || 0);
                }, 0),
                vehicles,
                parkings,
                recentCarLogs
            };
        } catch (error) {
            console.error(error);

            errorMessage.value = "입주민 정보를 불러오지 못했습니다.";
        } finally {
            loading.value = false;
        }
    };

    return {
        loading,
        errorMessage,
        weatherLoading,
        weatherErrorMessage,
        weather,
        dashboard,
        residenceText,
        normalVehicles,
        visitVehicles,
        parkingStatusList,
        loadWeather,
        loadDashboard
    };

});
