import { defineStore } from "pinia";
import { ref } from "vue";
import { deleteGate, getList, signUpGate, updateGate } from "./gateApi";

export const useGateStore =  defineStore("gate", () => {

  const list = ref ([]);

  // 게이트 목록
  const loadList = async () => {
    const res = await getList();
    list.value = res.data;
  };

  // 게이트 등록
  const signup = async (data, router) => {
    const duplicated = list.value.some((gate) =>
      String(gate.gateCode).toUpperCase()
        === String(data.gateCode).toUpperCase()
    );

    if (duplicated) {
      return {
        success: false,
        message: "이미 등록된 게이트 코드입니다.",
      };
    }

    try {
      const res = await signUpGate(data);

      if (Number(res.data) === 1) {
        await loadList();
        if (router) {
          router.push("/admin/gates");
        }
        return { success: true };
      }

      return {
        success: false,
        message: "게이트 등록 처리 결과를 확인할 수 없습니다.",
      };
    } catch (e) {
      console.error(e);
      return {
        success: false,
        message: e.response?.data?.message
          ?? "게이트 등록에 실패했습니다.",
      };
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
        return { success: false, message: "연결된 카메라·입출차 기록 또는 주차면이 있어 삭제할 수 없습니다." };
      }  
    } catch (e) {
      console.error(e);

      return { success: false, message: e.response?.data?.message ?? "카메라 입출차 기록에서 사용 중인 게이트는 삭제할 수 없습니다." };
    }
  };

  return {
    list,

    loadList,
    signup,
    update,
    remove
  };

});
