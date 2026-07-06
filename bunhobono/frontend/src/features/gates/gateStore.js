import { defineStore } from "pinia";
import { ref } from "vue";
import { deleteGate, getGateDetail, getList, signUpGate, updateGate } from "./gateApi";


export const useGateStore =  defineStore("gate", () => {

  const list = ref ([]);
  const detail = ref(null);
  const errorMessage = ref(null);

  // 전체 조회
  const loadList = async () => {
    const res = await getList();
    list.value = res.data;
  };

  const loadDetail = async (gateNo) => {
    const res = await getGateDetail(gateNo);
    detail.value = res.data;
  };

   const gateSignUp = async (gateData) => {
  try {
    const res = await signUpGate(gateData);
    if (res.data === 1) {
      alert("게이트 등록 성공"); 
    } else {
      errorMessage.value = "게이트 등록 실패";
    }
  } catch (err) {
    errorMessage.value = "게이트 등록 중 오류 발생";
    console.error(err);
  }
};

const remove = async (gateNo) => {
  if (confirm("정말 삭제하시겠습니까?")) {
    try {
      const res = await deleteGate(gateNo);
      if (res.data === 1) {
        alert("게이트 삭제 성공");
        await loadList(); // 목록 갱신
      } else {
        alert("게이트 삭제 실패");
      }
    } catch (err) {
      console.error("삭제 오류:", err);
      alert("삭제 중 오류 발생");
    }
  }
};

// 수정
  const update = async (gateNo, data, router) => {
    const res = await updateGate(gateNo, data);
    if (res.data === 1) {
      alert("수정 완료");
      router.push("/gates/list");
    } else {
      alert("잘못된 정보 입력");
    }
  };

  const loadGates = async () => {
  try {
    const res = await getList();
    list.value = res.data;
  } catch (err) {
    console.error("게이트 목록 불러오기 실패:", err);
  }
}

  return {
    loadGates,
    list,
    detail,
    loadList,
    loadDetail,
    gateSignUp,
    remove,
    errorMessage,
    update,
  };

});