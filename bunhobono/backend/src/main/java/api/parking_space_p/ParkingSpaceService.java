package api.parking_space_p;

import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class ParkingSpaceService {

    @Resource
    private ParkingSpaceMapper parkingSpaceMapper;

    // 주차장에 등록된 공간과 현재 차량을 조회한다.
    public List<ParkingSpaceDTO> list(String parkingCode) {
        return parkingSpaceMapper.list(parkingCode);
    }

    // 해당 게이트의 빈 입차·출차 대기면을 찾는다.
    public ParkingSpaceDTO findEmptyWaitingSpace(
            int gateNo,
            String spaceType
    ) {
        return parkingSpaceMapper.findEmptyWaitingSpace(
                gateNo,
                spaceType
        );
    }

    // 비어 있고 작업에 배정되지 않은 주차면을 찾는다.
    public ParkingSpaceDTO findEmptyParkingSpace(
            int parkingNo,
            int entryGateNo
    ) {
        return parkingSpaceMapper.findEmptyParkingSpace(
                parkingNo,
                entryGateNo
        );
    }

    // 차량 입출차 기록으로 현재 차량 위치를 찾는다.
    public ParkingSpaceDTO findByCarLogNo(int carLogNo) {
        return parkingSpaceMapper.findByCarLogNo(
                carLogNo
        );
    }

    // 출차대기면에서 10분이 지난 미출차 차량 조회
    public List<Integer> findTimedOutExitWaitCarLogNos() {
        return parkingSpaceMapper.findTimedOutExitWaitCarLogNos();
    }

    // 빈 공간에 차량을 배정한다.
    public int assignCarLog(
            long spaceNo,
            int carLogNo
    ) {
        return parkingSpaceMapper.assignCarLog(
                spaceNo,
                carLogNo
        );
    }

    // 차량이 떠난 공간을 빈자리로 변경한다.
    public int releaseCarLog(
            long spaceNo,
            int carLogNo
    ) {
        return parkingSpaceMapper.releaseCarLog(
                spaceNo,
                carLogNo
        );
    }

    //내 차량 조회
    public List<ParkingSpaceDTO> myVehicleLocations(
            String loginId
    ) {
        return parkingSpaceMapper.myVehicleLocations(loginId);
    }
}
