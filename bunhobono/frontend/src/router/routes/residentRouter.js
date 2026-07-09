import CameraList from "@/features/camera/CameraList.vue";
import ParkingsList from "@/features/parking/ParkingsList.vue";
import ResidentView from "@/views/resident/ResidentView.vue";
import ResVehicleView from "@/features/resVehicle/ResVehicleView.vue";
import Mypage from "@/features/member/Mypage.vue";
import ResidentEdit from "@/features/member/ResidentEdit.vue";

const residentMeta = {
    requireAuth : true,
    roles : ['RESIDENT']
}



const residentRouter = [

    {
        path : 'resident',
        name : 'residentMain',
        component : ResidentView,
        meta : residentMeta
    },

    {
        path: "resident/mypage",
        name: "residentMypage",
        component: Mypage,
        meta: residentMeta
    },

    {
        path : '/resident/mypage/edit',
        name : 'MypageEdit',
        component : ResidentEdit,
        meta : residentMeta
    },
    
    {
    path: "res/vehicles",
    name: "resVehicle",
    component: ResVehicleView,
    meta: residentMeta
    },

    
    

    {
        path: "/parkings",
        children: [
          {
            path: "list",      
            name: "parkingslist",
            component: ParkingsList,
          },
        ],
    },

    {
        path: "/camera",
        children: [
          {
            path: "list",      
            name: "cameralist",
            component: CameraList,
          },
        ],
    },

    

    
]

export default residentRouter;