import { defineStore } from "pinia";

import { ref } from "vue";
import { getNoteList, updateNoticeStatus } from "./noticeApi";
import { useJwtStore } from "../login/jwtStore";

export const useNoticeStore = defineStore("notice", () => {
    const jwtStore = useJwtStore();
    
    // 공지사항 목록
    const notices = ref([]);

    // 현재 조회중인 공지사항
    const notice = ref(null);

    // 목록 조회
    const loadNotices = async () => {
        const response = await getNoteList();

        notices.value = Array.isArray(response.data)? response.data : [];
    };

    // 상세 조회
    const loadNotice = async (noticeNo) => {
        if (notices.value.length === 0) {
            await loadNotices();
        }

        notice.value = notices.value.find((item) => {
            const itemNo = item.noticeNo ?? item.notice_no;

            return Number(itemNo) === Number(noticeNo);
        }) ?? null;
    };

    // 알림 상태 변경
    const changeNoticeStatus = async (noticeNo, alertStat) => {
        const handledByMemberName = alertStat === "Unresolved" ? null : jwtStore.userId;

        await updateNoticeStatus(noticeNo, alertStat, handledByMemberName);

        await loadNotices();
        await loadNotice(noticeNo);
    };

    return {
        notices,
        notice,

        loadNotices,
        loadNotice,
        changeNoticeStatus,
    };
});
