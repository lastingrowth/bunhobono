import MemList from "@/features/member/MemList.vue";
import MemListDetail from "@/features/member/MemListDetail.vue";
import MemModify from "@/features/member/MemModify.vue";

export default [
    {
        path : 'members',
        name : 'memberList',
        component : MemList
    },

    {
        path: "members/:memberNo/detail",
        name: "memberDetail",
        component: MemListDetail,
    },

    {
        path: "members/:memberNo/edit",
        name: "memberModify",
        component: MemModify,
  },
]