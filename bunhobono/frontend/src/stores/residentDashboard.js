import { getResidentDashboard } from "@/shared/api/residentDashboardApi"; 
import { getParkingsList } from "@/features/parking/parkingsApi";
import { toVehicleView } from "@/features/vehicle/vehicleFormat";
import { defineStore } from "pinia";
import { computed, ref } from "vue";

export const useResidentDashboardStore = defineStore("residentdashboard", () => {

    const loading = ref(false);
    const errorMessage = ref("");

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

        if (!member.memDong || !member.memHo) {
            return "-";
        }

        return `${member.memDong}동 ${member.memHo}호`;
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

    // 입주민 대시보드에 필요한 데이터를 조회하고 조합
    const loadDashboard = async () => {
        loading.value = true;
        errorMessage.value = "";

        try {
            const [residentResponse, parkingResponse] = await Promise.all([
                getResidentDashboard(),
                getParkingsList()
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
        dashboard,
        residenceText,
        normalVehicles,
        visitVehicles,
        parkingStatusList,
        loadDashboard
    };

});