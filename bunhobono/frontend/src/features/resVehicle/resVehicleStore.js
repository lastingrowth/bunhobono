import { defineStore } from "pinia";
import { ref } from "vue";

import {
    getResVehicleMemberInfo,
    getResVehicleList,
    createResVehicle,
    updateResVehicle,
    deleteResVehicle
} from "./resVehicleApi";

import { toVehicleView } from "../vehicle/vehicleFormat";

export const useResVehicleStore = defineStore("resVehicle", () => {

    const member = ref({});
    const vehicleList = ref([]);
    const vehicle = ref({});

    // 로그인한 입주민 정보
    const loadMyInfo = async () => {
        const res = await getResVehicleMemberInfo();

        member.value = res.data;
    };

    // 입주민 본인 차량 목록
    const loadVehicleList = async () => {
        if (!member.value.memberNo) {
            await loadMyInfo();
        }

        const res = await getResVehicleList();

        vehicleList.value = res.data
            .filter((item) => {
                return Number(item.memberNo) === Number(member.value.memberNo);
            })
            .map(toVehicleView);
    };

    // 입주민 본인 차량 상세
    const loadVehicle = async (vehicleCarNo) => {
        if (vehicleList.value.length === 0) {
            await loadVehicleList();
        }

        vehicle.value = vehicleList.value.find((item) => {
            return Number(item.vehicleCarNo) === Number(vehicleCarNo);
        }) ?? {};
    };

    // 입주민 차량 등록
    const addVehicle = async (data) => {
        if (!member.value.memberNo) {
            await loadMyInfo();
        }

        const vehicleData = {
            ...data,
            memberNo: member.value.memberNo
        };

        await createResVehicle(vehicleData);

        await loadVehicleList();
    };

    // 입주민 차량 수정
    const editVehicle = async (vehicleCarNo, data) => {
        await updateResVehicle(vehicleCarNo, data);

        await loadVehicleList();
    };

    // 입주민 차량 삭제
    const removeVehicle = async (vehicleCarNo) => {
        await deleteResVehicle(vehicleCarNo);

        vehicleList.value = vehicleList.value.filter((item) => {
            return item.vehicleCarNo !== vehicleCarNo;
        });
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
        removeVehicle
    };

});