import api from "@/shared/api/apiClient";

// 회원 전체 조회
export const getMemberList = () => {
    return api.get("/members");
};

// 회원 검색
export const searchMember = (params) => {
    return api.get("/members/search", {params,});
} ;

// 회원 상세 조회
export const getMemberDetail = (memberNo) => {
    return api.get(`/member/${memberNo}`);
}

// 회원 수정
export const updateMember = (memberNo, data) => {
    return api.put(`/members/${memberNo}`, data);
};

// 회원 삭제
export const deleteMember = (memberNo) => {
    return api.delete(`/member/${memberNo}`);
};

// 회원 등록
export const signupMember = (member) => {
    return api.post("/members", member);
};