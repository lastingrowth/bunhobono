import api  from "@/shared/api/apiClient";

// 알림 목록 조회
export const getNoteList = () =>{
    return api.get("/notice");
};

// 알림 확인 상태 변경
export const updateNoticeStatus = (noticeNo, alertStat, handledByMemberName) => {
    return api.put(`/notice/${noticeNo}/status`, { alertStat, handledByMemberName });
};

// 알림 삭제
export const deleteNotice = (noticeNo) => {
    return api.delete(`/notice/${noticeNo}/delete`);
};
