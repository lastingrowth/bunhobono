import ResidentBoardView from "@/features/board/ResidentBoardView.vue";

export default [
  {
    path: "boards",
    name: "ResidentBoardList",
    component: ResidentBoardView,
  },
  {
    path: "boards/:boardNo/detail",
    name: "ResidentBoardDetail",
    component: ResidentBoardView,
  },
];
