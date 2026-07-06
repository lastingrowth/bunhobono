import { defineStore } from "pinia";
import { ref } from "vue";
import { deleteMember, getMemberDetail, getMemberList, searchMember, signupMember, updateMember } from "./memApi";

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

  return {
    memberList,
    member,

    loadmemberList,
    search,
    loadMember,
    editMember,
    removeMember,
    signup,
  };

});