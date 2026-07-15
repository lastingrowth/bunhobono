import api from "@/shared/api/apiClient";

// 회원 전체 조회
export const getMemberList = () => {
    return api.get("/members");
};

// 선택한 탈퇴 회원을 승인 회원으로 복원한다.
export const restoreWithdrawnMembers = (memberNos) => {
    return api.put('/members/restore', memberNos);
};

// 탈퇴 처리된 선택 회원을 영구 삭제한다.
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

// 로그인 입주민 정보와 차량·최근 입출차 기록을 한 번에 조회한다.
export const getResidentDashboard = () => {
    return api.get("/resident/mypage/dashboard");
};

// 입주민 차량 등록 신청
export const createResVehicle = (data) => {
    return api.post("/vehicles/signUp", data);
};

// 입주민 차량 수정 신청
export const updateResVehicle = (vehicleCarNo, data) => {
    return api.put(`/vehicles/${vehicleCarNo}/edit`, data);
};

// 입주민 차량 삭제
export const deleteResVehicle = (vehicleCarNo) => {
    return api.delete(`/vehicles/${vehicleCarNo}/delete`);
};
