package api.member_p;

import api.a_security_config.VerificationService;
import api.apartmentunit_p.ApartmentUnitDTO;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;
import java.lang.reflect.*;
import java.util.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MemberServiceTest {
 @Mock MemberMapper mapper; @Mock PasswordEncoder passwordEncoder; @Mock VerificationService verificationService; @InjectMocks MemberService service;

 @Test @DisplayName("UT-BE-MEMBER-001 관리자 가입은 역할·비밀번호를 변환하고 전화 인증 후 저장한다")
 void signupAdmin(){ MemberDTO d=member("admin","pw","010",null,null,null); when(passwordEncoder.encode("pw")).thenReturn("enc"); when(mapper.signupAdmin(d)).thenReturn(1); service.signup(d); assertThat(d.getRole()).isEqualTo("ADMIN"); assertThat(d.getLoginPwd()).isEqualTo("enc"); verify(verificationService).consumeSignupPhoneVerification("010"); verify(verificationService,never()).consumeSignupEmailVerification(any()); }
 @Test @DisplayName("UT-BE-MEMBER-002 입주민 가입은 빈 세대와 전화·이메일 인증 후 저장한다")
 void signupResident(){ MemberDTO d=member("resident","pw","010","a@b.com",101,101); when(mapper.lockWithdrawnUnit(101,101)).thenReturn(1); when(mapper.countActiveMembersAtUnit(101,101)).thenReturn(0); when(passwordEncoder.encode("pw")).thenReturn("enc"); when(mapper.signup(d)).thenReturn(1); service.signup(d); verify(verificationService).consumeSignupEmailVerification("a@b.com"); verify(mapper).signup(d); }
 @Test @DisplayName("UT-BE-MEMBER-003 가입은 잘못된 역할·세대·저장 실패를 거부한다")
 void signupFailures(){ assertThatThrownBy(()->service.signup(member("guest","pw","010",null,null,null))).isInstanceOf(ResponseStatusException.class); assertThatThrownBy(()->service.signup(member("RESIDENT","pw","010","a",null,101))).isInstanceOf(ResponseStatusException.class); assertThatThrownBy(()->service.signup(member("RESIDENT","pw","010","a",999,9999))).isInstanceOf(ResponseStatusException.class); MemberDTO occupied=member("RESIDENT","pw","010","a",101,101); when(mapper.lockWithdrawnUnit(101,101)).thenReturn(1); when(mapper.countActiveMembersAtUnit(101,101)).thenReturn(1); assertThatThrownBy(()->service.signup(occupied)).isInstanceOf(ResponseStatusException.class); MemberDTO admin=member("ADMIN","pw","010",null,null,null); when(passwordEncoder.encode("pw")).thenReturn("enc"); when(mapper.signupAdmin(admin)).thenReturn(0); assertThatThrownBy(()->service.signup(admin)).isInstanceOf(ResponseStatusException.class); }

 @Test @DisplayName("UT-BE-MEMBER-004 가입 가능한 세대 목록을 반환한다") void availableSignupUnits(){ List<ApartmentUnitDTO> x=List.of(new ApartmentUnitDTO()); when(mapper.availableSignupUnits()).thenReturn(x); assertThat(service.availableSignupUnits()).isSameAs(x); }
 @Test @DisplayName("UT-BE-MEMBER-005 로그인 아이디 중복 여부를 반환한다") void checkLoginId(){ when(mapper.checkLoginId("user")).thenReturn(true); assertThat(service.checkLoginId("user")).isTrue(); }
 @Test @DisplayName("UT-BE-MEMBER-006 계정 복구 코드를 정규화해 발송한다") void sendRecovery(){ when(mapper.findRecoveryLoginId(any())).thenReturn("user"); service.sendAccountRecoveryCode(rec(" find_id "," phone "," 010 ",null,null,null)); verify(verificationService).sendRecoveryCode("FIND_ID","user","PHONE","010"); }
 @Test @DisplayName("UT-BE-MEMBER-007 계정 복구 코드 발송은 잘못된 요청과 미등록 회원을 거부한다") void sendRecoveryFailures(){ assertThatThrownBy(()->service.sendAccountRecoveryCode(null)).isInstanceOf(ResponseStatusException.class); assertThatThrownBy(()->service.sendAccountRecoveryCode(rec("BAD","PHONE","010",null,null,null))).isInstanceOf(ResponseStatusException.class); when(mapper.findRecoveryLoginId(any())).thenReturn(null); assertThatThrownBy(()->service.sendAccountRecoveryCode(rec("FIND_ID","PHONE","010",null,null,null))).isInstanceOf(ResponseStatusException.class); }
 @Test @DisplayName("UT-BE-MEMBER-008 아이디 찾기는 인증코드를 확인하고 아이디를 반환한다") void findLoginId(){ when(mapper.findRecoveryLoginId(any())).thenReturn("user"); assertThat(service.findAccountLoginId(rec("FIND_ID","PHONE","010",null,"123",null)).loginId()).isEqualTo("user"); verify(verificationService).verifyRecoveryCode("FIND_ID","user","PHONE","010","123"); }
 @Test @DisplayName("UT-BE-MEMBER-009 아이디 찾기는 목적 불일치와 미등록 회원을 거부한다") void findLoginIdFailures(){ assertThatThrownBy(()->service.findAccountLoginId(rec("RESET_PASSWORD","PHONE","010",null,"1",null))).isInstanceOf(ResponseStatusException.class); when(mapper.findRecoveryLoginId(any())).thenReturn(null); assertThatThrownBy(()->service.findAccountLoginId(rec("FIND_ID","PHONE","010",null,"1",null))).isInstanceOf(ResponseStatusException.class); }
 @Test @DisplayName("UT-BE-MEMBER-010 비밀번호 재설정은 인증 후 새 암호를 저장한다") void resetPassword(){ MemberDTO.AccountRecoveryRequest r=rec("RESET_PASSWORD","EMAIL","a@b.com","user","123","Newpass1!"); when(mapper.findRecoveryLoginId(any())).thenReturn("user"); when(mapper.findRecoveryPassword("user")).thenReturn("old"); when(passwordEncoder.matches("Newpass1!","old")).thenReturn(false); when(passwordEncoder.encode("Newpass1!")).thenReturn("enc"); when(mapper.updateRecoveredPassword("user","enc")).thenReturn(1); service.resetAccountPassword(r); verify(verificationService).verifyRecoveryCode("RESET_PASSWORD","user","EMAIL","a@b.com","123"); }
 @Test @DisplayName("UT-BE-MEMBER-011 비밀번호 재설정은 입력·회원·기존암호·갱신 오류를 거부한다") void resetPasswordFailures(){ assertThatThrownBy(()->service.resetAccountPassword(rec("FIND_ID","EMAIL","a","user","1","Newpass1!"))).isInstanceOf(ResponseStatusException.class); assertThatThrownBy(()->service.resetAccountPassword(rec("RESET_PASSWORD","EMAIL","a"," ","1","Newpass1!"))).isInstanceOf(ResponseStatusException.class); assertThatThrownBy(()->service.resetAccountPassword(rec("RESET_PASSWORD","EMAIL","a","user","1","short"))).isInstanceOf(ResponseStatusException.class); MemberDTO.AccountRecoveryRequest r=rec("RESET_PASSWORD","EMAIL","a","user","1","Newpass1!"); when(mapper.findRecoveryLoginId(any())).thenReturn("user"); when(mapper.findRecoveryPassword("user")).thenReturn("old"); when(passwordEncoder.matches("Newpass1!","old")).thenReturn(true); assertThatThrownBy(()->service.resetAccountPassword(r)).isInstanceOf(ResponseStatusException.class); }

 @Test @DisplayName("UT-BE-MEMBER-012 전체 회원 목록을 반환한다") void list(){ List<MemberDTO> x=List.of(new MemberDTO()); when(mapper.list()).thenReturn(x); assertThat(service.list()).isSameAs(x); }
 @Test @DisplayName("UT-BE-MEMBER-013 회원 상세를 반환한다") void detail(){ MemberDTO x=new MemberDTO(); when(mapper.detail(1)).thenReturn(x); assertThat(service.detail(1)).isSameAs(x); }
 @Test @DisplayName("UT-BE-MEMBER-014 허용된 조건으로 검색하고 잘못된 조건은 전체 조회한다") void search(){ when(mapper.search("name","kim",101,101)).thenReturn(List.of(new MemberDTO())); assertThat(service.search("name","kim",101,101)).hasSize(1); when(mapper.list()).thenReturn(List.of()); assertThat(service.search("bad","x",null,null)).isEmpty(); }
 @Test @DisplayName("UT-BE-MEMBER-015 회원 수정은 빈 비밀번호를 유지하고 새 비밀번호는 암호화한다") void updatePasswords(){ MemberDTO saved=saved(1,"resident","RESIDENT","old"); when(mapper.detail(1)).thenReturn(saved); MemberDTO blank=edit(1," ","ACTIVE"); service.update(blank,"admin"); assertThat(blank.getLoginPwd()).isEqualTo("old"); MemberDTO changed=edit(1,"new","ACTIVE"); when(passwordEncoder.encode("new")).thenReturn("enc"); service.update(changed,"admin"); assertThat(changed.getLoginPwd()).isEqualTo("enc"); }
 @Test @DisplayName("UT-BE-MEMBER-016 회원 수정은 미존재 회원과 관리자 본인 퇴사를 거부한다") void updateFailures(){ MemberDTO d=edit(1,null,"ACTIVE"); when(mapper.detail(1)).thenReturn(null); assertThatThrownBy(()->service.update(d,"admin")).isInstanceOf(ResponseStatusException.class); when(mapper.detail(1)).thenReturn(saved(1,"admin","ADMIN","old")); d.setMemStatus("INACTIVE"); assertThatThrownBy(()->service.update(d,"admin")).isInstanceOf(ResponseStatusException.class); }
 @Test @DisplayName("UT-BE-MEMBER-017 입주민 전출 수정은 이력·차량 처리 후 즉시 확정한다") void updateWithdrawal(){ MemberDTO d=edit(1,null,"WITHDRAW_PENDING"); when(mapper.detail(1)).thenReturn(saved(1,"resident","RESIDENT","old")); when(mapper.delete(1)).thenReturn(1); service.update(d,"admin"); verify(mapper).requestWithdrawnMember(1); verify(mapper).saveMemberArchive(1); verify(mapper).deleteVehiclesByMemberNo(1); verify(mapper,never()).update(any()); }
 @Test @DisplayName("UT-BE-MEMBER-018 선택 회원을 승인하며 빈 선택은 거부한다") void approve(){ service.approvePendingMembers(List.of(1)); verify(mapper).approvePendingMembers(List.of(1)); assertThatThrownBy(()->service.approvePendingMembers(null)).isInstanceOf(IllegalArgumentException.class); assertThatThrownBy(()->service.approvePendingMembers(List.of())).isInstanceOf(IllegalArgumentException.class); }
 @Test @DisplayName("UT-BE-MEMBER-019 관리자는 다른 회원을 전출 신청 처리한다") void delete(){ when(mapper.detail(1)).thenReturn(saved(1,"resident","RESIDENT","old")); when(mapper.requestWithdrawnMember(1)).thenReturn(1); service.delete(1,"admin"); verify(mapper).requestWithdrawnMember(1); }
 @Test @DisplayName("UT-BE-MEMBER-020 전출 신청은 미존재·본인·갱신 실패를 거부한다") void deleteFailures(){ when(mapper.detail(1)).thenReturn(null); assertThatThrownBy(()->service.delete(1,"admin")).isInstanceOf(ResponseStatusException.class); when(mapper.detail(1)).thenReturn(saved(1,"admin","ADMIN","old")); assertThatThrownBy(()->service.delete(1,"admin")).isInstanceOf(ResponseStatusException.class); when(mapper.detail(1)).thenReturn(saved(1,"resident","RESIDENT","old")); when(mapper.requestWithdrawnMember(1)).thenReturn(0); assertThatThrownBy(()->service.delete(1,"admin")).isInstanceOf(ResponseStatusException.class); }

 @Test @DisplayName("UT-BE-MEMBER-021 전출 회원 한 명을 복원한다") void restoreOne(){ when(mapper.detail(1)).thenReturn(new MemberDTO()); when(mapper.restoreWithdrawnMember(1)).thenReturn(1); service.restoreWithdrawnMember(1); }
 @Test @DisplayName("UT-BE-MEMBER-022 전출 복원은 미존재 회원과 갱신 실패를 거부한다") void restoreOneFailures(){ when(mapper.detail(1)).thenReturn(null); assertThatThrownBy(()->service.restoreWithdrawnMember(1)).isInstanceOf(ResponseStatusException.class); when(mapper.detail(1)).thenReturn(new MemberDTO()); assertThatThrownBy(()->service.restoreWithdrawnMember(1)).isInstanceOf(ResponseStatusException.class); }
 @Test @DisplayName("UT-BE-MEMBER-023 회원 이력과 차량을 처리하고 전출을 확정한다") void confirmOne(){ when(mapper.detail(1)).thenReturn(new MemberDTO()); when(mapper.delete(1)).thenReturn(1); service.confirmWithdrawnMember(1); verify(mapper).saveMemberArchive(1); verify(mapper).deleteVehiclesByMemberNo(1); }
 @Test @DisplayName("UT-BE-MEMBER-024 전출 확정은 미존재 회원과 갱신 실패를 거부한다") void confirmOneFailures(){ when(mapper.detail(1)).thenReturn(null); assertThatThrownBy(()->service.confirmWithdrawnMember(1)).isInstanceOf(ResponseStatusException.class); when(mapper.detail(1)).thenReturn(new MemberDTO()); assertThatThrownBy(()->service.confirmWithdrawnMember(1)).isInstanceOf(ResponseStatusException.class); }
 @Test @DisplayName("UT-BE-MEMBER-025 여러 전출 회원을 복원하고 처리 수를 반환한다") void restoreMany(){ when(mapper.detail(anyInt())).thenReturn(new MemberDTO()); when(mapper.restoreWithdrawnMember(anyInt())).thenReturn(1); assertThat(service.restoreWithdrawnMembers(List.of(1,2))).isEqualTo(2); }
 @Test @DisplayName("UT-BE-MEMBER-026 일괄 복원은 빈 선택을 거부한다") void restoreManyEmpty(){ assertThatThrownBy(()->service.restoreWithdrawnMembers(null)).isInstanceOf(IllegalArgumentException.class); assertThatThrownBy(()->service.restoreWithdrawnMembers(List.of())).isInstanceOf(IllegalArgumentException.class); }
 @Test @DisplayName("UT-BE-MEMBER-027 여러 전출 회원을 확정하고 처리 수를 반환한다") void confirmMany(){ when(mapper.detail(anyInt())).thenReturn(new MemberDTO()); when(mapper.delete(anyInt())).thenReturn(1); assertThat(service.permanentlyDeleteWithdrawnMembers(List.of(1,2))).isEqualTo(2); verify(mapper,times(2)).saveMemberArchive(anyInt()); }
 @Test @DisplayName("UT-BE-MEMBER-028 일괄 전출 확정은 빈 선택을 거부한다") void confirmManyEmpty(){ assertThatThrownBy(()->service.permanentlyDeleteWithdrawnMembers(null)).isInstanceOf(IllegalArgumentException.class); assertThatThrownBy(()->service.permanentlyDeleteWithdrawnMembers(List.of())).isInstanceOf(IllegalArgumentException.class); }

 @Test @DisplayName("UT-BE-MEMBER-029 마이페이지 회원의 비밀번호를 숨기고 미존재 시 null을 반환한다") void mypage(){ MemberDTO d=new MemberDTO(); d.setLoginPwd("secret"); when(mapper.residentMypage("user")).thenReturn(d); assertThat(service.residentMypage("user").getLoginPwd()).isNull(); when(mapper.residentMypage("none")).thenReturn(null); assertThat(service.residentMypage("none")).isNull(); }
 @Test @DisplayName("UT-BE-MEMBER-030 대시보드에 회원·차량·입출차를 집계한다") void dashboard(){ MemberDTO d=new MemberDTO(); List<MemberDTO.ResidentVehicle> v=List.of(new MemberDTO.ResidentVehicle()); List<MemberDTO.ResidentCarLog> l=List.of(new MemberDTO.ResidentCarLog()); when(mapper.residentMypage("user")).thenReturn(d); when(mapper.residentVehicles("user")).thenReturn(v); when(mapper.residentCarLogs("user")).thenReturn(l); MemberDTO.ResidentDashboard result=service.residentDashboard("user"); assertThat(result.getMember()).isSameAs(d); assertThat(result.getVehicles()).isSameAs(v); assertThat(result.getRecentCarLogs()).isSameAs(l); }
 @Test @DisplayName("UT-BE-MEMBER-031 마이페이지의 변경 이메일을 인증하고 새 비밀번호를 암호화한다")
 void editMypage(){
   MemberDTO d=new MemberDTO(); d.setLoginId("user"); d.setEmail(" new@a.com "); d.setLoginPwd("new"); d.setMemPhone("010-0000-0000");
   when(mapper.findResidentPhone("user")).thenReturn("01000000000");
   when(mapper.findResidentEmail("user")).thenReturn("old@a.com"); when(passwordEncoder.encode("new")).thenReturn("enc");
   service.residentMypageEdit(d);
   assertThat(d.getEmail()).isEqualTo("new@a.com"); assertThat(d.getLoginPwd()).isEqualTo("enc");
   verify(verificationService).consumeSignupEmailVerification("new@a.com");
   verify(verificationService,never()).consumeSignupPhoneVerification(any()); verify(mapper).residentMypageEdit(d);
 }
 @Test @DisplayName("UT-BE-MEMBER-032 같은 이메일과 빈 비밀번호는 재인증·암호화하지 않으며 빈 이메일은 거부한다")
 void editMypageBoundaries(){
   MemberDTO same=new MemberDTO(); same.setLoginId("user"); same.setEmail(" USER@A.COM "); same.setLoginPwd(" "); same.setMemPhone("010-0000-0000");
   when(mapper.findResidentPhone("user")).thenReturn("01000000000"); when(mapper.findResidentEmail("user")).thenReturn("user@a.com");
   service.residentMypageEdit(same);
   assertThat(same.getLoginPwd()).isNull(); verifyNoInteractions(passwordEncoder,verificationService);
   MemberDTO blank=new MemberDTO(); blank.setLoginId("user"); blank.setEmail(" "); blank.setMemPhone("01000000000");
   assertThatThrownBy(()->service.residentMypageEdit(blank)).isInstanceOf(ResponseStatusException.class).hasMessageContaining("이메일");
 }

 @Test @DisplayName("UT-BE-MEMBER-040 변경된 연락처는 전화 인증 후 공백을 제거해 저장한다")
 void changedPhoneRequiresVerification(){
   MemberDTO d=new MemberDTO(); d.setLoginId("user"); d.setMemPhone(" 010-1111-2222 "); d.setEmail("user@a.com");
   when(mapper.findResidentPhone("user")).thenReturn("01000000000"); when(mapper.findResidentEmail("user")).thenReturn("user@a.com");
   service.residentMypageEdit(d);
   verify(verificationService).consumeSignupPhoneVerification("010-1111-2222");
   assertThat(d.getMemPhone()).isEqualTo("010-1111-2222"); verify(mapper).residentMypageEdit(d);
 }

 @Test @DisplayName("UT-BE-MEMBER-033 PNG 보안문자와 식별자·만료시간을 발급한다") void issueCaptcha(){ Map<String,String> r=service.issueSecurityChallenge(); assertThat(r.get("challengeId")).isNotBlank(); assertThat(r.get("imageData")).startsWith("data:image/png;base64,"); assertThat(r.get("expiresIn")).isEqualTo("180"); }
 @Test @DisplayName("UT-BE-MEMBER-034 탈퇴 사전 확인은 보안문자와 현재 비밀번호를 검증한다") void verifyWithdrawal() throws Exception { Challenge c=challenge(); stubCurrent(); service.verifyResidentWithdrawal("user","current",c.id,c.answer); verify(mapper).findResPw("user"); }
 @Test @DisplayName("UT-BE-MEMBER-035 탈퇴 사전 확인은 누락·오답 보안문자와 잘못된 계정을 거부한다") void verifyWithdrawalFailures() throws Exception { assertThatThrownBy(()->service.verifyResidentWithdrawal("user","pw",null,"x")).isInstanceOf(ResponseStatusException.class); Challenge c=challenge(); assertThatThrownBy(()->service.verifyResidentWithdrawal("user","pw",c.id,"WRONG")).isInstanceOf(ResponseStatusException.class); Challenge c2=challenge(); when(mapper.findResPw("user")).thenReturn(null); assertThatThrownBy(()->service.verifyResidentWithdrawal("user","pw",c2.id,c2.answer)).isInstanceOf(ResponseStatusException.class); }
 @Test @DisplayName("UT-BE-MEMBER-036 입주민 탈퇴는 검증 후 전출 대기로 변경한다") void residentDelete(){ Challenge c=uncheckedChallenge(); stubCurrent(); when(mapper.residentDelete("user")).thenReturn(1); service.residentDelete("user","current",c.id,c.answer); verify(mapper).residentDelete("user"); }
 @Test @DisplayName("UT-BE-MEMBER-037 입주민 탈퇴는 상태 변경 실패를 거부한다") void residentDeleteFailure(){ Challenge c=uncheckedChallenge(); stubCurrent(); when(mapper.residentDelete("user")).thenReturn(0); assertThatThrownBy(()->service.residentDelete("user","current",c.id,c.answer)).isInstanceOf(ResponseStatusException.class); }
 @Test @DisplayName("UT-BE-MEMBER-038 입주민 비밀번호를 검증 후 새 암호로 변경한다") void changePassword(){ Challenge c=uncheckedChallenge(); stubCurrent(); when(passwordEncoder.matches("new","old")).thenReturn(false); when(passwordEncoder.encode("new")).thenReturn("enc"); when(mapper.changeResidentPassword("user","enc")).thenReturn(1); service.changeResidentPassword("user","current","new",c.id,c.answer); verify(mapper).changeResidentPassword("user","enc"); }
 @Test @DisplayName("UT-BE-MEMBER-039 비밀번호 변경은 빈 값·현재 암호 재사용·갱신 실패를 거부한다") void changePasswordFailures(){ Challenge a=uncheckedChallenge(); stubCurrent(); assertThatThrownBy(()->service.changeResidentPassword("user","current"," ",a.id,a.answer)).isInstanceOf(ResponseStatusException.class); Challenge b=uncheckedChallenge(); stubCurrent(); when(passwordEncoder.matches("same","old")).thenReturn(true); assertThatThrownBy(()->service.changeResidentPassword("user","current","same",b.id,b.answer)).isInstanceOf(ResponseStatusException.class); Challenge c=uncheckedChallenge(); stubCurrent(); when(passwordEncoder.matches("new","old")).thenReturn(false); when(passwordEncoder.encode("new")).thenReturn("enc"); assertThatThrownBy(()->service.changeResidentPassword("user","current","new",c.id,c.answer)).isInstanceOf(ResponseStatusException.class); }

 private void stubCurrent(){ when(mapper.findResPw("user")).thenReturn("old"); when(passwordEncoder.matches("current","old")).thenReturn(true); }
 private MemberDTO member(String role,String pwd,String phone,String email,Integer dong,Integer ho){ MemberDTO d=new MemberDTO(); d.setRole(role); d.setLoginPwd(pwd); d.setMemPhone(phone); d.setEmail(email); d.setDong(dong); d.setHo(ho); return d; }
 private MemberDTO saved(int no,String id,String role,String pwd){ MemberDTO d=new MemberDTO(); d.setMemberNo(no); d.setLoginId(id); d.setRole(role); d.setLoginPwd(pwd); return d; }
 private MemberDTO edit(int no,String pwd,String status){ MemberDTO d=new MemberDTO(); d.setMemberNo(no); d.setLoginPwd(pwd); d.setMemStatus(status); return d; }
 private MemberDTO.AccountRecoveryRequest rec(String purpose,String channel,String contact,String id,String code,String newPwd){ return new MemberDTO.AccountRecoveryRequest(purpose,channel,contact,"김입주",101,101,id,code,newPwd); }
 private Challenge uncheckedChallenge(){ try{return challenge();}catch(Exception e){throw new RuntimeException(e);} }
 private Challenge challenge() throws Exception { Map<String,String> issued=service.issueSecurityChallenge(); Field f=MemberService.class.getDeclaredField("captchaStore"); f.setAccessible(true); Object data=((Map<?,?>)f.get(service)).get(issued.get("challengeId")); Method m=data.getClass().getDeclaredMethod("answer"); m.setAccessible(true); return new Challenge(issued.get("challengeId"),(String)m.invoke(data)); }
 private record Challenge(String id,String answer){}
}
