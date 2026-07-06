import api  from "@/shared/api/apiClient";

// 알림 목록 조회
export const getNoteList = () =>{
    return api.get("/notice");
};

// 알림 상세 조회
export const getNoteDetail = (noticeNo) => {
    return api.get(`/notice/${noticeNo}`);
};

// 알림 확인 상태 변경
export const updateNoticeStatus = (noticeNo, alertStat) => {
    return api.put(`/notice/${noticeNo}/status`, { alertStat });
};
