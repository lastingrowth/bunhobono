import { defineStore } from "pinia";
import { ref } from "vue";

import { toVehicleView } from "./vehicleFormat";
import {
    createVehicle,
    deleteVehicle,
    getVehicleList,
    searchVehicleMembers,
    updateVehicle
} from "./vehicleApi";

export const useVehicleStore = defineStore("vehicle", () => {

    const vehicleList = ref([]);
    const vehicle = ref({});
    const registerMembers = ref([]);

    // 차량 목록
    const loadVehicleList = async () => {
        const res = await getVehicleList();
        vehicleList.value = res.data.map(toVehicleView);
    };

    // 차량 등록 화면에서 선택 가능한 회원 목록
    const loadRegisterMembers = async (params) => {
        const res = await searchVehicleMembers(params);
        registerMembers.value = res.data;
    };

    // 차량번호 검색
    const searchVehicle = async (carNo) => {
        const res = await getVehicleList();

        vehicleList.value = res.data
            .filter((item) => {
                return item.carNo?.includes(carNo.trim());
            })
            .map(toVehicleView);
    };

    // 차량 상세
    const loadVehicle = async (vehicleCarNo) => {
        if (vehicleList.value.length === 0) {
            await loadVehicleList();
        }

        vehicle.value = vehicleList.value.find((item) => {
            return Number(item.vehicleCarNo) === Number(vehicleCarNo);
        }) ?? {};
    };

    // 차량 등록
    // 등록 성공 여부만 반환한다.
    // 목록 갱신은 화면에서 필요할 때 따로 호출한다.
    const addVehicle = async (data) => {
        const res = await createVehicle(data);
        return res.data;
    };

    // 차량 수정
    const editVehicle = async (vehicleCarNo, data) => {
        await updateVehicle(vehicleCarNo, data);
        await loadVehicleList();
    };

    // 차량 삭제
    const removeVehicle = async (vehicleCarNo) => {
        await deleteVehicle(vehicleCarNo);
        await loadVehicleList();
    };

    return {
        vehicleList,
        vehicle,
        registerMembers,

        loadVehicleList,
        loadRegisterMembers,
        searchVehicle,
        loadVehicle,
        addVehicle,
        editVehicle,
        removeVehicle,
    };
});