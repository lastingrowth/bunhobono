<template>
    <h2>알림 상세</h2>

    <div>
        <button :disabled="!prevNotice" @click="moveNotice(prevNotice)">이전</button>
        <button :disabled="!nextNotice" @click="moveNotice(nextNotice)">다음</button>
        <button @click="router.push('/admin/notice')">목록</button>
        <button :disabled="!canResolveNotice" @click="resolveNotice">
            {{ saving ? "처리 중" : "처리 완료" }}
        </button>
    </div>

    <p v-if="loading">불러오는 중...</p>
    <p v-else-if="errorMessage">{{ errorMessage }}</p>

    <table v-else-if="store.notice" border="">
        <tbody>
            <tr>
                <th>알림 번호</th>
                <td>{{ formatValue(store.notice.noticeNo) }}</td>
            </tr>
            <tr>
                <th>등록 차량번호</th>
                <td>{{ formatValue(store.notice.registeredCarNo) }}</td>
            </tr>
            <tr>
                <th>촬영 차량번호</th>
                <td>{{ formatValue(store.notice.capturedCarNo) }}</td>
            </tr>
            <tr>
                <th>차량 구분</th>
                <td>{{ formatValue(formatCarKind(store.notice.carKind)) }}</td>
            </tr>
            <tr>
                <th>주차장</th>
                <td>{{ formatValue(store.notice.parkingName) }}</td>
            </tr>
            <tr>
                <th>입차 일시</th>
                <td>{{ formatValue(formatDate(store.notice.inTime)) }}</td>
            </tr>
            <tr>
                <th>감지 일시</th>
                <td>{{ formatValue(formatDate(store.notice.detectAt)) }}</td>
            </tr>
            <tr>
                <th>주차 일수</th>
                <td>{{ formatValue(store.notice.stayDays) }}</td>
            </tr>
            <tr>
                <th>처리 상태</th>
                <td>{{ statusOptions[store.notice.alertStat] ?? store.notice.alertStat }}</td>
            </tr>
            <tr>
                <th>처리 관리자</th>
                <td>{{ formatValue(store.notice.handledByMemberName) }}</td>
            </tr>
            <tr>
                <th>처리 일시</th>
                <td>{{ formatValue(formatDate(store.notice.handledAt)) }}</td>
            </tr>
        </tbody>
    </table>

    <p v-else>조회할 알림이 없습니다.</p>
</template>

<script setup>
import { computed, onMounted, ref, watch } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { getNoteList } from '@/features/notice/noticeApi';
import { useNoticeStore } from './noticeStore';

const route = useRoute();
const router = useRouter();
const store = useNoticeStore();

const loading = ref(false);
const saving = ref(false);
const errorMessage = ref("");

const statusOptions = {
    Unresolved: "미확인",
    Checked: "확인",
    Resolved: "처리 완료",
};

const noticeNo = computed(() => {
    return route.params.noticeNo;
});

const currentNoticeNo = computed(() => {
    return Number(noticeNo.value);
});

const currentIndex = computed(() => {
    return store.notices.findIndex((item) => {
        const itemNo = item.noticeNo ?? item.notice_no;

        return Number(itemNo) === currentNoticeNo.value;
    });
});

const prevNotice = computed(() => {
    if (currentIndex.value <= 0) {
        return null;
    }

    return store.notices[currentIndex.value - 1];
});

const nextNotice = computed(() => {
    if (currentIndex.value < 0 || currentIndex.value >= store.notices.length - 1) {
        return null;
    }

    return store.notices[currentIndex.value + 1];
});

const canResolveNotice = computed(() => {
    return Boolean(store.notice) && store.notice.alertStat !== "Resolved" && !saving.value;
});

const formatDate = (value) => {
    if (!value) {
        return "-";
    }

    const date = new Date(value);

    if (Number.isNaN(date.getTime())) {
        return value;
    }

    return date.toLocaleString("ko-KR");
};

const formatValue = (value) => {
    if (value === null || value === undefined || value === "") {
        return "-";
    }

    return value;
};

const formatCarKind = (value) => {
    if (value === "NORMAL") {
        return "입주민 차량";
    }

    if (value === "VISIT") {
        return "방문 차량";
    }

    if (value === "UNREGISTERED") {
        return "미등록 차량";
    }

    return value;
};

const loadNoticeList = async () => {
    const response = await getNoteList();

    store.notices = Array.isArray(response.data) ? response.data : [];
};

const loadDetail = async () => {
    loading.value = true;
    errorMessage.value = "";

    try {
        await store.loadNotice(noticeNo.value);

        if (!store.notice) {
            return;
        }

        if (store.notice.alertStat === "Unresolved") {
            try {
                await store.changeNoticeStatus(store.notice.noticeNo, "Checked");
            } catch (error) {
                console.error(error);
            }
        }
    } catch (error) {
        console.error(error);
        errorMessage.value = "알림 상세 정보를 불러오지 못했습니다.";
    } finally {
        loading.value = false;
    }
};

const moveNotice = (targetNotice) => {
    const targetNoticeNo = targetNotice?.noticeNo ?? targetNotice?.notice_no;

    if (!targetNoticeNo) {
        return;
    }

    router.push(`/admin/notice/${targetNoticeNo}`);
};

const resolveNotice = async () => {
    if (!canResolveNotice.value) {
        return;
    }

    saving.value = true;
    errorMessage.value = "";

    try {
        await store.changeNoticeStatus(store.notice.noticeNo, "Resolved");
    } catch (error) {
        console.error(error);
        errorMessage.value = "처리 완료 변경에 실패했습니다.";
    } finally {
        saving.value = false;
    }
};

onMounted(async () => {
    await loadNoticeList();
    await loadDetail();
});

watch(noticeNo, async () => {
    await loadDetail();
});
</script>
