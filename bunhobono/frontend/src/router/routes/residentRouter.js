import ParkingsList from "@/features/parking/ParkingsList.vue";
import ResidentView from "@/views/resident/ResidentView.vue";

const residentMeta = {
    requireAuth : true,
    roles : ['RESIDENT']
}

const residentRouter = [

    {
        path : 'resident',
        name : 'residentMain',
        component : ResidentView,
        meta : residentMeta
    },
    {
        path: "/parkings",
        children: [
          {
            path: "list",      
            name: "parkingslist",
            component: ParkingsList,
          },
        ],
    },
    

    
]

export default residentRouter;