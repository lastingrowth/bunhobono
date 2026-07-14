import GateEdit from "@/features/gates/GateEdit.vue";
import GateList from "@/features/gates/GateList.vue";

export default [
  {
    path: "gates",
    name: "GateList",
    component: GateList,
  },
  {
    path: "gates/:gateNo/edit",
    name: "GateEdit",
    component: GateEdit,
  },
];
