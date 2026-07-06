import { defineStore } from "pinia";
import { ref } from "vue";
import { deleteMember, getMemberDetail, getMemberList, searchMember, signupMember, updateMember, residentMypage, residentEdit, residentDelete } from "./memApi";

export const useMemStore =  defineStore("member", () => {

  const memberList = ref ([]);
  const member = ref({});

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
  };

  // 삭제
  const removeMember = async (memberNo) => {
    await deleteMember(memberNo);
  };

  // 등록
  const signup = async (member) => {
    const res = await signupMember(member);
    return res.data;
  };

    // 입주민 로그인 시, 마이페이지
  const loadMypage = async (loginId) => {
    const res = await residentMypage(loginId);
    member.value = res.data;
  };

    // 입주민 로그인 시, 입주민 본인이 회원 정보 수정
  const editResident = async (data) => {
    await residentEdit(data);
  };
  
    // 입주민 로그인 시, 입주민 본인이 직접 회원 탈퇴
  const removeResident = async () => {
    await residentDelete();
  };

  return {
    memberList,
    member,

    loadmemberList,
    search,
    loadMember,
    editMember,
    removeMember,
    signup,
    loadMypage,
    editResident,
    removeResident,
  };

});