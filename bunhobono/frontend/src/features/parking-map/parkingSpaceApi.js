import api from '@/shared/api/apiClient';

export const getParkingSpaces = (parkingCode = 'B1') => {
  return api.get('/parking-spaces', { params: { parkingCode } });
};

// 주차면을 예약한 로봇 작업 조회
export const getRobotTasks = () => {
  return api.get('/robot-tasks');
};

// 출차대기 차량 다시 입차
export const reparkVehicle = (carLogNo) => {
  return api.post('/robot-tasks/repark', null, {
    params: { carLogNo },
  });
};
