import { defineStore } from "pinia";
import { ref } from "vue";
import { deleteMember, getMemberArchiveAlerts, getMemberDetail, getMemberList, searchMember, signupMember, updateMember, updateMemberApprovalStatus, residentMypage, residentEdit, residentDelete, idCheckMember } from "./memApi";

export const useMemStore =  defineStore("member", () => {

  const memberList = ref ([]);
  const member = ref({});
  const memberArchiveAlerts = ref([]);

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
  
  return {
    memberList,
    member,
    memberArchiveAlerts,

    loadmemberList,
    loadMemberArchiveAlerts,
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
  };

});
