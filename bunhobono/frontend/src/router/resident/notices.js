import NoteDetail from "@/features/notice/NoteDetail.vue";
import NoteList from "@/features/notice/NoteList.vue";

export default [
  {
    path: "notice",
    name: "ResidentNoticeList",
    component: NoteList,
  },
  {
    path: "notice/:noticeNo",
    name: "ResidentNoticeDetail",
    component: NoteDetail,
  },
];