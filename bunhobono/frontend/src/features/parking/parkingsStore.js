import { defineStore } from "pinia";
import { ref } from "vue";
import {
  deleteParking,
  getParkingsList,
  signupParkings,
  updateParking,
} from "./parkingsApi";

export const useParkingsStore = defineStore("parkings", () => {
  const list = ref([]); // 전체 주차장 목록
  const statusList = ref([]); // 실시간 자리 상태

  // 전체 조회
  const loadList = async () => {
    const res = await getParkingsList();
    list.value = res.data;
  };

  // 등록
  const signup = async (data) => {
    const res = await signupParkings(data);
    return res.data;
  };

  // 삭제
  const remove = async (parkingNo) => {
    if (confirm("정말 삭제하시겠습니까?")) {
      try {
        await deleteParking(parkingNo);
        list.value = list.value.filter((p) => p.parkingNo !== parkingNo);
        alert("삭제 완료");
      } catch (err) {
        console.error("삭제 실패:", err);
        alert("삭제 실패");
      }
    }
  };

  // 수정
  // router가 있으면 수정 페이지 방식처럼 이동하고,
  // router가 없으면 현재 목록 화면에 그대로 머문다.
  const update = async (parkingNo, data, router) => {
    const res = await updateParking(parkingNo, data)

    if (res.data === 1) {
      alert('수정 완료')

      await loadList()

      if (router) {
        router.push('/admin/parkings')
      }

      return
    }

    alert('잘못된 정보 입력')
  }

  return {
    list,
    statusList,
    loadList,
    signup,
    remove,
    update,
  };
});
