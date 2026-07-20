import MainLayout from "@/layouts/MainLayout.vue";
import vehicles from "./vehicles";
import dashboard from "./dashboard";
import members from "./members";
import carlogs from "./carlogs";
import parkings from "./parkings";
import gates from "./gates";
import cameraData from "./cameraData";
import notices from "./notices";
import cameras from "./cameras";
import trash from "./trash";
import memberArchive from "./memberArchive";

export const adminRoutes = [
    {
        path : '/admin',
        component : MainLayout,
        redirect : '/admin/dashboard',
        meta : {
            requireAuth : true,
            allowedRoles : ['ADMIN']
        },
        children : [
            ...dashboard,
            ...members,
            ...carlogs,
            ...vehicles,
            ...parkings,
            ...gates,
            ...cameras,
            ...cameraData,
            ...notices,
            ...trash,
            ...memberArchive
        ]
    }
]