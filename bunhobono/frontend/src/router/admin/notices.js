import NoteDetail from "@/features/notice/NoteDetail.vue";
import NoteList from "@/features/notice/NoteList.vue";

export default [
  {
    path: "notice",
    name: "NoticeList",
    component: NoteList,
  },
  {
    path: 'notice/visit-long-stay',
    name: 'NoticeVisitLongStay',
    component: NoteList,
  },
  {
    path: 'notice/unknown-long-stay',
    name: 'NoticeUnknownLongStay',
    component: NoteList,
  },
  {
    path: "notice/:noticeNo",
    name: "NoticeDetail",
    component: NoteDetail,
  },
];