import MainLayout from "@/layouts/MainLayout.vue";
import welcome from "./welcome";
import dashboard from "./dashboard";
import vehicles from "./vehicles";
import parkings from "./parkings";
import mypage from "./mypage";
import carlogs from "./carlogs";

export const residentRoutes = [
    {
        path : '/resident',
        component : MainLayout,
        meta : {
            requireAuth : true,
            allowedRoles : ['RESIDENT']
        },
        children : [
            ...welcome,
            ...dashboard,
            ...vehicles,
            ...parkings,
            ...mypage,
            ...carlogs,
        ]
    }
]
