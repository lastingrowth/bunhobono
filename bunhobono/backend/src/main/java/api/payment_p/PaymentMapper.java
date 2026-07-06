package api.payment_p;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface PaymentMapper {

    @Select("select * from parking_payment order by payment_no")
    List<PaymentDTO> list(PaymentDTO dto);
}
