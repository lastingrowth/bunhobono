import { defineStore } from "pinia";
import { ref } from "vue";
import {
    getResVehicleMemberInfo,
    getResVehicleList,
    getResVehicleDetail,
    createResVehicle,
    updateResVehicle,
    deleteResVehicle,
} from "./resVehicleApi";

import { toVehicleView } from "../vehicle/vehicleFormat";

export const useResVehicleStore = defineStore("resVehicle", () => {

    const member = ref({});
    const vehicleList = ref([]);
    const vehicle = ref({});

    // 로그인한 입주민 정보 조회
    const loadMyInfo = async () => {
        const res = await getResVehicleMemberInfo();
        member.value = res.data;
    };

    // 입주민 본인 차량 목록 조회
    const loadVehicleList = async () => {
        const res = await getResVehicleList();

        vehicleList.value = res.data.map(vehicle => toVehicleView(vehicle));
    };

    // 입주민 본인 차량 상세 조회
    const loadVehicle = async (vehicleNo) => {
        const res = await getResVehicleDetail(vehicleNo);

        vehicle.value = toVehicleView(res.data);
    };

    // 입주민 차량 등록 신청
    const addVehicle = async (data) => {
        await createResVehicle(data);

        await loadVehicleList();
    };

    // 입주민 차량 수정 신청
    const editVehicle = async (vehicleNo, data) => {
        await updateResVehicle(vehicleNo, data);

        await loadVehicleList();
    };

    // 입주민 차량 삭제
    const removeVehicle = async (vehicleNo) => {
        await deleteResVehicle(vehicleNo);

        await loadVehicleList();
    };

    return {
        member,
        vehicleList,
        vehicle,

        loadMyInfo,
        loadVehicleList,
        loadVehicle,
        addVehicle,
        editVehicle,
        removeVehicle,
    };
});