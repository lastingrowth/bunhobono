package api.gate_p;


import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface GateMapper {

    //리스트 전체보기
    @Select("SELECT ROW_NUMBER() OVER (ORDER BY gate_no) AS display_no, " +
            "       gate_no, parking_no, gate_name, gate_type " +
            "FROM gate " +
            "ORDER BY gate_no")
    List<GateDTO> list(GateDTO dto);

    //생성
    @Insert("INSERT INTO gate (parking_no, gate_name, gate_type) " +
            "VALUES (#{parkingNo}, #{gateName}, #{gateType})")
//    @Options(useGeneratedKeys = true, keyProperty = "gateNo")  //시리얼자동증가값 필요하면 쓸것
    int insert(GateDTO dto);

    //디테일
    @Select("SELECT * FROM ( " +
            "    SELECT ROW_NUMBER() OVER (ORDER BY g.gate_no) AS display_no, " +
            "           g.gate_no, g.gate_name, g.gate_type, g.parking_no, " +
            "           p.parking_name, p.parking_location " +
            "    FROM gate g " +
            "    JOIN parking p ON g.parking_no = p.parking_no " +
            ") t " +
            "WHERE t.gate_no = #{gateNo}")
    GateDTO detail(int gateNo);

    //삭제
    @Delete("DELETE FROM gate WHERE gate_no = #{gateNo}")
    int delete(int gateNo);

    //수정
    @Update("UPDATE gate " +
            "SET parking_no = #{parkingNo}, " +
            "    gate_name = #{gateName}, " +
            "    gate_type = #{gateType} " +
            "WHERE gate_no = #{gateNo}")
    int update(GateDTO dto);
}


