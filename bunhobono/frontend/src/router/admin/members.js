import MemList from "@/features/member/MemList.vue";
import MemListDetail from "@/features/member/MemListDetail.vue";
import MemModify from "@/features/member/MemModify.vue";
import MemSignupRes from "@/features/member/MemSignupRes.vue";

export default [
    {
        path : 'members',
        name : 'memberList',
        component : MemList
    },

    {
        path: "signup",
        name: "memberCreate",
        component: MemSignupRes,
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
