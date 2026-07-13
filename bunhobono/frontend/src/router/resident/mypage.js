import Mypage from "@/features/member/Mypage.vue";
import ResidentEdit from "@/features/member/ResidentEdit.vue";

export default [
  {
    path: "mypage",
    name: "ResidentMypage",
    component: Mypage,
  },
  {
    path: "mypage/edit",
    name: "ResidentMypageEdit",
    component: ResidentEdit,
  },
];