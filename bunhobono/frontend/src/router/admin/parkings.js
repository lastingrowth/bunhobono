import ParkingsEdit from "@/features/parking/ParkingsEdit.vue";
import ParkingsList2 from "@/features/parking/ParkingsList2.vue";
import ParkingsSignup from "@/features/parking/ParkingsSignup.vue";

export default [
  {
    path: "parkings",
    name: "ParkingList",
    component: ParkingsList2,
  },
  {
    path: "parkings/signup",
    name: "ParkingSignup",
    component: ParkingsSignup,
  },
  {
    path: "parkings/:parkingNo/edit",
    name: "ParkingEdit",
    component: ParkingsEdit,
  },
];