import CameraEdit from "@/features/camera/CameraEdit.vue";
import CameraList from "@/features/camera/CameraList.vue";
import CameraSignup from "@/features/camera/CameraSignup.vue";

export default [
  {
    path: "cameras",
    name: "CameraList",
    component: CameraList,
  },
  {
    path: "cameras/signup",
    name: "CameraSignup",
    component: CameraSignup,
  },
  {
    path: "cameras/:cameraNo/edit",
    name: "CameraEdit",
    component: CameraEdit,
  },
];