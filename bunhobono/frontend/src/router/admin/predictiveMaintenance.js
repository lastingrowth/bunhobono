import PredictiveMaintenanceView from '@/features/predictive-maintenance/PredictiveMaintenanceView.vue';
import CameraPdmDetail from '@/features/predictive-maintenance/CameraPdmDetail.vue';
import GatePdmDetail from '@/features/predictive-maintenance/GatePdmDetail.vue';

export default [
  {
    path: 'predictive-maintenance',
    name: 'AdminPredictiveMaintenance',
    component: PredictiveMaintenanceView,
  },
  {
    path: 'predictive-maintenance/cameras/:cameraNo',
    name: 'AdminCameraPdmDetail',
    component: CameraPdmDetail,
  },
  {
    path: 'predictive-maintenance/gates/:gateNo',
    name: 'AdminGatePdmDetail',
    component: GatePdmDetail,
  },
];
