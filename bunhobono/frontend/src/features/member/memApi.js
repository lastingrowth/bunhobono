import api from "@/shared/api/apiClient";

// 회원 전체 조회
export const getMemberList = () => {
    return api.get("/members");
};

// 탈퇴 후 3일이 지나 보관 삭제 확인이 필요한 회원 알림을 조회한다.
export const getMemberArchiveAlerts = () => {
    return api.get('/members/archive-alerts');
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

// 회원 목록에서 선택한 여러 회원의 승인 상태를 한 번에 변경한다.
export const updateMemberApprovalStatus = (memberNos, approvalStatus) => {
    return api.put('/members/approval-status', { memberNos, approvalStatus });
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

// 입주민 로그인 시, 직접 회원 정보 삭제
export const residentDelete = (loginId) => {
    return api.delete("/resident/mypage/delete", {
        data: {
            loginId: loginId
        }
    });
};

// 아이디 중복확인
export const idCheckMember = (loginId) => {
    return api.get("/signup/check-id",{
        params : {loginId}
    });
};
