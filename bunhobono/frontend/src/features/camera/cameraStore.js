import { defineStore } from "pinia";
import { ref } from "vue";
import { deleteCamera, getCameraList, signupCamera, updateCamera } from "./cameraApi";

export const useCameraStore =  defineStore("camera", () => {

  const list = ref ([]);

  // 카메라 목록
  const loadList = async () => {
    const res = await getCameraList();
 
    list.value = res.data;
  };

  // 카메라 등록
  const signup = async (data, router) => {
    const res = await signupCamera(data);

    if (res.data === 1) {
      alert("카메라 등록 완료");

      await loadList();
      if (router) {
        router.push("/admin/cameras");
      }
      return true;
    } else {
      alert("카메라 등록 실패");
      return false;
    }
  };

  // 카메라 수정
  const update = async (cameraNo, data, router) => {
    const res = await updateCamera(cameraNo,data);

    if (res.data === 1) {
      alert("카메라 수정 완료");

      await loadList();
      router.push("/admin/cameras");
    } else {
      alert("카메라 수정 실패");
    }
  };

  // 카메라 삭제
  const remove = async (cameraNo) => {
    const result =
      confirm("카메라를 삭제하시겠습니까?");

    if (!result) {
      return;
    }

    try {
      const res = await deleteCamera(cameraNo);

      if (res.data === 1) {
        list.value = list.value.filter((camera) => {
          return camera.cameraNo !== cameraNo;
        });

        alert("카메라 삭제 완료");
      } else {
        alert("카메라 삭제 실패");
      }
    } catch (e) {
      console.error(e);

      alert("카메라 데이터가 연결되어 있어 삭제할 수 없습니다.");
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
