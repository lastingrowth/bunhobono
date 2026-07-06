import CameraDataDetail from "@/features/camera-data/CameraDataDetail.vue";
import CameraDataList from "@/features/camera-data/CameraDataList.vue";
import CarlogView from "@/features/carlog/CarlogView.vue";
import GateDetail from "@/features/gates/GateDetail.vue";
import GateList from "@/features/gates/GateList.vue";
import MemList from "@/features/member/MemList.vue";
import MemListDetail from "@/features/member/MemListDetail.vue";
import MemModify from "@/features/member/MemModify.vue";
import PolicyListView from "@/features/policy/PolicyListView.vue";
import VehicleView from "@/features/vehicle/VehicleView.vue";
import ParkingsEdit from "@/features/parking/ParkingsEdit.vue";
import ParkingsList2 from "@/features/parking/ParkingsList2.vue";
import ParkingsSignup from "@/features/parking/ParkingsSignup.vue";
import NoteList from "@/features/notice/NoteList.vue";
import NoteDetail from "@/features/notice/NoteDetail.vue";
import AdminView from "@/views/admin/AdminView.vue";
import GateSignup from "@/features/gates/GateSignup.vue";
import GateEdit from "@/features/gates/GateEdit.vue";
import CameraEdit from "@/features/camera/CameraEdit.vue";
import CameraList from "@/features/camera/CameraList.vue";
import CameraSignup from "@/features/camera/CameraSignup.vue";

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
      path : 'admin/members/:memberNo/modify',
      name : 'memberModify',
      component : MemModify,
      meta : adminMeta
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
      {
        path: "signUp", 
        name: "gatesignUp",
        component: GateSignup,
      },
      {
        path: ":gateNo/edit", 
        name: "gateEdit",
        component: GateEdit,
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

  {
        path : 'admin/carlog',
        name : 'carlogList',
        component : CarlogView,
        meta : adminMeta
  },
  
  {
      path : 'admin/notice',
      name : 'noticeList',
      component : NoteList,
      meta : adminMeta
  },

  {
      path : 'admin/notice/:noticeNo',
      name : 'noticeDetail',
      component : NoteDetail,
      meta : adminMeta
  },

  {
        path: "/cameras",
        children: [
          {
            path: "list",      
            name: "cameralist",
            component: CameraList,
          },
          {
            path: ":cameraNo/edit",      
            name: "cameraEdit",
            component: CameraEdit,
          },
          {
            path: "signup",      
            name: "cameraSignup",
            component: CameraSignup,
          },
        ],
    },
];

export default adminRouter;
