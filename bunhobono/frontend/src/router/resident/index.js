import MainLayout from "@/layouts/MainLayout.vue";
import dashboard from "./dashboard";
import vehicles from "./vehicles";
import parkings from "./parkings";
import mypage from "./mypage";

export const residentRoutes = [
    {
        path : '/resident',
        component : MainLayout,
        redirect : '/resident/dashboard',
        meta : {
            requireAuth : true,
            allowedRoles : ['RESIDENT']
        },
        children : [
            ...dashboard,
            ...vehicles,
            ...parkings,
            ...mypage
        ]
    }
]
