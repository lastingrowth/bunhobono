import { defineStore } from "pinia";
import { ref } from "vue";
import { getGateDetail, getList } from "./gateApi";


export const useGateStore =  defineStore("gate", () => {

  const list = ref ([]);
  const detail = ref(null);

  // 전체 조회
  const loadList = async () => {
    const res = await getList();
    list.value = res.data;
  };

  const loadDetail = async (gateNo) => {
    const res = await getGateDetail(gateNo);
    detail.value = res.data;
  };


  return {
    list,
    detail,
    loadList,
    loadDetail,
  };

});