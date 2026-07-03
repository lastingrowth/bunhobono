import MemList from "@/components/member/MemList.vue";
import MemListDetail from "@/components/member/MemListDetail.vue";
import MemSignup from "@/components/member/MemSignup.vue";


const memRouter = [
    {
        path: "/members",
        name: "memberList",
        component: MemList,
    },
        {
        path: "/members/detail/:memberNo",
        name: "memberDetail",
        component: MemListDetail,
    },
            {
        path: "/members/signup",
        name: "memberSignup",
        component: MemSignup,
    },

];

export default memRouter;