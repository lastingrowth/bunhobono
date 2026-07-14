import { defineStore } from "pinia";
import { ref } from "vue";
import {
  createResVehicle,
  deleteMember,
  deleteMemberArchives,
  deleteResVehicle,
  getMemberArchiveAlerts,
  getMemberDetail,
  getMemberList,
  getResVehicleList,
  idCheckMember,
  residentDelete,
  residentEdit,
  residentMypage,
  searchMember,
  signupMember,
  updateMember,
  updateMemberApprovalStatus,
  updateResVehicle
} from "./memApi";
import { getCarLogs } from "../carlog/carlogApi";
import { getParkingsList } from "../parking/parkingsApi";
import { toVehicleView } from "../vehicle/vehicleFormat";

export const useMemStore =  defineStore("member", () => {

  const memberList = ref ([]);
  const member = ref({});
  const memberArchiveAlerts = ref([]);
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

  // Spring 스케줄러가 분류한 탈퇴 3일 경과 회원 알림을 조회한다.
  const loadMemberArchiveAlerts = async () => {
    const res = await getMemberArchiveAlerts();
    memberArchiveAlerts.value = res.data;
  };

  // 탈퇴 후 3일 경과 회원을 삭제하고 회원 목록과 알림을 함께 갱신한다.
  const removeMemberArchives = async (memberNos) => {
    const res = await deleteMemberArchives(memberNos);
    await Promise.all([loadmemberList(), loadMemberArchiveAlerts()]);
    return res.data;
  };

  // 선택된 회원들의 승인 상태를 저장한 뒤 최신 목록을 다시 조회한다.
  const editApprovalStatus = async (memberNos, approvalStatus) => {
    await updateMemberApprovalStatus(memberNos, approvalStatus);
    await loadmemberList();
  };

  // 삭제
  const removeMember = async (memberNo) => {
    await deleteMember(memberNo);

    memberList.value = memberList.value.filter((item) => {
      return item.memberNo !== memberNo;
    });
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
    const res = await getResVehicleList();

    vehicleList.value = res.data
      .filter((item) => Number(item.memberNo) === Number(member.value.memberNo))
      .map(toVehicleView);
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
      const [memberResponse, vehicleResponse, parkingResponse, carLogResponse] = await Promise.all([
        residentMypage(),
        getResVehicleList(),
        getParkingsList(),
        getCarLogs({})
      ]);

      const memberData = memberResponse.data || {};
      const vehicles = (vehicleResponse.data || [])
        .filter((item) => Number(item.memberNo) === Number(memberData.memberNo))
        .map(toVehicleView);
      const parkings = parkingResponse.data || [];
      const vehicleNos = new Set(vehicles.map((item) => Number(item.vehicleCarNo)));
      const recentCarLogs = (carLogResponse.data || [])
        .filter((log) => vehicleNos.has(Number(log.vehicleCarNo)))
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
    memberArchiveAlerts,
    vehicleList,
    vehicle,
    loading,
    errorMessage,
    dashboard,

    loadmemberList,
    loadMemberArchiveAlerts,
    removeMemberArchives,
    search,
    loadMember,
    editMember,
    editApprovalStatus,
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
