import api from "@/shared/api/apiClient";

// 전체 로봇 조회
export const getRobotList = () => {
    return api.get("/robots");
};

// 로봇 상세 조회
export const getRobotDetail = (robotNo) => {
    return api.get(`/robots/${robotNo}`);
};

// 로봇별 원시 상태 로그 조회
export const getRobotLogs = (robotNo) => {
    return api.get("/robot-logs", {
        params: {
            robotNo
        }
    });
};

// 로봇 점검 완료
export const completeRobotMaintenance = (robotNo) => {
    return api.patch(`/robots/${robotNo}/maintenance`);
};