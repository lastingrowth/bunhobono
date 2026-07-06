import { defineStore } from "pinia";
import { ref } from "vue";
import { getCameraList } from "./cameraApi";

export const useCameraStore =  defineStore("camera", () => {

  const list = ref ([]);


  // 전체 조회
  const loadList = async () => {
    const res = await getCameraList();
    list.value = res.data;
  };


  return {
    loadList,
    list
  };

});