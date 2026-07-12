import CameraDataDetail from "@/features/camera-data/CameraDataDetail.vue";
import CameraDataList from "@/features/camera-data/CameraDataList.vue";

export default [
  {
    path: "camera-data",
    name: "CameraDataList",
    component: CameraDataList,
  },
  {
    path: "camera-data/:cameraDataNo/detail",
    name: "CameraDataDetail",
    component: CameraDataDetail,
  },
];