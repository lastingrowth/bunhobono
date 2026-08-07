import ResidentInquiryView
  from "@/features/inquiry/ResidentInquiryView.vue";

export default [
  {
    path: "inquiries",
    name: "ResidentInquiryList",
    component: ResidentInquiryView
  },
  {
    path: "inquiries/write",
    name: "ResidentInquiryWrite",
    component: ResidentInquiryView
  },
  {
    path: "inquiries/:inquiryNo/detail",
    name: "ResidentInquiryDetail",
    component: ResidentInquiryView
  },
  {
    path: "inquiries/:inquiryNo/re-inquiry",
    name: "ResidentReInquiry",
    component: ResidentInquiryView
  }
];