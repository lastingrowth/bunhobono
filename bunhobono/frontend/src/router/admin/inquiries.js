import AdminFaqView from "@/features/faq/AdminFaqView.vue";
import AdminInquiryView from "@/features/inquiry/AdminInquiryView.vue";

export default [
  {
    path: "inquiries",
    name: "AdminInquiryList",
    component: AdminInquiryView,
  },
  {
    path: "inquiries/:inquiryNo/detail",
    name: "AdminInquiryDetail",
    component: AdminInquiryView,
  },
  {
    path: "inquiries/faqs",
    name: "AdminFaqList",
    component: AdminFaqView,
  },
];
