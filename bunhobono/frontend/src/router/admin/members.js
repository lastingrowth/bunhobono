import MemList from "@/features/member/MemList.vue";
import MemListDetail from "@/features/member/MemListDetail.vue";
import MemModify from "@/features/member/MemModify.vue";
import MemSignup from "@/features/member/MemSignup.vue";

export default [
    {
        path : 'members',
        name : 'memberList',
        component : MemList
    },

    {
        path: "signup",
        name: "memberCreate",
        component: MemSignup,
        props: {
            adminMode: true
        },
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
