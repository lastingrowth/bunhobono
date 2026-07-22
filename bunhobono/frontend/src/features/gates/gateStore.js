import { defineStore } from "pinia";
import { ref } from "vue";
import { deleteGate, getList, signUpGate, updateGate, updateGateStatus, openGate } from "./gateApi";

export const useGateStore =  defineStore("gate", () => {

  const list = ref ([]);

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
      if (router) {
        router.push("/admin/gates");
      }
      return true;
    } else {
      alert("게이트 등록 실패");
      return false;
    }
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
    try {
      const res = await deleteGate(gateNo);

      if (res.data === 1) {
        list.value = list.value.filter((gate) => {
          return gate.gateNo !== gateNo;
        });
        
        return { success: true };
      } else {
        return { success: false, message: "게이트 삭제에 실패했습니다." };
      }  
    } catch (e) {
      console.error(e);

      return { success: false, message: e.response?.data?.message ?? "카메라 입출차 기록에서 사용 중인 게이트는 삭제할 수 없습니다." };
    }
  };

  // 게이트 상태 변경
  // 관리자 화면에서 열기/닫기 버튼을 눌렀을 때 사용
  const changeStatus = async (gateNo, gateStatus) => {
    const res = await updateGateStatus(gateNo, gateStatus);

    if (res.data === 1) {
      await loadList();
    } else {
      alert("게이트 상태 변경 실패");
    }
  };

  // 게이트 열기
  // 백엔드 open API를 호출하면 백엔드에서 5초 뒤 자동 닫힘까지 처리
  const open =  async (gateNo) => {
    const res = await openGate(gateNo);

    if (res.data === 1) {
      await loadList();
      return true;
    } else {
      alert("게이트 열기 실패");
      return false;
    }
  };

  return {
    list,

    loadList,
    signup,
    update,
    remove,

    changeStatus,
    open
  };

});
