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
]

export default residentRouter;