import api from '@/shared/api/apiClient';

export const getSavedCameraPdm = () => api.get('/camera-pdm');

export const getLatestCameraPdm = () => api.get('/camera-pdm/latest');

export const getRecentCameraPdm = (cameraNo) =>
  api.get(`/camera-pdm/recent/${cameraNo}`);

export const analyzeAllCameras = () => api.post('/camera-pdm/analyze-all');

export const getSavedGatePdm = () => api.get('/gate-pdm');

export const getLatestGatePdm = () => api.get('/gate-pdm/latest');

export const getRecentGatePdm = (gateNo) =>
  api.get(`/gate-pdm/recent/${gateNo}`);

export const analyzeAllGates = () => api.post('/gate-pdm/analyze-all');
