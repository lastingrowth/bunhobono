import api from "@/shared/api/apiClient";

// 회원 전체 조회
export const getMemberList = () => {
    return api.get("/members");
};

// 선택한 전출 신청 회원을 거주 상태로 복원
export const restoreWithdrawnMembers = (memberNos) => {
    return api.put('/members/restore', memberNos);
};

// 선택한 전출 신청 회원을 전출 확정 처리
// 실제 member 삭제가 아니라 member_archive에 보관 후 member 원본을 미등록 상태로 비움
export const permanentlyDeleteWithdrawnMembers = (memberNos) => {
    return api.delete('/members/withdrawn', { data: memberNos });
};

// 회원 검색
export const searchMember = (params) => {
    return api.get("/members/search", {params,});
} ;

// 회원 상세 조회
export const getMemberDetail = (memberNo) => {
    return api.get(`/members/${memberNo}/detail`);
};

// 회원 수정
export const updateMember = (memberNo, data) => {
    return api.put(`/members/${memberNo}/edit`, data);
};

// 선택한 승인 대기 회원의 역할을 입주민으로 변경한다.
export const approvePendingMembers = (memberNos) => {
    return api.put('/members/approve', memberNos);
};

// 회원 삭제
export const deleteMember = (memberNo) => {
    return api.delete(`/members/${memberNo}/delete`);
};

// 회원 등록
export const signupMember = (member) => {
    return api.post("/members", member);
};

// 입주민 로그인 시, 마이페이지
export const residentMypage = () => {
    return api.get(`/resident/mypage`);
};

// 입주민 로그인 시, 직접 회원 정보 수정
export const residentEdit = (data) => {
    return api.put(`/resident/mypage/edit`, data);
};

// 민감한 작업에 사용할 일회용 이미지 보안문자를 발급한다.
export const getResidentSecurityChallenge = () => {
    return api.get("/resident/security-challenge");
};

// 현재 비밀번호와 보안문자를 확인한 뒤 입주민 본인을 탈퇴 처리한다.
export const residentDelete = (data) => {
    return api.delete("/resident/mypage/delete", {
        data
    });
};

// 실제 탈퇴 전에 현재 비밀번호와 보안문자의 일치 여부만 확인한다.
export const verifyResidentWithdrawal = (data) => {
    return api.post("/resident/mypage/delete/verify", data);
};

// 현재 비밀번호와 보안문자를 확인한 뒤 비밀번호를 변경한다.
export const changeResidentPassword = (data) => {
    return api.put("/resident/mypage/password", data);
};

// 아이디 중복확인
export const idCheckMember = (loginId) => {
    return api.get("/signup/check-id",{
        params : {loginId}
    });
};

// 전출 처리되어 공개 회원가입이 가능한 세대 조회
export const getAvailableSignupUnits = () => {
    return api.get("/signup/available-units");
};

// 로그인 입주민 정보와 차량·최근 입출차 기록을 한 번에 조회한다.
export const getResidentDashboard = () => {
    return api.get("/resident/mypage/dashboard");
};
