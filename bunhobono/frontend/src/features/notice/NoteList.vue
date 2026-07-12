<template>
    <main class="notice-page">
        <div class="notice-header">
            <h1>알림 리스트</h1>

            <div class="notice-actions">
                <div class="status-filters">
                    <label
                        v-for="option in statusOptions"
                        :key="option.value"
                        class="status-filter"
                    >
                        <input
                            v-model="selectedStatus"
                            type="radio"
                            name="noticeStatus"
                            :value="option.value"
                        >
                        <span>{{ option.label }}</span>
                    </label>
                </div>

                <select v-model="sortOrder">
                    <option value="desc">최신순</option>
                    <option value="asc">오래된순</option>
                </select>

                <button type="button" @click="handleLoadNotices">새로고침</button>
            </div>
        </div>

        <p v-if="loading">불러오는 중...</p>
        <p v-else-if="errorMessage">{{ errorMessage }}</p>
        <p v-else-if="sortedNotices.length === 0">선택한 처리상태의 알림이 없습니다.</p>

        <div v-else class="notice-table-wrap">
            <table class="notice-table" border="1">
                <thead>
                    <tr>
                        <th class="col-xs">번호</th>
                        <th class="col-sm">등록 차량번호</th>
                        <th class="col-sm">촬영 차량번호</th>
                        <th class="col-date">감지 일시</th>
                        <th class="col-xs">주차 일수</th>
                        <th class="col-status">처리 상태</th>
                    </tr>
                </thead>

                <tbody>
                    <tr
                        v-for="notice in sortedNotices"
                        :key="notice.noticeNo ?? notice.notice_no"
                        class="notice-row"
                        @click="goDetail(notice)"
                    >
                        <td class="col-xs">{{ getNoticeNo(notice) }}</td>
                        <td class="col-sm">{{ formatValue(notice.registeredCarNo ?? notice.registered_car_no) }}</td>
                        <td class="col-sm">{{ formatValue(notice.capturedCarNo ?? notice.captured_car_no) }}</td>
                        <td class="col-date">{{ formatValue(notice.detectAt ?? notice.detect_at, "date") }}</td>
                        <td class="col-xs">{{ formatValue(notice.stayDays ?? notice.stay_days) }}</td>
                        <td class="col-status">
                            <span>{{ getStatusLabel(getAlertStat(notice)) }}</span>
                        </td>
                    </tr>
                </tbody>
            </table>
        </div>
    </main>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue';
import { useRouter } from 'vue-router';
import { storeToRefs } from 'pinia';
import { useNoticeStore } from './noticeStore';

const router = useRouter();
const store = useNoticeStore();
const { notices } = storeToRefs(store);

const loading = ref(false);
const errorMessage = ref("");
const sortOrder = ref("desc");
const selectedStatus = ref("Unresolved");

const statusOptions = [
    { value: "Unresolved", label: "미확인" },
    { value: "Checked", label: "확인" },
    { value: "Resolved", label: "처리완료" },
];

const getAlertStat = (notice) => {
    return notice.alertStat ?? notice.alert_stat ?? "Unresolved";
};

const filteredNotices = computed(() => {
    return notices.value.filter((notice) => {
        return getAlertStat(notice) === selectedStatus.value;
    });
});

const sortedNotices = computed(() => {
    return [...filteredNotices.value].sort((a, b) => {
        const aNo = Number(a.noticeNo ?? a.notice_no ?? 0);
        const bNo = Number(b.noticeNo ?? b.notice_no ?? 0);

        return sortOrder.value === "desc" ? bNo - aNo : aNo - bNo;
    });
});

const formatDate = (value) => {
    const date = new Date(value);

    if (Number.isNaN(date.getTime())) {
        return value;
    }

    const month = String(date.getMonth() + 1).padStart(2, "0");
    const day = String(date.getDate()).padStart(2, "0");
    const hour = String(date.getHours()).padStart(2, "0");
    const minute = String(date.getMinutes()).padStart(2, "0");

    return `${month}-${day} ${hour}:${minute}`;
};

const formatValue = (value, type) => {
    if (value === null || value === undefined || value === "") {
        return "-";
    }

    if (type === "date") {
        return formatDate(value);
    }

    return value;
};

const getNoticeNo = (notice) => {
    return notice.noticeNo ?? notice.notice_no;
};

const getStatusLabel = (value) => {
    return statusOptions.find((option) => option.value === value)?.label ?? value ?? "-";
};

const handleLoadNotices = async () => {
    loading.value = true;
    errorMessage.value = "";

    try {
        await store.loadNotices();
    } catch (error) {
        console.error(error);
        errorMessage.value = "알림 목록을 불러오지 못했습니다.";
    } finally {
        loading.value = false;
    }
};

const goDetail = (notice) => {
    const noticeNo = getNoticeNo(notice);

    if (!noticeNo) {
        return;
    }

    router.push(`/admin/notice/${noticeNo}`);
};

onMounted(() => {
    handleLoadNotices();
});
</script>
