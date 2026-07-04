package api.gate_p;


import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface GateMapper {

    @Select("select * from gate order by gate_no")
    List<GateDTO> list(GateDTO dto);

    @Insert("INSERT INTO gate (parking_no, gate_name, gate_type) " +
            "VALUES (#{parkingNo}, #{gateName}, #{gateType})")
//    @Options(useGeneratedKeys = true, keyProperty = "gateNo")  //시리얼자동증가값 필요하면 쓸것
    int insert(GateDTO dto);

    @Select("SELECT * FROM gate WHERE gate_no = #{gateNo}")
    GateDTO findById(int gateNo);
}


