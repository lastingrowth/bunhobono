import RobotDetail from "@/features/robot/RobotDetail.vue";
import RobotList from "@/features/robot/RobotList.vue";

export default [
  {
    path: "robots",
    name: "RobotList",
    component: RobotList,
  },
  {
    path: "robots/:robotNo",
    name: "RobotDetail",
    component: RobotDetail,
  },
];