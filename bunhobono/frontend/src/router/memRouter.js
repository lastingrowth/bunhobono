import MemList from "@/views/member/MemList.vue";
import MemListDetail from "@/views/member/MemListDetail.vue";
import MemModify from "@/views/member/MemModify.vue";
import MemSignup from "@/views/member/MemSignup.vue";

const memRouter = [
    {
        path: "/members",
        name: "memberList",
        component: MemList,
    },
        {
    {
        path: "/members/detail/:memberNo",
        name: "memberDetail",
        component: MemListDetail,
    },
            {
    {
        path: "/members/signup",
        name: "memberSignup",
        component: MemSignup,
    },
    {
        path: "/members/detail/:memberNo/modify",
        name: "memberModify",
        component: MemModify
    },

];

export default memRouter;