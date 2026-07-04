import CarlogView from "@/features/carlog/CarlogView.vue";
import MemList from "@/features/member/MemList.vue";
import MemListDetail from "@/features/member/MemListDetail.vue";
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
    }
]

export default adminRouter