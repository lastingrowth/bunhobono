package api.bill_p;
import org.junit.jupiter.api.*; import org.junit.jupiter.api.extension.ExtendWith; import org.mockito.*; import org.mockito.junit.jupiter.MockitoExtension; import org.springframework.security.core.Authentication; import org.springframework.web.server.ResponseStatusException; import java.util.List; import static org.assertj.core.api.Assertions.*; import static org.mockito.Mockito.*;
@ExtendWith(MockitoExtension.class) class BillControllerTest {
 @Mock BillService service; @Mock Authentication auth; @InjectMocks BillController controller;
 @Test @DisplayName("UT-BE-BILL-CTRL-001 비입주민 주차 정산 목록을 조회한다") void guestCars(){ List<BillDTO>x=List.of(new BillDTO()); when(service.findParkingBills("3456",1)).thenReturn(x); assertThat(controller.guestCars("3456",1)).isSameAs(x); }
 @Test @DisplayName("UT-BE-BILL-CTRL-002 차량번호로 정산서를 생성·갱신한다") void calculate(){ BillDTO d=new BillDTO(); d.setCarNo("12가3456"); d.setKioskNo(1); BillDTO x=new BillDTO(); when(service.createOrRefreshBill("12가3456",1)).thenReturn(x); assertThat(controller.calculate(d)).isSameAs(x); }
 @Test @DisplayName("UT-BE-BILL-CTRL-003 결제를 승인한다") void confirm(){ BillDTO d=new BillDTO(); when(service.confirmPayment(d)).thenReturn(d); assertThat(controller.confirm(d)).isSameAs(d); }
 @Test @DisplayName("UT-BE-BILL-CTRL-004 로그인 입주민 정산서를 조회한다") void resident(){ when(auth.getName()).thenReturn("user"); BillDTO x=new BillDTO(); when(service.findResidentBill(1,"user")).thenReturn(x); assertThat(controller.residentBill(auth,1)).isSameAs(x); }
 @Test @DisplayName("UT-BE-BILL-CTRL-005 인증 없는 입주민 정산 조회를 거부한다") void residentUnauthorized(){ assertThatThrownBy(()->controller.residentBill(null,1)).isInstanceOf(ResponseStatusException.class); verifyNoInteractions(service); }
 @Test @DisplayName("UT-BE-BILL-CTRL-006 관리자 정산 목록을 조회한다") void adminList(){ List<BillDTO>x=List.of(); when(service.findAdminBillingList()).thenReturn(x); assertThat(controller.adminList()).isSameAs(x); }
 @Test @DisplayName("UT-BE-BILL-CTRL-007 관리자 정산 상세를 조회한다") void adminDetail(){ BillDTO x=new BillDTO(); when(service.findAdminBillingDetail(1)).thenReturn(x); assertThat(controller.adminDetail(1)).isSameAs(x); }
 @Test @DisplayName("UT-BE-BILL-CTRL-008 관리자 정산서를 수정한다") void update(){ BillDTO d=new BillDTO(); when(service.updateAdminBilling(1,d)).thenReturn(d); assertThat(controller.updateAdminBilling(1,d)).isSameAs(d); }
 @Test @DisplayName("UT-BE-BILL-CTRL-009 관리자 정산서를 지난 기록으로 이동한다") void archive(){ when(service.moveAdminBillingToTrash(1)).thenReturn(1); assertThat(controller.archiveAdminBilling(1)).isEqualTo(1); }
}
