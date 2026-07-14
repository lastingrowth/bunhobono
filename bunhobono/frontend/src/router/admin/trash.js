import TrashList from "@/features/trash/TrashList.vue";
import TrashDetail from "@/features/trash/TrashDetail.vue";

export default [
    {
        path: "trash",
        name: "TrashList",
        component: TrashList,
    },
    {
        path: "trash/:trashNo",
        name: "TrashDetail",
        component: TrashDetail,
    },
];