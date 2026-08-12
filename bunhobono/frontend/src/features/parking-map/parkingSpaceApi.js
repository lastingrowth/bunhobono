import api from '@/shared/api/apiClient';

export const getParkingSpaces = (parkingCode = 'B1') => {
  return api.get('/parking-spaces', { params: { parkingCode } });
};
