import { defineStore } from "pinia";
import { computed, ref } from "vue";
import {
    cancelResVisitVehicle,
    createResVehicle,
    getResVehicleList,
    getResVehicleMemberInfo,
    getResVehicleNotifications,
    markResVehicleNotificationRead,
    deleteResVehicleNotification,
    updateResVisitVehicleTime,
    extendResVisitVehicleOneDay,
    extendResNormalVehicle
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

    // 입주민이 선택한 알림 한 건만 읽음 처리
    const readNotification = async (notification) => {
        if (!notification || notification.readAt != null) {
            return;
        }

        await markResVehicleNotificationRead(notification.memNoticeNo);
        notification.readAt = new Date().toISOString();
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

    const addVisitVehicle = async (data) => {
        await createResVehicle(data);
        await loadVehicleList();
    };

    const cancelVisitVehicle = async (vehicleCarNo) => {
        await cancelResVisitVehicle(vehicleCarNo);
        await loadVehicleList();
    };

    const updateVisitVehicleTime = async (vehicleCarNo, data) => {
        await updateResVisitVehicleTime(vehicleCarNo, data);
        await loadVehicleList();
    };

    const extendVisitVehicleOneDay = async (vehicleCarNo) => {
        await extendResVisitVehicleOneDay(vehicleCarNo);
        await loadVehicleList();
    };

    const extendNormalVehicle = async (vehicleCarNo, endDate) => {
        await extendResNormalVehicle(vehicleCarNo, endDate);
        await loadVehicleList();
    };

    const removeNotification = async (memNoticeNo) => {
        await deleteResVehicleNotification(memNoticeNo);

        notifications.value = notifications.value.filter((item) => {
            return Number(item.memNoticeNo) !== Number(memNoticeNo);
        });
    };

    return {
        member,
        vehicleList,
        vehicle,
        notifications,
        normalVehicles,
        visitVehicles,
        unreadNotificationCount,
        removeNotification,
        readNotification,
        loadMyInfo,
        loadVehicleList,
        loadNotifications,
        addVisitVehicle,
        updateVisitVehicleTime,
        extendVisitVehicleOneDay,
        extendNormalVehicle,
        cancelVisitVehicle
    };
});
