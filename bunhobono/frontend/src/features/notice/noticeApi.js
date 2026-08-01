import api from "@/shared/api/apiClient";

// 관리자 알림 목록 조회
export const getNoteList = () => {
    return api.get("/notice");
};

// 차량번호로 관리자 알림 검색
export const searchNoticesByCarNo = (carNo) => {
    return api.get("/notice/search", {
        params: { carNo }
    });
};

// 관리자 알림 처리상태 변경
// 처리한 관리자는 백엔드에서 로그인 토큰으로 판단한다.
export const updateNoticeStatus = (noticeNo, alertStat) => {
    return api.put(`/notice/${noticeNo}/status`, {
        alertStat
    });
};

// 관리자 알림 삭제
export const deleteNotice = (noticeNo) => {
    return api.delete(`/notice/${noticeNo}/delete`);
};