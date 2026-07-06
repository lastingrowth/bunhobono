import { defineStore } from "pinia";
import { ref } from "vue";
import { deleteCamera, getCameraList, signupCamera, updateCamera } from "./cameraApi";

export const useCameraStore =  defineStore("camera", () => {

   const list = ref ([]);
   const errorMessage = ref(null);


  // 전체 조회
  const loadList = async () => {
    const res = await getCameraList();
    list.value = res.data;
  };

  // 등록
  const signup = async (data) => {
    try {
      const res = await signupCamera(data);
      return res.data;
    } catch (err) {
      console.error("카메라 등록 실패:", err);
      errorMessage.value = "카메라 등록 실패";
    }
  };

  // 삭제
  const remove = async (cameraNo) => {
    if (confirm("정말 삭제하시겠습니까?")) {
      try {
        await deleteCamera(cameraNo);
        list.value = list.value.filter((c) => c.cameraNo !== cameraNo);
        alert("삭제 완료");
      } catch (err) {
        console.error("삭제 실패:", err);
        alert("삭제 실패");
      }
    }
  };

  // 수정
  const update = async (cameraNo, data, router) => {
    try {
      const res = await updateCamera(cameraNo, data);
      if (res.data === 1) {
        alert("수정 완료");
        router.push("/cameras/list"); // 성공 시 목록 화면 이동
      } else {
        alert("잘못된 정보 입력");
      }
    } catch (err) {
      console.error("수정 실패:", err);
      alert("수정 실패");
    }
  };

  return {
    loadList,
    list,
    update,
    remove,
    signup,
    errorMessage

  };

});