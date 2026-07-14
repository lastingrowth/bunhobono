import { defineStore } from "pinia";

import { ref } from "vue";
import { deleteNotice, getNoteList, updateNoticeStatus } from "./noticeApi";
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

    // 알림 삭제
    const remove = async (noticeNo, router) => {
        const result = confirm("알림을 삭제하시겠습니까?");

        if (!result) {
            return;
        }

        const response = await deleteNotice(noticeNo);

        if (response.data === 1) {
            notices.value = notices.value.filter((item) => {
                const itemNo = item.noticeNo ?? item.notice_no;

                return Number(itemNo) !== Number(noticeNo);
            });

            if (notice.value) {
                const currentNo = notice.value.noticeNo ?? notice.value.notice_no;

                if (Number(currentNo) === Number(noticeNo)) {
                    notice.value = null;
                }
            }

            alert("알림 삭제 완료");

            if (router) {
                router.push("/admin/notice");
            }
        } else {
            alert("알림 삭제 실패");
        }
    };

    return {
        notices,
        notice,

        loadNotices,
        loadNotice,
        changeNoticeStatus,
        remove,
    };
});
