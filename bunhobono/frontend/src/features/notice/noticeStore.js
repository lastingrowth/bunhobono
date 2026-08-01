import { defineStore } from "pinia";
import { ref } from "vue";

import {
    deleteNotice,
    getNoteList,
    updateNoticeStatus
} from "./noticeApi";

export const useNoticeStore = defineStore("notice", () => {

    // 관리자 알림 목록
    const notices = ref([]);

    // 현재 조회 중인 관리자 알림
    const notice = ref(null);

    const feedbackMessage = ref("");
    const feedbackType = ref("success");

    let feedbackTimer;

    const showFeedback = (message, type = "success") => {
        feedbackMessage.value = message;
        feedbackType.value = type;

        window.clearTimeout(feedbackTimer);

        feedbackTimer = window.setTimeout(() => {
            feedbackMessage.value = "";
        }, 2500);
    };

    // 관리자 알림 목록 조회
    const loadNotices = async () => {
        const response = await getNoteList();

        notices.value = Array.isArray(response.data)
            ? response.data
            : [];
    };

    // 관리자 알림 상세 조회
    const loadNotice = async (noticeNo) => {
        if (notices.value.length === 0) {
            await loadNotices();
        }

        notice.value = notices.value.find((item) => {
            const itemNo = item.noticeNo ?? item.notice_no;

            return Number(itemNo) === Number(noticeNo);
        }) ?? null;
    };

    // 관리자 알림 처리상태 변경
    // 처리한 관리자는 백엔드에서 로그인 토큰으로 판단한다.
    const changeNoticeStatus = async (noticeNo, alertStat) => {
        await updateNoticeStatus(noticeNo, alertStat);

        await loadNotices();
        await loadNotice(noticeNo);
    };

    // 관리자 알림 삭제
    const remove = async (noticeNo) => {
        try {
            const response = await deleteNotice(noticeNo);

            if (response.data !== 1) {
                showFeedback(
                    "알림 기록 삭제에 실패했습니다.",
                    "error"
                );

                return false;
            }

            notices.value = notices.value.filter((item) => {
                const itemNo = item.noticeNo ?? item.notice_no;

                return Number(itemNo) !== Number(noticeNo);
            });

            if (notice.value) {
                const currentNo =
                    notice.value.noticeNo
                    ?? notice.value.notice_no;

                if (Number(currentNo) === Number(noticeNo)) {
                    notice.value = null;
                }
            }

            showFeedback("알림 기록을 삭제했습니다.");

            return true;
        } catch (error) {
            console.error("알림 삭제 실패", error);

            showFeedback(
                "알림 기록 삭제에 실패했습니다.",
                "error"
            );

            return false;
        }
    };

    return {
        notices,
        notice,
        feedbackMessage,
        feedbackType,

        loadNotices,
        loadNotice,
        changeNoticeStatus,
        remove
    };
});