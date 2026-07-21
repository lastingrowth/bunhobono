import VehicleView from "@/features/vehicle/VehicleView.vue";

export default [
  {
    path: "vehicles",
    name: "VehicleList",
    component: VehicleView,
  },
  {
    path: 'vehicles/parked-expired',
    name: 'ParkedExpiredVehicleList',
    component: VehicleView,
  },
];