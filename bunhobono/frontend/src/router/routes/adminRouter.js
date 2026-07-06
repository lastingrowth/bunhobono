import CarlogView from "@/features/carlog/CarlogView.vue";
import MemList from "@/features/member/MemList.vue";
import MemListDetail from "@/features/member/MemListDetail.vue";
import PolicyListView from "@/features/policy/PolicyListView.vue";
import VehicleView from "@/features/vehicle/VehicleView.vue";
import AdminView from "@/views/admin/AdminView.vue";

const adminMeta = {
    requireAuth : true,
    roles : ['ADMIN']
}

const adminRouter = [

    {
        path : 'admin',
        name : 'adminMain',
        component : AdminView,
        meta : adminMeta
    },

    {
        path : 'admin/members',
        name : 'memberList',
        component : MemList,
        meta : adminMeta
    },

    {
        path : 'admin/members/:memberNo',
        name : 'memberDetail',
        component : MemListDetail,
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
]

export default adminRouter