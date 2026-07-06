import CameraDataDetail from "@/features/camera-data/CameraDataDetail.vue";
import CameraDataList from "@/features/camera-data/CameraDataList.vue";
import CarlogView from "@/features/carlog/CarlogView.vue";
import GateDetail from "@/features/gates/GateDetail.vue";
import GateList from "@/features/gates/GateList.vue";
import MemList from "@/features/member/MemList.vue";
import MemListDetail from "@/features/member/MemListDetail.vue";
import PolicyListView from "@/features/policy/PolicyListView.vue";
import VehicleView from "@/features/vehicle/VehicleView.vue";
import ParkingsEdit from "@/features/parking/ParkingsEdit.vue";
import ParkingsList2 from "@/features/parking/ParkingsList2.vue";
import ParkingsSignup from "@/features/parking/ParkingsSignup.vue";
import AdminView from "@/views/admin/AdminView.vue";

const adminMeta = {
  requireAuth: true,
  roles: ["ADMIN"],
};

const adminRouter = [
  {
    path: "admin",
    name: "adminMain",
    component: AdminView,
    meta: adminMeta,
  },

  {
    path: "admin/members",
    name: "memberList",
    component: MemList,
    meta: adminMeta,
  },

  {
    path: "admin/members/:memberNo",
    name: "memberDetail",
    component: MemListDetail,
    meta: adminMeta,
  },

  {
    path: "admin/carlog",
    name: "carlogList",
    component: CarlogView,
    meta: adminMeta,
  },

  {
      path : 'admin/carlog',
      name : 'carlogList',
      component : CarlogView,
      meta : adminMeta
  },

  // 차량 정보
  {
      path : 'vehicle',
      name : 'vehicle',
      component : VehicleView,
      meta : adminMeta
  },

  // 요금 정보
  {
      path : 'admin/fee-policy',
      name : 'feePolicyList',
      component : PolicyListView,
      meta : adminMeta
  },

  {
    path: "/gates",
    children: [
      {
        path: "list",
        name: "gatelist",
        component: GateList,
      },
      {
        path: ":gateNo/detail", 
        name: "gatedetail",
        component: GateDetail,
      },
     
    ],
  },

  {
    path: "/cameraData",
    children: [
      {
        path: "list",
        name: "cameraDatalist",
        component: CameraDataList,
      },

      {
        path: ":id/detail",
        name: "cameraDatadetail",
        component: CameraDataDetail,
      },
    ],
  },

  {
    path: "/parkings",
    children: [
      {
        path: "list2",
        name: "parkingslist2",
        component: ParkingsList2,
      },

      {
        path: "signUp", 
        name: "ParkingsSignup",
        component: ParkingsSignup,
      },

      {
        path: ":parkingNo/edit", 
        name: "ParkingEdit",
        component: ParkingsEdit,
      },
    ],
  },
];

export default adminRouter;
