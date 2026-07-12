import GateDetail from "@/features/gates/GateDetail.vue";
import GateEdit from "@/features/gates/GateEdit.vue";
import GateList from "@/features/gates/GateList.vue";
import GateSignup from "@/features/gates/GateSignup.vue";

export default [
  {
    path: "gates",
    name: "GateList",
    component: GateList,
  },
  {
    path: "gates/signup",
    name: "GateSignup",
    component: GateSignup,
  },
  {
    path: "gates/:gateNo/detail",
    name: "GateDetail",
    component: GateDetail,
  },
  {
    path: "gates/:gateNo/edit",
    name: "GateEdit",
    component: GateEdit,
  },
];