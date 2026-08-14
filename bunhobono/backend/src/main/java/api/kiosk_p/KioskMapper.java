package api.kiosk_p;

import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface KioskMapper {

    // 키오스크 전체 목록 조회
    @Select("SELECT ROW_NUMBER() OVER (ORDER BY k.kiosk_no) AS display_no, " +
            "k.kiosk_no, k.parking_no, p.parking_name, " +
            "k.model_name, k.kiosk_location, k.install_date " +
            "FROM kiosk k " +
            "LEFT JOIN parking p ON k.parking_no = p.parking_no " +
            "ORDER BY k.kiosk_no")
    List<KioskDTO> list();

    // 키오스크 번호로 키오스크가 설치된 주차장 정보를 조회
    @Select("SELECT k.kiosk_no, k.parking_no, p.parking_name, " +
            "k.model_name, k.kiosk_location, k.install_date " +
            "FROM kiosk k " +
            "JOIN parking p ON p.parking_no = k.parking_no " +
            "WHERE k.kiosk_no = #{kioskNo}")
    KioskDTO findByKioskNo(int kioskNo);

    // 키오스크 삭제
    @Delete("DELETE FROM kiosk WHERE kiosk_no = #{kioskNo}")
    int delete(int kioskNo);

    // 키오스크 등록
    @Insert("INSERT INTO kiosk (parking_no, model_name, kiosk_location, install_date) " +
            "VALUES (#{parkingNo}, #{modelName}, #{kioskLocation}, #{installDate})")
    int signUp(KioskDTO kioskDTO);
}
