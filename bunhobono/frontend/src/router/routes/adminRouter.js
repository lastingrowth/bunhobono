import CarlogView from "@/features/carlog/CarlogView.vue";
import MemList from "@/features/member/MemList.vue";
import MemListDetail from "@/features/member/MemListDetail.vue";
import NoteList from "@/features/notice/NoteList.vue";
import NoteDetail from "@/features/notice/NoteDetail.vue";
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
    }
]

export default adminRouter
