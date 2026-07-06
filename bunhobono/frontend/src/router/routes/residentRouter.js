import Mypage from "@/features/member/Mypage.vue";
import ResidentEdit from "@/features/member/ResidentEdit.vue";
import ParkingsList from "@/features/parking/ParkingsList.vue";
import ResidentView from "@/views/resident/ResidentView.vue";

const residentMeta = {
    requireAuth : true,
    roles : ['RESIDENT']
}

const residentRouter = [

    {
        path : '/resident',
        name : 'residentMain',
        component : ResidentView,
        meta : residentMeta
    },
    {
        path : '/resident/mypage',
        name : 'Mypage',
        component : Mypage,
        meta : residentMeta
    },
    {
        path : '/resident/mypage/edit',
        name : 'MypageEdit',
        component : ResidentEdit,
        meta : residentMeta
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
    

    
]

export default residentRouter;