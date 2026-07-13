import { defineStore } from "pinia";
import { ref } from "vue";


import { toVehicleView } from "./vehicleFormat";
import { createVehicle, deleteVehicle, getVehicleList, updateVehicle, updateVehicleStatus } from "./vehicleApi";

export const useVehicleStore = defineStore("vehicle", () => {

    const vehicleList = ref([]);
    const vehicle = ref({});
    const approveList = ref([]);

    // 차량 목록
    const loadVehicleList = async () => {
        const res = await getVehicleList();
        vehicleList.value = res.data.map(toVehicleView);
    };

    // 차량번호 검색
    const searchVehicle = async (carNo) => {
        const res = await getVehicleList();
        vehicleList.value = res.data.filter((item) => {
            return item.carNo?.includes(carNo.trim());
        }).map(toVehicleView);
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
    const addVehicle = async (data) => {
        const res = await createVehicle(data);
        
        await loadVehicleList();

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

    // 승인 대기 차량 목록
    const loadVehicleApproveList = async () => {
        const res = await getVehicleList();

        approveList.value = res.data
            .filter((item) => {
                return item.vehicleStatus === "WAITING";
            })
            .map(toVehicleView);
    };

    // 승인 대기 차량 상세
    const loadVehicleApproveDetail = async (vehicleCarNo) => {
        if (approveList.value.length === 0) {
            await loadVehicleApproveList();
        }

        vehicle.value = approveList.value.find((item) => {
            return Number(item.vehicleCarNo) === Number(vehicleCarNo);
        }) ?? {};
    };

    // 승인 상태 변경
    const changeVehicleApproveStatus = async (vehicleCarNo, data) => {
        await updateVehicleStatus(vehicleCarNo, data);

        await loadVehicleList();
        await loadVehicleApproveList();
    };

    return {
        vehicleList,
        vehicle,
        approveList,

        loadVehicleList,
        searchVehicle,
        loadVehicle,
        addVehicle,
        editVehicle,
        removeVehicle,

        loadVehicleApproveList,
        loadVehicleApproveDetail,
        changeVehicleApproveStatus,
    };
});