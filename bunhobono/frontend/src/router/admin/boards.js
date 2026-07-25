import BoardAdminView from "@/features/board/BoardAdminView.vue";

export default [
  {
    path: "boards",
    name: "BoardList",
    component: BoardAdminView,
  },
  {
    path: "boards/signUp",
    name: "BoardCreate",
    component: BoardAdminView,
  },
  {
    path: "boards/:boardNo/detail",
    name: "BoardDetail",
    component: BoardAdminView,
  },
  {
    path: "boards/:boardNo/edit",
    name: "BoardEdit",
    component: BoardAdminView,
  },
];
