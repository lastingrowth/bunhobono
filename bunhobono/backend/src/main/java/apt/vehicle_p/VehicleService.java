package apt.vehicle_p;

import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class VehicleService {

    @Resource
    VehicleMapper vehicleMapper;

    public List<VehicleDTO> list() { return vehicleMapper.list(); }
}