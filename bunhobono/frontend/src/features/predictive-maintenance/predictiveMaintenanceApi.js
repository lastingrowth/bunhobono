import api from '@/shared/api/apiClient';

export const getSavedCameraPdm = () => api.get('/camera-pdm');

export const getLatestCameraPdm = () => api.get('/camera-pdm/latest');

export const getRecentCameraPdm = (cameraNo) =>
  api.get(`/camera-pdm/recent/${cameraNo}`);

export const analyzeAllCameras = () => api.post('/camera-pdm/analyze-all');

export const completeCameraPdmAction = (pdmNo, actionNote) =>
  api.patch(`/camera-pdm/${pdmNo}/complete-action`, { actionNote });

export const getSavedGatePdm = () => api.get('/gate-pdm');

export const getLatestGatePdm = () => api.get('/gate-pdm/latest');

export const getRecentGatePdm = (gateNo) =>
  api.get(`/gate-pdm/recent/${gateNo}`);

export const analyzeAllGates = () => api.post('/gate-pdm/analyze-all');

export const completeGatePdmAction = (pdmNo, actionNote) =>
  api.patch(`/gate-pdm/${pdmNo}/complete-action`, { actionNote });

export const getSavedRobotPdm = () => api.get('/robot-pdm');

export const getLatestRobotPdm = () => api.get('/robot-pdm/latest');

export const getRecentRobotPdm = (robotNo) =>
  api.get(`/robot-pdm/recent/${robotNo}`);

export const analyzeAllRobots = () => api.post('/robot-pdm/analyze-all');

export const completeRobotPdmAction = (pdmNo, actionNote) =>
  api.patch(`/robot-pdm/${pdmNo}/complete-action`, { actionNote });
