import { defineStore } from "pinia";
import { ref } from "vue";
import {
  approvePendingMembers,
  createResVehicle,
  deleteMember,
  deleteResVehicle,
  getMemberDetail,
  getMemberList,
  getResidentDashboard,
  idCheckMember,
  permanentlyDeleteWithdrawnMembers,
  residentDelete,
  residentEdit,
  residentMypage,
  restoreWithdrawnMembers,
  searchMember,
  signupMember,
  updateMember,
  updateResVehicle
} from "./memApi";
import { getParkingsList } from "../parking/parkingsApi";
import { toVehicleView } from "../vehicle/vehicleFormat";

export const useMemStore =  defineStore("member", () => {

  const memberList = ref ([]);
  const member = ref({});
  const vehicleList = ref([]);
  const vehicle = ref({});
  const loading = ref(false);
  const errorMessage = ref("");
  const dashboard = ref({
    member: {},
    normalVehicleCount: 0,
    visitVehicleCount: 0,
    totalParkingSpaces: 0,
    availableParkingSpaces: 0,
    vehicles: [],
    parkings: [],
    recentCarLogs: []
  });

  // 전체 조회
  const loadmemberList = async () => {
    const res = await getMemberList();
    memberList.value = res.data;
  };

  // 검색
  const search = async (params) => {
    const res = await searchMember(params);
    memberList.value = res.data;
  };

  // 상세 조회
  const loadMember = async (memberNo) => {
    const res = await getMemberDetail(memberNo);
    member.value = res.data;
  };

  // 수정
  const editMember = async (memberNo, data) => {
    await updateMember(memberNo, data);
    await loadMember(memberNo);
  };

  const restoreMembers = async (memberNos) => {
    const res = await restoreWithdrawnMembers(memberNos);
    await loadmemberList();
    return res.data;
  };

  const removeWithdrawnMembers = async (memberNos) => {
    const res = await permanentlyDeleteWithdrawnMembers(memberNos);
    await loadmemberList();
    return res.data;
  };

  // 선택된 승인 대기 회원을 입주민 역할로 변경한 뒤 목록을 갱신한다.
  const approveMembers = async (memberNos) => {
    await approvePendingMembers(memberNos);
    await loadmemberList();
  };

  // 회원을 탈퇴 처리하고 목록을 갱신한다.
  const removeMember = async (memberNo) => {
    await deleteMember(memberNo);
    await loadmemberList();
  };

  // 등록
  const signup = async (memberData) => {
    await signupMember(memberData);
  };

    // 입주민 로그인 시, 마이페이지
  const loadMypage = async () => {
    const res = await residentMypage();
    member.value = res.data;
  };

    // 입주민 로그인 시, 입주민 본인이 회원 정보 수정
  const editResident = async (data) => {
    await residentEdit(data);

    await loadMypage();
  };
  
    // 입주민 로그인 시, 입주민 본인이 직접 회원 탈퇴
  const removeResident = async () => {
    await residentDelete(member.value.loginId);
  };

  // 아이디 중복확인
  const idCheck = async (loginId) => {
    const res = await idCheckMember(loginId)
    return res.data;
  }

  // 로그인한 입주민이 등록한 차량만 조회한다.
  const loadVehicleList = async () => {
    const res = await getResidentDashboard();

    // 통합 대시보드 응답에서 본인 차량 목록만 사용한다.
    vehicleList.value = (res.data?.vehicles || []).map(toVehicleView);
  };

  // 현재 Store의 차량 목록에서 선택한 차량을 조회한다.
  const loadVehicle = async (vehicleCarNo) => {
    if (vehicleList.value.length === 0) {
      await loadVehicleList();
    }

    vehicle.value = vehicleList.value.find((item) => {
      return Number(item.vehicleCarNo) === Number(vehicleCarNo);
    }) ?? {};
  };

  // 입주민 차량을 등록하고 목록을 갱신한다.
  const addVehicle = async (data) => {
    await createResVehicle(data);
    await loadVehicleList();
  };

  // 입주민 차량을 수정하고 목록을 갱신한다.
  const editVehicle = async (vehicleCarNo, data) => {
    await updateResVehicle(vehicleCarNo, data);
    await loadVehicleList();
  };

  // 입주민 차량을 삭제하고 현재 목록에서도 제거한다.
  const removeVehicle = async (vehicleCarNo) => {
    await deleteResVehicle(vehicleCarNo);

    vehicleList.value = vehicleList.value.filter((item) => {
      return item.vehicleCarNo !== vehicleCarNo;
    });
  };

  // 기존 Spring API 데이터를 조회해 입주민 대시보드 데이터로 조합한다.
  const loadDashboard = async () => {
    loading.value = true;
    errorMessage.value = "";

    try {
      const [residentResponse, parkingResponse] = await Promise.all([
        getResidentDashboard(),
        getParkingsList()
      ]);

      const residentData = residentResponse.data || {};
      const memberData = residentData.member || {};
      // Spring Service가 집계한 본인 차량을 대시보드 표시 형식으로 변환한다.
      const vehicles = (residentData.vehicles || []).map(toVehicleView);
      const parkings = parkingResponse.data || [];
      const recentCarLogs = (residentData.recentCarLogs || [])
        .sort((left, right) => new Date(right.inTime) - new Date(left.inTime))
        .slice(0, 5);

      dashboard.value = {
        member: memberData,
        normalVehicleCount: vehicles.filter((item) => item.vehicleType === "normal").length,
        visitVehicleCount: vehicles.filter((item) => item.vehicleType === "visit").length,
        totalParkingSpaces: parkings.reduce((sum, parking) => sum + (parking.parkingSpaces || 0), 0),
        availableParkingSpaces: parkings.reduce((sum, parking) => sum + (parking.availableSpaces || 0), 0),
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
    memberList,
    member,
    vehicleList,
    vehicle,
    loading,
    errorMessage,
    dashboard,

    loadmemberList,
    restoreMembers,
    removeWithdrawnMembers,
    search,
    loadMember,
    editMember,
    approveMembers,
    removeMember,
    signup,

    loadMypage,
    editResident,
    removeResident,
    idCheck,

    loadVehicleList,
    loadVehicle,
    addVehicle,
    editVehicle,
    removeVehicle,
    loadDashboard,
  };

});
