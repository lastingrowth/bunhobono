import { defineStore } from "pinia";
import { ref } from "vue";
import { getVehicleByNo, getVehicleList, getVehicleDetail, createVehicle, updateVehicle,deleteVehicle } from "./vehicleApi";
import { getVehicleApproveList } from "./vehicleApi";
import { getVehicleApproveDetail } from "./vehicleApi";
import { updateVehicleApproveStatus } from "./vehicleApi";

export const useVehicleStore = defineStore("vehicle", () => {

    const vehicleList = ref([]);
    const vehicle = ref({});
    const approveList = ref([]);
    
    // 차량 목록
    const loadVehicleList = async () => {
        const res = await getVehicleList();
        vehicleList.value = res.data;
    };

    // 차량 검색
    const searchVehicle = async (vehicleNo) => {
        const res = await getVehicleByNo(vehicleNo);
        vehicleList.value = res.data;
    };

    // 차량 상세
    const loadVehicle = async (vehicleNo) => {
        const res = await getVehicleDetail(vehicleNo);
        vehicle.value = res.data;
    };

    // 차량 등록
    const addVehicle = async (data) => {
        const res = await createVehicle(data);
        return res.data;
    };

    // 차량 수정
    const editVehicle = async (vehicleNo, data) => {
        await updateVehicle(vehicleNo, data);
    };

    // 차량 삭제
    const removeVehicle = async (vehicleNo) => {
        await deleteVehicle(vehicleNo);
    };

    // 승인 목록
    const loadVehicleApproveList = async () => {
        const res = await getVehicleApproveList();
        approveList.value =  res.data;
    };

    // 승인 상세
    const loadVehicleApproveDetail = async (vehicleNo) => {
        const res = await getVehicleApproveDetail(vehicleNo);
        vehicle.value = res.data;
    };

    // 승인 상태 변경
    const changeVehicleApproveStatus = async (vehicleNo, data) => {
        await updateVehicleApproveStatus(vehicleNo, data);
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

