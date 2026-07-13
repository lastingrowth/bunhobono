import CameraEdit from "@/features/camera/CameraEdit.vue";
import CameraList from "@/features/camera/CameraList.vue";

export default [
  {
    path: "cameras",
    name: "CameraList",
    component: CameraList,
  },
  {
    path: "cameras/:cameraNo/edit",
    name: "CameraEdit",
    component: CameraEdit,
  },
];
