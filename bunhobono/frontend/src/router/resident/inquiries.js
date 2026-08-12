import ResidentFaqView
  from "@/features/faq/ResidentFaqView.vue";

import ResidentInquiryView
  from "@/features/inquiry/ResidentInquiryView.vue";

export default [
  // 자주하는 질문 기본 화면
  {
    path: "inquiries",
    name: "ResidentFaqList",
    component: ResidentFaqView
  },

  // 입주민 문의 목록
  {
    path: "inquiries/my",
    name: "ResidentInquiryList",
    component: ResidentInquiryView
  },

  // 입주민 문의 작성
  {
    path: "inquiries/write",
    name: "ResidentInquiryWrite",
    component: ResidentInquiryView
  },

  // 입주민 문의 상세 조회
  {
    path: "inquiries/:inquiryNo/detail",
    name: "ResidentInquiryDetail",
    component: ResidentInquiryView
  },

  // 입주민 재문의 작성
  {
    path: "inquiries/:inquiryNo/re-inquiry",
    name: "ResidentReInquiry",
    component: ResidentInquiryView
  }
];