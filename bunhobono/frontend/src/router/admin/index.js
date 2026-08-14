import MainLayout from "@/layouts/MainLayout.vue";
import vehicles from "./vehicles";
import dashboard from "./dashboard";
import members from "./members";
import carlogs from "./carlogs";
import parkings from "./parkings";
import robots from "./robots";
import gates from "./gates";
import cameraData from "./cameraData";
import notices from "./notices";
import cameras from "./cameras";
import trash from "./trash";
import memberArchive from "./memberArchive";
import statistics from "./statistics";
import boards from "./boards";
import kiosk from "./kiosk";
import inquiries from "./inquiries";
import predictiveMaintenance from "./predictiveMaintenance";
import parkingMap from "./parkingMap";
import billing from "./billing";


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
            ...robots,
            ...gates,
            ...cameras,
            ...cameraData,
            ...notices,
            ...trash,
            ...memberArchive,
            ...statistics,
            ...boards,
            ...kiosk,
            ...billing,
            ...inquiries,
            ...predictiveMaintenance,
            ...parkingMap,
        ]
    }
]
