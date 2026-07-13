import { defineStore } from "pinia";
import { ref } from "vue";
import { deleteGate, getGateDetail, getList, signUpGate, updateGate } from "./gateApi";

export const useGateStore =  defineStore("gate", () => {

  const list = ref ([]);
  const detail = ref(null);

  // 게이트 목록
  const loadList = async () => {
    const res = await getList();
    list.value = res.data;
  };

  // 게이트 등록
  const signup = async (data, router) => {
    const res = await signUpGate(data);

    if (res.data === 1) {
      alert("게이트 등록 완료");

      await loadList();
      router.push("/admin/gates");
    } else {
      alert("게이트 등록 실패");
    }
  };

  // 게이트 상세
  const loadDetail = async (gateNo) => {
    const res = await getGateDetail(gateNo);
    detail.value = res.data;
  };

  // 게이트 수정
  const update = async (gateNo, data, router) => {
    const res = await updateGate(gateNo, data);

    if (res.data === 1) {
      alert("게이트 수정 완료");

      await loadList();
      router.push("/admin/gates");
    } else {
      alert("게이트 수정 실패");
    }
  };

  // 게이트 삭제
  const remove = async (gateNo) => {
    const result = confirm("게이트를 삭제하시겠습니까?");

    if (!result) {
        return;
    }

    const res = await deleteGate(gateNo);

    if (res.data === 1) {
      list.value = list.value.filter((gate) => {
        return gate.gateNo !== gateNo;
      });

      alert("게이트 삭제 완료");
    } else {
      alert("게이트 삭제 실패");
    }
  };



  return {
    list,
    detail,

    loadList,
    signup,
    loadDetail,
    update,
    remove
  };

});