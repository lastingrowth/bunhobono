import { defineStore } from "pinia";
import { computed, ref } from "vue";
import {
    createResVehicle,
    getResVehicleList,
    getResVehicleMemberInfo,
    getResVehicleNotifications,
    markResVehicleNotificationsRead,
    deleteResVehicleNotification
} from "./resVehicleApi";
import { toVehicleView } from "../vehicle/vehicleFormat";

export const useResVehicleStore = defineStore("resVehicle", () => {
    const member = ref({});
    const vehicleList = ref([]);
    const vehicle = ref({});
    const notifications = ref([]);

    const loadMyInfo = async () => {
        const res = await getResVehicleMemberInfo();
        member.value = res.data;
    };

    const loadVehicleList = async () => {
        const res = await getResVehicleList();
        vehicleList.value = res.data.map(toVehicleView);
    };

    // 로그인한 입주민의 알림 조회
    const loadNotifications = async () => {
        const res = await getResVehicleNotifications();
        notifications.value = res.data;
    };

    // readAt이 없는 알림만 읽지 않은 알림으로 계산
    const unreadNotificationCount = computed(() => {
        return notifications.value.filter((item) => {
            return item.readAt == null;
        }).length;
    });

    // 알림함을 열었을 때 읽지 않은 알림 전체 읽음 처리
    const markAllNotificationsRead = async () => {
        if (unreadNotificationCount.value === 0) {
            return;
        }

        await markResVehicleNotificationsRead();
        await loadNotifications();
    };

    const normalVehicles = computed(() => {
        return vehicleList.value.filter((item) => {
            return item.vehicleType === "normal";
        });
    });

    const visitVehicles = computed(() => {
        return vehicleList.value
            .filter((item) => item.vehicleType === "visit")
            .sort((a, b) => {
                return Number(b.vehicleCarNo) - Number(a.vehicleCarNo);
            });
    });

    // 현재 추가 방문차량을 신청할 수 없는지 확인
    const hasActiveVisitVehicle = computed(() => {
        const now = Date.now();

        return visitVehicles.value.some((item) => {
            if (item.vehicleStatus === "WAITING") {
                return true;
            }

            if (item.vehicleStatus !== "APPROVED" || item.outTime) {
                return false;
            }

            if (item.expiryType === "NO_ENTRY" || item.expiryType === "OVERSTAY") {
                return false;
            }

            if (item.inTime || !item.startDate) {
                return true;
            }

            const noEntryDeadline =
                new Date(item.startDate).getTime() + (60 * 60 * 1000);

            return now <= noEntryDeadline;
        });
    });

    const addVisitVehicle = async (data) => {
        await createResVehicle(data);
        await loadVehicleList();
    };

    const removeNotification = async (vehicleNtNo) => {
    await deleteResVehicleNotification(vehicleNtNo);

    notifications.value = notifications.value.filter((item) => {
        return Number(item.vehicleNtNo) !== Number(vehicleNtNo);
    });
};

    return {
        member,
        vehicleList,
        vehicle,
        notifications,
        normalVehicles,
        visitVehicles,
        hasActiveVisitVehicle,
        unreadNotificationCount,
        removeNotification,
        loadMyInfo,
        loadVehicleList,
        loadNotifications,
        markAllNotificationsRead,
        addVisitVehicle
    };
});
