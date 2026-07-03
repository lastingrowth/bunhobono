<<<<<<<< HEAD:bunhobono/frontend/src/router/routes/memRouter.js
import MemList from "@/components/member/MemList.vue";
import MemListDetail from "@/components/member/MemListDetail.vue";
import MemSignup from "@/components/member/MemSignup.vue";

========
import MemList from "@/views/member/MemList.vue";
import MemListDetail from "@/views/member/MemListDetail.vue";
import MemModify from "@/views/member/MemModify.vue";
import MemSignup from "@/views/member/MemSignup.vue";
>>>>>>>> origin/psh:bunhobono/frontend/src/router/memRouter.js

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