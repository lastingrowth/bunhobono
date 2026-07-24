import { defineStore } from "pinia";
import { ref } from "vue";
import {
  approvePendingMembers,
  changeResidentPassword,
  getMemberDetail,
  getMemberList,
  getResidentSecurityChallenge,
  getAvailableSignupUnits,
  idCheckMember,
  permanentlyDeleteWithdrawnMembers,
  residentDelete,
  residentEdit,
  residentMypage,
  restoreWithdrawnMembers,
  searchMember,
  signupMember,
  updateMember,
  verifyResidentWithdrawal,
} from "./memApi";

export const useMemStore =  defineStore("member", () => {

  const memberList = ref ([]);
  const member = ref({});
  const availableSignupUnits = ref([]);

  // 서버의 날짜 문자열 또는 LocalDateTime 배열을 회원 관리 공통 형식으로 변환
  const formatMemberDateTime = (value) => {
    if (!value) {
      return "-";
    }

    const date = Array.isArray(value)
      ? new Date(
          value[0],
          Number(value[1] || 1) - 1,
          value[2] || 1,
          value[3] || 0,
          value[4] || 0,
          value[5] || 0
        )
      : new Date(value);

    if (Number.isNaN(date.getTime())) {
      return "-";
    }

    const pad = (number) => String(number).padStart(2, "0");

    return `${String(date.getFullYear()).slice(2)}.${pad(date.getMonth() + 1)}.${pad(date.getDate())} ${pad(date.getHours())}:${pad(date.getMinutes())}:${pad(date.getSeconds())}`;
  };

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

  // 선택한 전출 신청 회원을 거주 상태로 복원
  const restoreMembers = async (memberNos) => {
    const res = await restoreWithdrawnMembers(memberNos);
    await loadmemberList();
    return res.data;
  };

  // 선택한 전출 신청 회원을 전출 확정 처리
  // 실제 member 삭제가 아니라 archive 보관 후 member 원본을 미등록 상태로 비움
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

  // 등록
  const signup = async (memberData) => {
    await signupMember(memberData);
  };

  const loadAvailableSignupUnits = async () => {
    const res = await getAvailableSignupUnits();
    availableSignupUnits.value = res.data || [];
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
  
  const loadSecurityChallenge = async () => {
    const res = await getResidentSecurityChallenge();
    return res.data;
  };

  // 현재 비밀번호와 보안문자를 포함해 입주민 본인 탈퇴를 요청한다.
  const removeResident = async (securityData) => {
    await residentDelete(securityData);
  };

  // 회원 상태를 바꾸기 전에 비밀번호와 보안문자의 일치 여부만 확인한다.
  const verifyWithdrawal = async (securityData) => {
    await verifyResidentWithdrawal(securityData);
  };

  const updateResidentPassword = async (securityData) => {
    await changeResidentPassword(securityData);
  };

  // 아이디 중복확인
  const idCheck = async (loginId) => {
    const res = await idCheckMember(loginId)
    return res.data;
  }

  return {
    memberList,
    member,
    availableSignupUnits,
    formatMemberDateTime,

    loadmemberList,
    restoreMembers,
    removeWithdrawnMembers,
    search,
    loadMember,
    editMember,
    approveMembers,
    signup,
    loadAvailableSignupUnits,

    loadMypage,
    editResident,
    loadSecurityChallenge,
    verifyWithdrawal,
    removeResident,
    updateResidentPassword,
    idCheck,
  };

});
