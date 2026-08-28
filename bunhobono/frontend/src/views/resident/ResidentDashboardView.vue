<template>
    <section class="resident-board-page">
        <div v-if="loading" class="board-state">입주민 정보를 불러오는 중입니다.</div>
        <div v-else-if="errorMessage" class="board-state board-error">
            <p>{{ errorMessage }}</p>
            <button type="button" @click="loadDashboard">다시 불러오기</button>
        </div>


        <article
          v-else
          class="resident-board"
          :class="{
            'resident-carlog-page': mode === 'carlogs',
            'resident-standard-page': mode === 'carlogs'
          }"
        >
            <template v-if="mode === 'dashboard'">
            <header class="board-header">
                <div class="board-welcome">
                    <span class="profile-icon">●</span>
                    <div class="welcome-title-row">
                      <h1>{{ dashboard.member.memName || "입주민" }}님 반갑습니다.</h1>

                      <button type="button" class="resident-notification-button" title="차량 알림" aria-label="차량 알림" @click="router.push({ path: '/resident/vehicles', query: { mode: 'notification' } })">
                        <img src="@/assets/images/mail.png" alt="" />
                        <span v-if="resVehicleStore.unreadNotificationCount > 0" class="notification-count">{{ resVehicleStore.unreadNotificationCount > 99 ? '99+' : resVehicleStore.unreadNotificationCount }}</span>
                      </button>
                    </div>
                </div>

            </header>

            <div class="board-info-grid">
                <section
                    class="member-summary-card"
                >
                    <header class="summary-card-header">
                        <h2>내 정보</h2>
                    </header>
                    <dl class="member-summary-list">
                        <div><dt>이름</dt><dd>{{ dashboard.member.memName || "-" }}</dd></div>
                        <div><dt>동·호수</dt><dd>{{ residenceText }}</dd></div>
                        <div><dt>연락처</dt><dd>{{ dashboard.member.memPhone || "-" }}</dd></div>
                        <div><dt>상태</dt><dd><span class="member-status-badge">{{ dashboard.member.memStatus === 'ACTIVE' ? '거주' : dashboard.member.memStatus || '-' }}</span></dd></div>
                    </dl>
                </section>

                <section class="board-summary-card">
                    <header class="summary-card-header">
                        <h2>공지사항</h2>
                        <button type="button" @click="openBoards">전체보기</button>
                    </header>

                    <ul v-if="dashboardBoards.length" class="dashboard-board-list">
                        <li v-for="board in dashboardBoards" :key="board.boardNo">
                            <button
                                type="button"
                                @click="openBoardDetail(board.boardNo)"
                            >
                                <span class="dashboard-board-copy">
                                    <strong>
                                        {{ board.title }}
                                        <!-- [new] 등록 후 3일 이내인 공지사항에 NEW를 표시 -->
                                        <span v-if="isNewBoard(board)" class="dashboard-board-new">NEW</span>
                                    </strong>
                                    <small>{{ boardPeriodText(board) }}</small>
                                </span>
                                <span class="dashboard-board-status">{{ board.periodStatus }}</span>
                            </button>
                        </li>
                    </ul>
                    <p v-else class="latest-board-empty">게시 중인 공지사항이 없습니다.</p>
                </section>

                <section
                    class="vehicle-summary-card"
                >
                    <header class="summary-card-header">
                        <h2>차량현황</h2>
                        <span class="visit-registration-remaining">
                            방문차량 {{ visitRegistrationRemainingText }} 남음
                        </span>
                    </header>
                    <div class="vehicle-status-groups">
                        <div class="vehicle-status-group">
                            <div class="vehicle-group-title">
                                <strong>내 차량</strong>
                                <span>{{ normalVehicles.length }}대</span>
                            </div>
                            <div class="vehicle-slots">
                                <div v-for="vehicle in normalVehicles.slice(0, 2)" :key="vehicle.vehicleNo || vehicle.carNo" class="vehicle-summary-row">
                                    <div class="vehicle-info-section vehicle-number-section">
                                        <strong>{{ vehicle.carNo || vehicle.vehicleCarNo || "차량번호 없음" }}</strong>
                                        <span
                                            class="vehicle-parking-state"
                                            :class="vehicleParkingStateClass(vehicle)"
                                        >{{ vehicleParkingStateText(vehicle) }}</span>
                                    </div>
                                    <div class="vehicle-info-section">
                                        <div class="vehicle-period-label-line">
                                            <small>등록기간</small>
                                            <span v-if="imminentExpiryText(vehicle)" class="vehicle-expiry-badge">
                                                {{ imminentExpiryText(vehicle) }}
                                            </span>
                                        </div>
                                        <span>{{ approvalPeriodText(vehicle) }}</span>
                                    </div>
                                </div>
                                <p v-if="normalVehicles.length === 0">등록된 내 차량이 없습니다.</p>
                            </div>
                        </div>
                        <div class="vehicle-status-group visit-group">
                            <div class="vehicle-group-title">
                                <strong>방문차량</strong>
                                <span>{{ visitVehicles.length }}대</span>
                            </div>
                            <div class="vehicle-slots">
                                <div v-for="vehicle in visitVehicles.slice(0, 1)" :key="vehicle.vehicleNo || vehicle.carNo" class="vehicle-summary-row">
                                    <div class="vehicle-info-section vehicle-number-section">
                                        <strong>{{ vehicle.carNo || vehicle.vehicleCarNo || "차량번호 없음" }}</strong>
                                        <span
                                            class="vehicle-parking-state"
                                            :class="vehicleParkingStateClass(vehicle)"
                                        >{{ vehicleParkingStateText(vehicle) }}</span>
                                    </div>
                                    <div class="vehicle-info-section">
                                        <small>등록기간</small>
                                        <span>{{ approvalPeriodText(vehicle) }}</span>
                                    </div>
                                </div>
                                <p v-if="visitVehicles.length === 0">등록된 방문차량이 없습니다.</p>
                            </div>
                        </div>
                    </div>
                </section>
            </div>

            <div class="board-bottom-grid">
                <section
                    class="recent-log-card"
                >
                    <h2>입출차 기록</h2>
                    <div v-if="residentCarLogs.length" class="recent-log-summary-list">
                        <div
                            v-for="log in residentCarLogs.slice(0, 5)"
                            :key="log.carLogNo || `${log.carNo}-${log.inTime}`"
                            class="recent-log-summary-item"
                        >
                            <div class="recent-log-car">
                                <strong>{{ log.carNo || "차량번호 없음" }}</strong>
                                <small>{{ log.parkingName || "주차장 정보 없음" }}</small>
                            </div>
                            <div class="recent-log-times">
                                <span><b class="log-direction in">입차</b>{{ dateTimeText(log.inTime) }}</span>
                                <span><b class="log-direction out">출차</b>{{ dateTimeText(log.outTime) }}</span>
                            </div>
                            <div class="recent-log-actions">
                                <span class="carlog-state" :class="{ parking: log.parkingState === 'PARKING' }">
                                    {{ log.parkingState === "PARKING" ? "주차 중" : "출차" }}
                                </span>
                                <button
                                    v-if="log.parkingState === 'PARKING'"
                                    type="button"
                                    class="exit-request-button"
                                    title="출차 신청 화면으로 이동"
                                    @click="openExitRequest(log)"
                                >
                                    출차 신청
                                </button>
                            </div>
                        </div>
                    </div>
                    <p v-else class="recent-log-empty">최근 입출차 기록이 없습니다.</p>
                </section>

                <section class="parking-card">
                    <h2>주차장 현황</h2>
                    <div v-if="parkingStatusList.length" class="parking-zones">
                        <div
                            v-for="parking in parkingStatusList.slice(0, 4)"
                            :key="parking.parkingNo"
                            class="parking-zone"
                            :style="{
                                '--zone-color': parkingColor(parking.usageRate),
                                '--usage-rate': `${parking.usageRate * 3.6}deg`,
                            }"
                            >
                            <div class="zone-heading">
                                <span>{{ parkingFloorName(parking.parkingName) }}</span>
                            </div>
                            <div
                                class="zone-donut"
                                role="progressbar"
                                :aria-valuenow="parking.usageRate"
                                aria-valuemin="0"
                                aria-valuemax="100">
                                <div class="zone-donut-inner">
                                    <small>현재 사용률</small>
                                    <b>{{ parking.usageRate }}%</b>
                                </div>
                            </div>
                            <div class="zone-space-count">
                                <strong>{{ parking.available }}</strong>
                                <span>/ {{ parking.total }}면 가능</span>
                            </div>
                            <div v-if="parkedCarNumbers(parking).length" class="my-parked-cars">
                                <b v-for="carNo in parkedCarNumbers(parking)" :key="carNo">
                                    <i aria-hidden="true">✓</i>
                                    {{ carNo }}
                                </b>
                            </div>
                        </div>
                    </div>
                    <p v-else class="parking-empty">등록된 주차장이 없습니다.</p>
                </section>
            </div>
            </template>

            <section v-else-if="mode === 'carlogs'" class="resident-carlog-section">
                <header class="resident-carlog-header detail-header resident-standard-header">
                    <div>
                        <h2>입출차내역</h2>
                        <p class="carlog-retention-guide">(입출차 기록 조회는 3개월 까지만 가능합니다.)</p>
                    </div>
                    <div class="detail-actions">
                        <button type="button" class="resident-home-button" title="홈으로 돌아가기" aria-label="홈으로 돌아가기" @click="router.push('/resident/dashboard')"><svg viewBox="0 0 24 24" aria-hidden="true"><path d="M3 11.2 12 4l9 7.2"/><path d="M5.5 10.2V20h13v-9.8"/><path d="M9.5 20v-6h5v6"/></svg></button>
                    </div>
                </header>

                <div class="resident-carlog-table-wrap">
                    <table class="resident-carlog-table">
                        <thead>
                            <tr>
                                <th>차량번호</th>
                                <th>주차장</th>
                                <th>입차시간</th>
                                <th>출차시간</th>
                                <th>상태</th>
                            </tr>
                        </thead>
                        <tbody>
                            <tr
                                v-for="log in paginatedCarLogs"
                                :key="log.carLogNo || `${log.carNo}-${log.inTime}`"
                            >
                                <td>{{ log.carNo || "-" }}</td>
                                <td>{{ log.parkingName || "-" }}</td>
                                <td>{{ dateTimeText(log.inTime) }}</td>
                                <td>{{ dateTimeText(log.outTime) }}</td>
                                <td>
                                    <span class="carlog-state" :class="{ parking: log.parkingState === 'PARKING' }">
                                        {{ log.parkingState === "PARKING" ? "주차 중" : "출차" }}
                                    </span>
                                </td>
                            </tr>
                            <tr v-if="residentCarLogs.length === 0">
                                <td colspan="5" class="resident-carlog-empty">입출차 기록이 없습니다.</td>
                            </tr>
                        </tbody>
                    </table>
                </div>
                <Pagination
                    v-if="residentCarLogs.length"
                    :current-page="currentPage"
                    :total-pages="totalPages"
                    :page-numbers="pageNumbers"
                    @change-page="setPage"
                />
            </section>
        </article>

        <!-- [공지 팝업] 로그인 후 최신 공지 포스터를 보여준다. -->
        <dialog ref="boardPopupDialog" class="board-popup-dialog">
            <article v-if="popupBoard" class="board-popup-card">
                <button
                    v-if="popupImageUrl"
                    type="button"
                    class="board-popup-poster-link"
                    :aria-label="`${popupBoard.title} 상세보기`"
                    @click="openPopupBoardDetail"
                >
                    <img
                        :src="popupImageUrl"
                        :alt="popupBoard.title"
                        class="board-popup-poster"
                    />
                </button>
                <div class="board-popup-content">
                    <div class="board-popup-actions">
                        <label class="board-popup-hide">
                            <input
                                v-model="hidePopupTodayChecked"
                                type="checkbox"
                                @change="changePopupTodayHidden"
                            />
                            <span>오늘 하루 보지 않기</span>
                        </label>
                        <button type="button" class="board-popup-detail" @click="closeBoardPopup">
                            닫기
                        </button>
                    </div>
                </div>
            </article>
        </dialog>

    </section>
</template>

<script setup>
import { computed, onMounted, onUnmounted, ref } from "vue";
import { useRoute, useRouter } from "vue-router";
import { storeToRefs } from "pinia";
import { useResidentDashboardStore } from "@/stores/residentDashboard";
import { usePagination } from "@/shared/pagination/usePagination";
import Pagination from "@/shared/pagination/Pagination.vue";
import { useBoardStore } from "@/features/board/boardStore";
import { vehicleExpiryText } from "@/features/vehicle/vehicleFormat";
import { useResVehicleStore } from "@/features/resVehicle/resVehicleStore";

const router = useRouter();

const openExitRequest = () => {
    router.push("/resident/exit-request");
};
const route = useRoute();
const dashboardStore = useResidentDashboardStore();
const boardStore = useBoardStore();
const resVehicleStore = useResVehicleStore();
const mode = computed(() => route.name === "ResidentCarlogList" ? "carlogs" : "dashboard");
let vehicleStatusTimer;
const boardPopupDialog = ref(null);
const popupBoard = ref(null);
const popupImageUrl = ref("");
const hidePopupTodayChecked = ref(false);
const vehicleStatusNow = ref(Date.now());

const {
    loading,
    errorMessage,
    dashboard,
    residenceText,
    normalVehicles,
    visitVehicles,
    visitRegistrationRemainingText,
    parkingStatusList
} = storeToRefs(dashboardStore);

// 대시보드에서는 게시 중인 공지사항을 최대 3건만 표시한다.
const dashboardBoards = computed(() => boardStore.list.slice(0, 3));

// [new] 등록 후 3일이 지나지 않은 공지사항인지 확인한다.
const isNewBoard = (board) => {
    if (!board.createdAt) return false;

    const createdAt = new Date(board.createdAt).getTime();
    if (Number.isNaN(createdAt)) return false;

    const elapsed = Date.now() - createdAt;
    return elapsed >= 0 && elapsed < 3 * 24 * 60 * 60 * 1000;
};

const threeMonthsAgo = () => {
    const cutoff = new Date();
    cutoff.setMonth(cutoff.getMonth() - 3);
    return cutoff;
};

const residentCarLogs = computed(() => {
    const cutoff = threeMonthsAgo();

    return (dashboard.value.recentCarLogs || []).filter((log) => {
        const referenceTime = log.outTime || log.inTime;
        if (!referenceTime) return false;

        const referenceDate = new Date(referenceTime);
        return !Number.isNaN(referenceDate.getTime()) && referenceDate >= cutoff;
    });
});
const {
    currentPage,
    totalPages,
    pageNumbers,
    paginatedItems: paginatedCarLogs,
    setPage,
} = usePagination(residentCarLogs, 10);

const dateTimeText = (value) => value ? new Intl.DateTimeFormat("ko-KR", {
    year: "2-digit", month: "2-digit", day: "2-digit",
    hour: "2-digit", minute: "2-digit", hour12: false,
}).format(new Date(value)) : "-";

const boardPeriodText = (board) => {
    const start = board.startAt ? approvalDateText(board.startAt) : "-";
    const end = board.endAt ? approvalDateText(board.endAt) : "계속 게시";
    return `${start} ~ ${end}`;
};

const approvalDateText = (value) => value ? new Intl.DateTimeFormat("ko-KR", {
    year: "2-digit", month: "2-digit", day: "2-digit",
}).format(new Date(value)) : "";

// 일반 차량은 승인일부터 제한 없음으로, 방문 차량은 시작일부터 종료일까지 표시한다.
const approvalPeriodText = (vehicle) => {
    const startDate = vehicle.startDate || vehicle.approvedAt;
    const endDate = vehicle.endDate;

    if (!startDate && !endDate) return "-";
    if (startDate && endDate) return `${approvalDateText(startDate)} ~ ${approvalDateText(endDate)}`;
    if (startDate) return `${approvalDateText(startDate)} ~ 제한 없음`;
    return `~ ${approvalDateText(endDate)}`;
};

const imminentExpiryText = (vehicle) => {
    const text = vehicleExpiryText(vehicle, vehicleStatusNow.value);
    return text.startsWith("만료 임박") ? text : "";
};

const vehicleParkingStateText = (vehicle) => {
    if (vehicle.parkingState === "PARKING") {
        const location = vehicle.parkingLocation;

        if (location?.spaceCode) {
            const parkingCode = location.parkingCode
                ? `${location.parkingCode} · `
                : "";
            return `주차중 · ${parkingCode}${location.spaceCode}`;
        }

        return "주차중 · 위치 배정 중";
    }

    if (vehicle.parkingState === "OUT") {
        return "주차완료";
    }

    return "미주차";
};

const vehicleParkingStateClass = (vehicle) => ({
    parking: vehicle.parkingState === "PARKING",
    completed: vehicle.parkingState === "OUT"
});

const parkingColor = () => "#39e98a";
const parkingFloorName = (value) => String(value || "-")
    .replace(/\bB1\b/gi, "지하 1층")
    .replace(/\bB2\b/gi, "지하 2층");
const parkedCarNumbers = (parking) => normalVehicles.value
    .filter((vehicle) => {
        return vehicle.parkingState === "PARKING"
            && String(vehicle.parkingName || "").trim() === String(parking.parkingName || "").trim();
    })
    .map((vehicle) => vehicle.carNo || vehicle.vehicleCarNo)
    .filter(Boolean);
const loadDashboard = () => dashboardStore.loadDashboard();
const openVehicleManagement = () => router.push("/resident/vehicles");
const openDashboard = () => router.push("/resident/dashboard");
const goMypage = () => router.push("/resident/mypage");
const goWelcome = () => router.push("/resident");
const openBoards = () => router.push("/resident/boards");
const openBoardDetail = (boardNo) => router.push(`/resident/boards/${boardNo}/detail`);

// [공지 팝업] 오늘 날짜를 로컬 시간 기준으로 만든다.
const todayKey = () => {
    const now = new Date();
    const pad = (value) => String(value).padStart(2, "0");
    return `${now.getFullYear()}-${pad(now.getMonth() + 1)}-${pad(now.getDate())}`;
};

const closeBoardPopup = () => boardPopupDialog.value?.close();

// [공지 팝업] 포스터를 누르면 해당 공지사항 상세 화면으로 이동한다.
const openPopupBoardDetail = () => {
    const boardNo = popupBoard.value?.boardNo;
    closeBoardPopup();
    if (boardNo) openBoardDetail(boardNo);
};

// [공지 팝업] 현재 공지를 오늘 하루 동안 다시 표시하지 않는다.
const changePopupTodayHidden = () => {
    if (popupBoard.value && hidePopupTodayChecked.value) {
        localStorage.setItem(
            "residentBoardPopupHidden",
            `${popupBoard.value.boardNo}|${todayKey()}`
        );
        closeBoardPopup();
        return;
    }

    localStorage.removeItem("residentBoardPopupHidden");
};

// [공지 팝업] 이미지가 있는 최신 공지사항을 로그인 후 한 번 표시한다.
const showLatestBoardPopup = async () => {
    if (mode.value !== "dashboard") return;

    const board = boardStore.list.find((item) => item.hasImage);
    if (!board) return;

    const hiddenValue = localStorage.getItem("residentBoardPopupHidden");
    if (hiddenValue === `${board.boardNo}|${todayKey()}`) return;

    const imageUrl = await boardStore.loadImage(board.boardNo);
    if (!imageUrl) return;

    popupBoard.value = board;
    popupImageUrl.value = imageUrl;
    hidePopupTodayChecked.value = false;
    boardPopupDialog.value?.showModal();
};

const loadBoards = async () => {
    await boardStore.loadList();
};

onMounted(async () => {
    resVehicleStore.loadNotifications().catch(() => {});
    vehicleStatusTimer = window.setInterval(() => {
        vehicleStatusNow.value = Date.now();
    }, 1000);

    await Promise.all([
        loadDashboard(),
        loadBoards().catch(() => [])
    ]);

    await showLatestBoardPopup();

});

onUnmounted(() => {
    window.clearInterval(vehicleStatusTimer);
});
</script>

<style scoped>
:global(.content:has(.resident-board-page)) { padding: 0; }
:global(.content:has(.resident-board-page) > .resident-board-page) { width: 100%; max-width: none; margin: 0; }
.resident-board-page { min-height: calc(100vh - var(--header-height)); display: grid; place-items: start center; padding: 0; background-image: linear-gradient(180deg,rgba(248,252,255,.44) 0%,rgba(250,253,255,.63) 45%,rgba(255,255,255,.81) 75%,rgba(255,255,255,.91) 100%),url("@/assets/images/back.jpg"); background-position: center; background-size: cover; background-repeat: no-repeat; background-attachment: fixed; }
.resident-board { width: min(1500px, 100%); padding: 18px 28px; border: 0; border-radius: 0; background: transparent; box-shadow: none; }
.resident-board.resident-carlog-page { align-self: start; width: min(760px, calc(100% - 200px)); margin: 30px auto; padding: 28px; border: 0; border-radius: 0; background: rgba(255,255,255,.94); box-shadow: 0 14px 38px rgba(39,79,113,.14); }
.board-popup-dialog { width: fit-content; max-width: calc(100vw - 32px); max-height: calc(100vh - 32px); padding: 0; overflow: hidden; border: 0; border-radius: 14px; background: #fff; box-shadow: 0 24px 70px rgba(15,35,52,.4); }
.board-popup-dialog::backdrop { background: rgba(13,25,36,.58); }
.board-popup-card { position: relative; display: flex; width: fit-content; max-width: 100%; flex-direction: column; margin: 0; overflow: hidden; background: #fff; }
.board-popup-poster-link { display: block; width: fit-content; max-width: 100%; margin: 0 !important; padding: 0 !important; overflow: hidden; border: 0 !important; border-radius: 0 !important; outline: 0; background: transparent !important; box-shadow: none !important; line-height: 0; cursor: pointer; }
.board-popup-poster-link:hover,
.board-popup-poster-link:focus,
.board-popup-poster-link:focus-visible { margin: 0 !important; padding: 0 !important; border: 0 !important; outline: 0 !important; background: transparent !important; box-shadow: none !important; }
.board-popup-poster { display: block; width: auto; max-width: calc(100vw - 32px); height: auto; max-height: calc(100vh - 105px); margin: 0; padding: 0; object-fit: contain; background: transparent; }
.board-popup-content { padding: 12px 14px 10px; }
.board-popup-actions { display: flex; align-items: center; justify-content: flex-end; gap: 12px; }
.board-popup-actions button { padding: 10px 14px; border-radius: 8px; font-weight: 800; cursor: pointer; }
.board-popup-hide { display: inline-flex; align-items: center; gap: 8px; color: #5e7182; font-size: 14px; cursor: pointer; }
.board-popup-hide input { width: 17px; height: 17px; margin: 0; accent-color: #2f83d5; cursor: pointer; }
.board-popup-detail { border: 1px solid #2f83d5; color: #fff; background: #2f83d5; }

/* 모바일에서는 공지 팝업이 화면 전체를 덮지 않도록 작게 표시한다. */
@media (any-pointer: coarse) and (max-width: 820px),
       (any-pointer: coarse) and (max-height: 820px) {
    .board-popup-dialog {
        width: min(420px, calc(100vw - 64px));
        max-width: calc(100vw - 64px);
        max-height: 70dvh;
        border-radius: 12px;
    }

    .board-popup-card,
    .board-popup-poster-link {
        width: 100%;
    }

    .board-popup-poster {
        width: 100%;
        max-width: 100%;
        max-height: calc(70dvh - 58px);
        object-fit: contain;
    }

    .board-popup-content {
        padding: 8px 10px;
    }

    .board-popup-actions {
        gap: 8px;
    }

    .board-popup-hide {
        gap: 5px;
        font-size: 12px;
    }

    .board-popup-hide input {
        width: 15px;
        height: 15px;
    }

    .board-popup-actions button {
        padding: 7px 10px;
        font-size: 12px;
    }
}
.board-header { display: flex; align-items: center; justify-content: space-between; margin-bottom: 14px; }
.board-navigation-actions { display: flex; align-items: center; gap: 7px; margin-left: auto; margin-right: 10px; }
.board-navigation-actions button { padding: 9px 14px; border: 1px solid transparent; border-radius: 9px; color: #fff; font-size: 14px; font-weight: 700; cursor: pointer; transition: background-color .2s ease, box-shadow .2s ease, transform .2s ease; }
.board-navigation-actions .refresh-button { background: #35a554; }
.board-navigation-actions .refresh-button:hover { border-color: #fff !important; color: #fff !important; background: #35a554 !important; box-shadow: inset 0 0 0 1px #fff,0 0 0 2px rgba(53,165,84,.25); filter: none; opacity: 1; }
.board-navigation-actions .welcome-button { background: #2f7fdf; }
.board-navigation-actions .welcome-button:hover { border-color: #fff !important; color: #fff !important; background: #2f7fdf !important; box-shadow: inset 0 0 0 1px #fff,0 0 0 2px rgba(47,127,223,.25); filter: none; opacity: 1; }
.board-welcome { display: flex; align-items: center; gap: 11px; }
.profile-icon { display: grid; place-items: center; width: 42px; height: 42px; border-radius: 50%; color: #fff; background: #3d86e6; font-size: 0; }
.profile-icon::before { content: "☺"; font-size: 25px; font-weight: 700; line-height: 1; }
.board-welcome h1 { margin: 0; color: #203c58; font-size: 27px; }
.welcome-actions { display: flex; align-items: center; gap: 7px; margin-left: 10px; }
.welcome-actions button { padding: 8px 12px; border: 1px solid #c9dcef; border-radius: 9px; color: #315c86; background: #f5faff; font-size: 12px; font-weight: 700; cursor: pointer; }
.welcome-actions button:hover { border-color: #76a9dd; color: var(--resident-accent); background: #eaf4ff; }
.board-date-time { display: flex; align-items: center; gap: 14px; padding: 9px 15px; border-radius: 13px; color: #38536d; background: #f4f8fc; font-size: 16px; font-weight: 700; }
.board-date-time i { width: 1px; height: 16px; background: #d7e1eb; }
.board-info-grid { display: grid; grid-template-columns: 32% 1fr; gap: 18px; }
.board-info-grid > * { min-width: 0; }
.member-summary-card,.board-summary-card,.vehicle-summary-card { box-sizing: border-box; min-height: 174px; padding: 13px 15px; border: 1px solid #dfe9f2; border-radius: 15px; background: #fff; }
.member-summary-card { display: flex; flex-direction: column; border-color: #d8e6f2; background: #fff; }
.summary-card-header { display: flex; align-items: center; justify-content: space-between; margin-bottom: 12px; }
.summary-card-header h2 { margin: 0; color: #263f59; font-size: 18px; }
.visit-registration-remaining { color: #66798c; font-size: 12px; font-weight: 500; white-space: nowrap; }
.summary-card-header button { padding: 6px 10px; border: 1px solid #c9dcef; border-radius: 8px; color: #315c86; background: #f5faff; font-size: 11px; font-weight: 700; cursor: pointer; }
.summary-card-header button:hover { border-color: #76a9dd; color: var(--resident-accent); background: #eaf4ff; }
.summary-card-header h2,.recent-log-card h2,.parking-card h2 { margin-left: 0; }
.dashboard-board-list { display: grid; overflow: hidden; margin: 0; padding: 0; border: 0; border-radius: 0; list-style: none; background: #fff; }
.dashboard-board-list li + li { border-top: 1px solid #e1eaf1; }
.dashboard-board-list button { display: flex; align-items: center; justify-content: space-between; gap: 12px; width: 100%; min-height: 48px; padding: 8px 4px; border: 0; color: inherit; background: transparent; text-align: left; cursor: pointer; }
.dashboard-board-list button:hover { background: #eef6fc; }
.dashboard-board-copy { display: grid; min-width: 0; gap: 3px; }
.dashboard-board-copy strong { overflow: hidden; color: #29465f; font-size: 14px; text-overflow: ellipsis; white-space: nowrap; }
/* [new] 공지사항 제목 옆 NEW 배지 모양 */
.dashboard-board-new { display: inline-block; margin-left: 5px; padding: 1px 5px; border-radius: 999px; color: #fff; background: #ef4444; font-size: 9px; font-weight: 900; vertical-align: 1px; }
.dashboard-board-copy small { color: #8294a4; font-size: 10px; }
.dashboard-board-status { flex-shrink: 0; padding: 4px 8px; border-radius: 999px; color: #14783d; background: #e4f7eb; font-size: 10px; font-weight: 800; }
.latest-board-empty { display: grid; place-items: center; min-height: 120px; margin: 0; border: 1px dashed #d5e1eb; border-radius: 11px; color: #8395a5; background: #f8fbfd; }
.member-summary-list { display: grid; flex: 1; grid-template-columns: 1fr; overflow: hidden; margin: 0; border: 0; border-radius: 11px; background: rgba(255,255,255,.9); }
.member-summary-list div { display: grid; grid-template-columns: 72px minmax(0,1fr); align-items: center; column-gap: 12px; min-width: 0; padding: 6px 11px; }
.member-summary-list div + div { border-top: 0; }
.member-summary-list dt { color: #8495a5; font-size: 13px; font-weight: 600; }
.member-summary-list dd { overflow: hidden; margin: 0; color: #455d72; font-size: 15px; font-weight: 600; text-overflow: ellipsis; white-space: nowrap; }
.member-summary-list div:nth-child(1) dd { color: var(--resident-accent); font-weight: 800; }
.member-summary-list div:nth-child(2) dd { color: var(--resident-accent); font-weight: 800; }
.member-summary-list div:nth-child(3) dd { color: var(--resident-accent); font-weight: 700; }
.member-status-badge { display: inline-flex; align-items: center; min-height: 22px; padding: 2px 10px; border-radius: 999px; color: #287a4a; background: #e9f7ee; font-size: 13px; font-weight: 700; }
.vehicle-status-groups { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); align-items: stretch; gap: 12px; overflow: visible; border: 0; border-radius: 11px; }
.vehicle-status-group { min-width: 0; padding: 10px; display: flex; flex-direction: column; gap: 8px; border: 1px solid #dce8f1; border-radius: 12px; background: #f7faff; }
.vehicle-status-group + .vehicle-status-group { border-top: 1px solid #dce8f1; }
.vehicle-status-group.visit-group { background: #f8fbf8; }
.vehicle-group-title { display: flex; align-items: center; justify-content: space-between; gap: 10px; padding: 0 2px 7px; border-bottom: 1px solid #dfe9f1; }
.vehicle-group-title strong { color: #294761; font-size: 14px; }
.vehicle-group-title span { color: var(--resident-accent); font-size: 12px; font-weight: 800; }
.visit-group .vehicle-group-title span { color: var(--resident-accent); }
.vehicle-slots { display: grid; grid-template-columns: 1fr; align-content: start; gap: 6px; min-width: 0; }
.vehicle-summary-row { display: grid; grid-template-columns: minmax(0, 1fr); align-items: start; gap: 6px 12px; min-width: 0; padding: 8px 10px; border: 1px solid #e3ecf3; border-radius: 9px; background: #fff; box-shadow: none; }
.vehicle-info-section { display: grid; gap: 4px; min-width: 0; color: #405a70; font-size: 13px; font-weight: 650; line-height: 1.4; word-break: keep-all; }
.vehicle-number-section { display: flex; align-items: center; justify-content: space-between; gap: 10px; }
.vehicle-info-section + .vehicle-info-section { padding-left: 0; border-left: 0; }
.vehicle-summary-row .vehicle-info-section:nth-child(1) { grid-column: 1; grid-row: 1; }
.vehicle-summary-row .vehicle-info-section:nth-child(2) { grid-column: 1 / -1; grid-row: 2; padding-top: 6px; border-top: 1px solid #e7eef4; }
.vehicle-info-section small { color: #71879a; font-size: 11px; font-weight: 700; }
.vehicle-info-section span { min-width: 0; white-space: normal; overflow-wrap: anywhere; }
.vehicle-number-section strong { color: #243f58; font-size: 16px; font-weight: 900; line-height: 1.3; white-space: normal; overflow-wrap: anywhere; }
.vehicle-parking-state { flex: 0 0 auto; width: fit-content; max-width: 45%; color: #8a99a7; font-size: 10px; font-weight: 750; line-height: 1.35; text-align: right; white-space: nowrap; }
.vehicle-parking-state.parking { color: #198754; }
.vehicle-parking-state.completed { color: #55758f; }
.vehicle-period-label-line { display: flex; align-items: center; gap: 6px; min-width: 0; }
.vehicle-expiry-badge { flex: 0 0 auto; padding: 3px 7px; border: 1px solid #f1a43c; border-radius: 999px; color: #c96d00; background: #fff4df; font-size: 10px; font-weight: 900; line-height: 1.2; white-space: nowrap; }
.vehicle-status-group:not(.visit-group) .vehicle-number-section strong { color: var(--resident-accent) !important; }
.vehicle-status-group.visit-group .vehicle-number-section strong { color: var(--resident-accent) !important; }
.vehicle-slots p { margin: 0; color: #8799aa; font-size: 12px; }
.board-top-grid { display: grid; grid-template-columns: 29% 34% 1fr; gap: 14px; height: 256px; }
.board-top-grid > * { min-width: 0; min-height: 0; }
.quick-menu { display: grid; gap: 12px; }
.quick-card { position: relative; display: flex; align-items: center; justify-content: center; min-width: 0; padding: 13px 42px; border: 0; border-radius: 15px; text-align: center; cursor: pointer; }
.quick-card.quick-visit {
    color: #fff;
    background-color: #2f7fdf !important;
    background-image: none !important;
}
.quick-card.quick-home {
    color: #fff;
    background-color: #61b889 !important;
    background-image: none !important;
}
.quick-card.quick-visit:hover { background-color: #246fc9 !important; }
.quick-card.quick-home:hover { background-color: #4da575 !important; }
.quick-icon { display: grid; place-items: center; flex: 0 0 54px; height: 54px; border-radius: 50%; background: rgba(255,255,255,.94); font-size: 27px; }
.quick-copy { display: block; }
.quick-copy strong { font-size: 24px; }
.quick-card b { position: absolute; right: 16px; font-size: 30px; font-weight: 300; }
.apartment-card { position: relative; overflow: hidden; height: 100%; min-height: 0; border-radius: 15px; background: #dceeff; }
.apartment-card > img { position: absolute; inset: 0; width: 100%; height: 100%; object-fit: cover; object-position: center 60%; }
.apartment-card::after { content: ""; position: absolute; inset: 55% 0 0; background: linear-gradient(transparent,rgba(18,45,70,.65)); }
.apartment-caption { position: absolute; z-index: 1; left: 14px; right: 14px; bottom: 12px; display: flex; align-items: center; gap: 9px; color: #fff; }
.apartment-caption > span:last-child { display: grid; gap: 2px; }
.apartment-caption small { font-size: 9px; opacity: .88; }
.shield { display: grid; place-items: center; width: 34px; height: 34px; border-radius: 50%; color: #fff; background: #4fac78; }
.alert-card { overflow: hidden; padding: 12px 14px; border: 1px solid #e1e9f1; border-radius: 15px; background: #fff; }
.alert-card-link { cursor: pointer; transition: border-color .2s ease, box-shadow .2s ease, transform .2s ease; }
.alert-card-link:hover { border-color: #8bb9e5; box-shadow: 0 7px 18px rgba(39, 91, 140, .12); transform: translateY(-1px); }
.alert-card-link:focus-visible { outline: 3px solid rgba(47, 127, 223, .28); outline-offset: 2px; }
.alert-card header { display: flex; align-items: center; justify-content: space-between; margin-bottom: 4px; }
.alert-card h2,.recent-log-card h2,.parking-card h2 { margin: 0; color: #263f59; font-size: 18px; }
.alert-card header button { border: 0; color: #73879b; background: none; font-size: 9px; cursor: pointer; }
.alert-card ul { margin: 0; padding: 0; list-style: none; }
.alert-card li { display: grid; grid-template-columns: 30px 1fr auto; align-items: center; gap: 8px; padding: 7px 0; border-bottom: 1px solid #edf1f5; }
.alert-card li:last-child { border-bottom: 0; }
.alert-icon { display: grid; place-items: center; width: 27px; height: 27px; border-radius: 50%; font-weight: 900; }
.alert-icon.red { color: #ef4b4b; background: #ffe7e7; }.alert-icon.orange { color: #ec9a1e; background: #fff1d6; }.alert-icon.blue { color: #347fdb; background: #e3efff; }
.alert-copy { display: grid; gap: 2px; min-width: 0; }
.alert-copy strong { color: #334b63; font-size: 13px; }.alert-copy small { overflow: hidden; color: #7d8d9d; font-size: 11px; text-overflow: ellipsis; white-space: nowrap; }
.alert-card time { color: #718398; font-size: 10px; }
.board-bottom-grid { display: grid; grid-template-columns: 32% 1fr; gap: 18px; margin-top: 18px; }
.recent-log-card,.parking-card { padding: 13px 15px; border: 1px solid #dfe9f2; border-radius: 15px; background: #fff; }
.recent-log-card h2 { margin-bottom: 22px; }
.parking-card h2 { margin-bottom: 10px; }
.recent-log-columns { display: grid; grid-template-columns: 1fr 1fr; }
.recent-log-item { display: grid; justify-items: center; gap: 3px; padding: 0 10px; border-right: 1px solid #e2e9f0; }
.recent-log-item:last-child { border-right: 0; }
.log-label { padding: 6px 14px; border-radius: 10px; font-size: 15px; font-weight: 800; }
.log-in { color: #176fd0; background: #e4f0ff; }
.log-out { color: #258b45; background: #e5f6e9; }
.recent-log-item b { color: #213d58; font-size: 24px; }.recent-log-item > span:not(.log-label) { color: #344f69; font-size: 14px; }.recent-log-item small { color: #6d91b8; font-size: 11px; }
.recent-log-item small.parking-movement-text { color: #df2f2f; font-weight: 800; }
.recent-log-summary-list { display: grid; gap: 7px; }
.recent-log-summary-item { display: grid; grid-template-columns: minmax(125px,.75fr) minmax(270px,1.8fr) auto; align-items: center; gap: 14px; min-width: 0; padding: 11px 13px; border-radius: 10px; background: #f7faff; }
.recent-log-actions { display: flex; align-items: center; justify-content: flex-end; gap: 8px; }
.exit-request-button { min-width: 70px; height: 28px; padding: 0 10px; border: 1px solid #b9d2e9; border-radius: 7px; color: #5f7f9d; background: #edf5fc; font-size: 10px; font-weight: 800; }
.exit-request-button:disabled { cursor: not-allowed; opacity: .72; }
.recent-log-car { display: grid; gap: 2px; min-width: 0; }
.recent-log-car strong { overflow: hidden; color: var(--resident-accent); font-size: 15px; text-overflow: ellipsis; white-space: nowrap; }
.recent-log-car small { overflow: hidden; color: #7b8fa1; font-size: 11px; text-overflow: ellipsis; white-space: nowrap; }
.recent-log-times { display: grid; gap: 5px; color: #536b80; font-size: 12px; }
.recent-log-times span { display: flex; align-items: center; gap: 8px; white-space: nowrap; }
.log-direction { display: inline-flex; justify-content: center; min-width: 34px; font-size: 10px; }
.log-direction.in { color: #176fd0; }.log-direction.out { color: #258b45; }
.recent-log-empty { margin: 30px 0; color: #8799aa; text-align: center; }
@media (any-pointer: coarse) and (max-width: 820px),
       (any-pointer: coarse) and (max-height: 820px) {
    .recent-log-summary-item { grid-template-columns: 1fr auto; gap: 8px; }
    .recent-log-times { grid-column: 1 / -1; }
    .recent-log-actions { grid-row: 1; grid-column: 2; flex-direction: column; align-items: flex-end; gap: 5px; }
}

@media (any-pointer: coarse) and (max-width: 820px),
       (any-pointer: coarse) and (max-height: 820px) {
    .resident-carlog-header .detail-actions { display:none!important; }
}
.parking-zones { display: grid; grid-template-columns: repeat(2,minmax(0,1fr)); gap: 8px; }
.parking-zone { position: relative; display: grid; justify-items: center; gap: 4px; padding: 8px 6px 7px; border: 0; border-radius: 0; background: transparent; box-shadow: none; }
.parking-zone::before { display: none; }
.zone-heading { display: flex; align-items: center; justify-content: center; width: 100%; }
.zone-heading > span { overflow: hidden; color: #7b8792; font-size: 17px; font-weight: 700; text-overflow: ellipsis; white-space: nowrap; }
.zone-donut { --zone-color: #39e98a !important; display: grid; place-items: center; width: 86px; height: 86px; margin: 3px 0; border-radius: 50%; background: conic-gradient(var(--zone-color) 0 var(--usage-rate), #e7edf3 var(--usage-rate) 360deg); box-shadow: inset 0 0 0 1px rgba(37,61,85,.05); }
.zone-donut-inner { display: grid; place-items: center; align-content: center; width: 61px; height: 61px; border-radius: 50%; background: #fff; box-shadow: 0 2px 7px rgba(48,78,108,.12); }
.zone-donut-inner small { color: #73869a; font-size: 8px; line-height: 1.1; }
.zone-donut-inner b { color: #39e98a !important; font-size: 17px; line-height: 1.2; }
.zone-space-count { display: flex; align-items: baseline; gap: 2px; color: #6f8193; font-size: 9px; }
.zone-space-count strong { color: #39e98a !important; font-size: 15px; }
.my-parked-cars { display: grid; justify-items: center; gap: 2px; width: 100%; margin-top: 1px; }
.my-parked-cars b { display: flex; align-items: center; justify-content: center; gap: 4px; width: 100%; padding: 3px 4px; border-radius: 6px; color: #294761; background: #fff0f0; font-size: 9px; text-align: center; }
.my-parked-cars i { color: #e33232; font-size: 13px; font-style: normal; font-weight: 900; line-height: 1; }
.resident-carlog-section { width: 100%; min-height: 0; height: auto; }
.carlog-retention-guide { margin: 7px 0 0; color: #708698; font-size: 12px; font-weight: 600; }
.resident-carlog-table-wrap { width: 100%; overflow-x: hidden; padding-bottom: 0; }
.resident-carlog-table { width: 100%; min-width: 0 !important; }
.resident-carlog-table th:nth-child(3),
.resident-carlog-table th:nth-child(4) { width: 165px; }
.resident-carlog-table th,
.resident-carlog-table td { box-sizing: border-box; padding-right: 10px; padding-left: 10px; }
.carlog-state { display: inline-flex; padding: 3px 9px; border-radius: 999px; color: #687b8d; background: #edf1f4; font-size: 11px; font-weight: 700; }
.carlog-state.parking {
    display: inline;
    padding: 0 !important;
    border: 0 !important;
    border-radius: 0 !important;
    color: #287a4a;
    background: none !important;
    box-shadow: none !important;
    outline: 0;
}
.resident-carlog-empty { padding: 45px 12px !important; color: #8799aa; text-align: center; }
.parking-empty,.board-state { color: #667d92; text-align: center; }.board-state { padding: 40px; border-radius: 18px; background: #fff; }.board-error { color: #b83e3e; }.board-state button { padding: 8px 14px; border: 1px solid #ccddeb; border-radius: 10px; background: #fff; cursor: pointer; }
@media (max-height:760px) and (min-width:901px){
    .resident-board-page{min-height:calc(100vh - var(--header-height));padding:0}
    .resident-board{width:min(1500px,100%);padding:10px 18px;border-radius:0}
    .board-header{margin-bottom:8px}
    .profile-icon{width:34px;height:34px}.profile-icon::before{font-size:18px}
    .board-welcome h1{font-size:22px}
    .welcome-actions{gap:5px;margin-left:6px}.welcome-actions button{padding:6px 9px;font-size:10px}
    .board-date-time{padding:7px 12px;font-size:14px}
    .board-info-grid{gap:9px}.member-summary-card,.board-summary-card,.vehicle-summary-card{min-height:148px;padding:9px 11px}.summary-card-header{margin-bottom:7px}.member-summary-list{gap:0}.member-summary-list div{padding:5px 9px}.vehicle-status-group{min-height:43px;padding:5px 8px}
    .board-top-grid{height:205px;gap:9px}
    .quick-menu{gap:8px}.quick-card{padding:8px 11px;border-radius:12px}
    .quick-icon{flex-basis:42px;height:42px;font-size:21px}
    .quick-copy strong{font-size:20px}.quick-card b{font-size:23px}
    .alert-card{padding:8px 10px}.alert-card li{padding:4px 0}.alert-icon{width:23px;height:23px}
    .board-bottom-grid{gap:9px;margin-top:9px}
    .recent-log-card,.parking-card{padding:9px 11px}
    .recent-log-card h2{margin-bottom:14px}.parking-card h2{margin-bottom:6px}
    .log-car{font-size:22px}.recent-log-item b{font-size:17px}
    .zone-donut{width:72px;height:72px}.zone-donut-inner{width:51px;height:51px}.zone-donut-inner b{font-size:15px}
    .parking-zone{gap:2px}
}
@media (max-width:900px){.resident-board.resident-carlog-page{width:calc(100% - 36px)}.board-info-grid,.board-bottom-grid{grid-template-columns:1fr}.parking-zones{min-height:120px}}
@media (any-pointer: coarse) and (max-width: 820px), (any-pointer: coarse) and (max-height: 820px){.resident-board-page{padding:6px}.resident-board{padding:14px}.resident-board.resident-carlog-page{width:calc(100% - 24px);margin:12px auto;padding:16px 12px}.board-header{align-items:flex-start;flex-direction:column;gap:10px}.board-welcome{align-items:flex-start;flex-wrap:wrap}.welcome-actions{width:100%;margin-left:0}.board-date-time{align-self:stretch;justify-content:center}.board-info-grid,.board-bottom-grid{grid-template-columns:1fr}.member-summary-list{grid-template-columns:1fr}.vehicle-status-group{grid-template-columns:82px 1fr}.vehicle-summary-row{grid-template-columns:1fr;gap:5px}.vehicle-info-section+.vehicle-info-section{padding-top:5px;padding-left:0;border-top:0;border-left:0}.parking-zones{grid-template-columns:1fr 1fr;gap:14px}.parking-zone:nth-child(2){border-right:0}.resident-carlog-header{align-items:flex-start;flex-direction:column;gap:14px}.resident-carlog-header .detail-actions{width:100%}.resident-carlog-header button{width:100%;min-height:44px}.resident-carlog-section{min-height:0}.resident-carlog-table-wrap{overflow:visible;padding:0}.resident-carlog-table,.resident-carlog-table tbody,.resident-carlog-table tr,.resident-carlog-table td{display:block;width:100%}.resident-carlog-table{min-width:0;border:0;box-shadow:none;background:transparent}.resident-carlog-table thead{display:none}.resident-carlog-table tbody{display:grid;gap:12px}.resident-carlog-table tbody tr{padding:14px;border:1px solid #d9e5ee;border-radius:12px;background:#fff}.resident-carlog-table tbody td{padding:7px 0;border:0;text-align:left;white-space:normal;overflow-wrap:anywhere}.resident-carlog-table tbody td::before{display:inline-block;min-width:70px;margin-right:8px;color:#71879a;font-size:12px;font-weight:800}.resident-carlog-table tbody td:nth-child(1)::before{content:"차량번호"}.resident-carlog-table tbody td:nth-child(2)::before{content:"주차장"}.resident-carlog-table tbody td:nth-child(3)::before{content:"입차시간"}.resident-carlog-table tbody td:nth-child(4)::before{content:"출차시간"}.resident-carlog-table tbody td:nth-child(5)::before{content:"상태"}.resident-carlog-table .resident-carlog-empty{padding:36px 10px!important;text-align:center}.resident-carlog-table .resident-carlog-empty::before{display:none}}
@media (any-pointer: coarse) and (max-width: 820px), (any-pointer: coarse) and (max-height: 820px) {
    .resident-carlog-header .resident-home-button { width: 42px; min-height: 42px; }
}
.welcome-title-row { display: flex; align-items: center; gap: 10px; }

@media (max-width: 900px) {
    .board-header { align-items: stretch; flex-direction: column; gap: 12px; }
}

@media (max-width: 480px) {
}
.notification-button {
    position: relative;
    display: flex;
    align-items: center;
    justify-content: center;

    width: 38px;
    height: 38px;

    padding: 0;
    border: 1px solid #c9dcef;
    border-radius: 9px;
    background: #f5faff;
    cursor: pointer;
}.notification-button:hover { border-color: #76a9dd; color: var(--resident-accent); background: #eaf4ff; }
.notification-envelope {
    width: 22px;
    height: 22px;
    object-fit: contain;

    display: block;
    margin: auto;
}
.notification-badge { position: absolute; top: -7px; right: -7px; display: grid; min-width: 20px; height: 20px; place-items: center; padding: 0 5px; border: 2px solid #fff; border-radius: 10px; color: #fff; background: #e5484d; font-size: 11px; font-weight: 800; }

/* 대시보드의 각 영역을 하나의 큰 카드 안에서 구분한다. */
.resident-board:not(.resident-carlog-page) {
    position: relative;
    width: min(760px, calc(100% - 200px));
    margin: 30px auto;
    padding: 30px 48px 38px;
    border: 1px solid rgba(202, 220, 235, .9);
    border-radius: 0;
    background: rgba(255, 255, 255, .94);
    box-shadow: 0 14px 38px rgba(39, 79, 113, .14);
    backdrop-filter: blur(5px);
}

.resident-board:not(.resident-carlog-page) .board-info-grid,
.resident-board:not(.resident-carlog-page) .board-bottom-grid { gap: 0; }
.resident-board:not(.resident-carlog-page) .board-info-grid {
    padding-bottom: 18px;
    border-bottom: 1px solid #dfe9f2;
}
.resident-board:not(.resident-carlog-page) .board-bottom-grid { margin-top: 0; }
.resident-board:not(.resident-carlog-page) .member-summary-card,
.resident-board:not(.resident-carlog-page) .board-summary-card,
.resident-board:not(.resident-carlog-page) .vehicle-summary-card,
.resident-board:not(.resident-carlog-page) .recent-log-card,
.resident-board:not(.resident-carlog-page) .parking-card {
    border: 0;
    border-radius: 0;
    background: transparent;
}
.resident-board:not(.resident-carlog-page) .member-summary-card,
.resident-board:not(.resident-carlog-page) .recent-log-card { padding-left: 0; padding-right: 22px; }
.resident-board:not(.resident-carlog-page) .board-summary-card {
    padding-right: 22px;
    padding-left: 22px;
    border-left: 1px solid #dfe9f2;
}
.resident-board:not(.resident-carlog-page) .vehicle-summary-card,
.resident-board:not(.resident-carlog-page) .parking-card {
    padding-right: 0;
    padding-left: 22px;
    border-left: 1px solid #dfe9f2;
}
.resident-board:not(.resident-carlog-page) .recent-log-card,
.resident-board:not(.resident-carlog-page) .parking-card { padding-top: 18px; }
@media (max-width:900px) {
    .resident-board:not(.resident-carlog-page) { width: calc(100% - 36px); padding-right: 22px; padding-left: 22px; }
    .resident-board:not(.resident-carlog-page) .board-info-grid { padding-bottom: 0; }
    .resident-board:not(.resident-carlog-page) .member-summary-card,
    .resident-board:not(.resident-carlog-page) .board-summary-card,
    .resident-board:not(.resident-carlog-page) .vehicle-summary-card,
    .resident-board:not(.resident-carlog-page) .recent-log-card,
    .resident-board:not(.resident-carlog-page) .parking-card { padding: 16px 0; border-left: 0; }
    .resident-board:not(.resident-carlog-page) .vehicle-summary-card,
    .resident-board:not(.resident-carlog-page) .board-summary-card,
    .resident-board:not(.resident-carlog-page) .parking-card { border-top: 1px solid #dfe9f2; }
}
@media (any-pointer: coarse) and (max-width: 820px),
       (any-pointer: coarse) and (max-height: 820px) {
    .resident-board:not(.resident-carlog-page) { width: calc(100% - 24px); margin: 12px 0; padding: 22px; border-radius: 0; }
}

/* 홈페이지처럼 주요 영역을 위에서 아래로 이어서 보여준다. */
.resident-board:not(.resident-carlog-page) .board-info-grid,
.resident-board:not(.resident-carlog-page) .board-bottom-grid {
    display: grid;
}
.resident-board:not(.resident-carlog-page) .board-info-grid {
    grid-template-columns: minmax(0, 1fr) minmax(0, 1fr);
    padding-bottom: 0;
}
.resident-board:not(.resident-carlog-page) .board-bottom-grid { grid-template-columns: 1fr; }
.resident-board:not(.resident-carlog-page) .member-summary-card,
.resident-board:not(.resident-carlog-page) .board-summary-card,
.resident-board:not(.resident-carlog-page) .vehicle-summary-card,
.resident-board:not(.resident-carlog-page) .recent-log-card,
.resident-board:not(.resident-carlog-page) .parking-card {
    min-height: 0;
    padding: 24px 26px;
    border: 0;
    background: transparent;
}
.resident-board:not(.resident-carlog-page) .member-summary-card {
    padding-top: 12px;
    border-top: 0;
}
.resident-board:not(.resident-carlog-page) .board-summary-card {
    padding-top: 12px;
    border-top: 0;
    background: transparent;
}
.resident-board:not(.resident-carlog-page) .vehicle-summary-card {
    grid-column: 1 / -1;
}
.resident-board:not(.resident-carlog-page) .parking-card { padding-bottom: 8px; }

/* 대시보드 내부를 카드로 나누지 않고 하나의 바탕으로 표시한다. */
.resident-board:not(.resident-carlog-page) .board-info-grid,
.resident-board:not(.resident-carlog-page) .board-bottom-grid {
    gap: 0;
}

.resident-board:not(.resident-carlog-page) :is(
    .member-summary-list,
    .dashboard-board-list,
    .vehicle-status-groups,
    .vehicle-status-group,
    .vehicle-summary-row,
    .alert-card,
    .recent-log-summary-item,
    .latest-board-empty
) {
    border: 0;
    border-radius: 0;
    background: transparent;
    box-shadow: none;
}

.resident-board:not(.resident-carlog-page) :is(
    .dashboard-board-list li + li,
    .alert-card li,
    .recent-log-item
) {
    border-color: transparent;
}

.resident-board:not(.resident-carlog-page) .recent-log-summary-list {
    gap: 0;
}

.resident-board:not(.resident-carlog-page) .recent-log-summary-item + .recent-log-summary-item {
    border-top: 1px solid #dfe7ee;
}

/* 홈 차량현황은 내 차량과 방문차량을 좌우의 독립 영역으로 구분한다. */
.resident-board:not(.resident-carlog-page) .vehicle-status-groups {
    background: transparent;
}

.resident-board:not(.resident-carlog-page) .vehicle-status-group {
    border: 1px solid #bcdcf3;
    border-top: 3px solid #42a5e8;
    border-radius: 12px;
    background: #eef8ff;
    box-shadow: 0 5px 14px rgba(53, 132, 190, .08);
}

.resident-board:not(.resident-carlog-page) .vehicle-status-group.visit-group {
    border-color: #bfe5cd;
    border-top-color: #52b97b;
    background: #effaf3;
    box-shadow: 0 5px 14px rgba(58, 150, 94, .08);
}

.resident-board:not(.resident-carlog-page) .vehicle-summary-row {
    border: 1px solid #cfe4f4;
    border-radius: 9px;
    background: #fbfdff;
}

.resident-board:not(.resident-carlog-page) .visit-group .vehicle-summary-row {
    border-color: #d1e9d9;
    background: #fcfffd;
}

.resident-board:not(.resident-carlog-page) .vehicle-status-group:not(.visit-group) .vehicle-group-title {
    border-bottom-color: #c8e1f4;
}

.resident-board:not(.resident-carlog-page) .vehicle-status-group.visit-group .vehicle-group-title {
    border-bottom-color: #cce6d5;
}

@media (max-width: 900px) {
    .vehicle-status-groups {
        grid-template-columns: 1fr;
    }

    .vehicle-status-group {
        display: flex;
    }

    .vehicle-summary-row {
        grid-template-columns: 1fr;
    }

    .vehicle-summary-row .vehicle-info-section:nth-child(1),
    .vehicle-summary-row .vehicle-info-section:nth-child(2) {
        grid-column: 1;
        grid-row: auto;
        min-width: 0;
        padding-right: 0;
        padding-left: 0;
        border-left: 0;
    }

    .vehicle-summary-row .vehicle-info-section:nth-child(2) {
        padding-top: 7px;
        border-top: 1px solid #e7eef4;
    }
}
@media (any-pointer: coarse) and (max-width: 820px),
       (any-pointer: coarse) and (max-height: 820px) {
    .resident-board:not(.resident-carlog-page) .member-summary-card,
    .resident-board:not(.resident-carlog-page) .board-summary-card,
    .resident-board:not(.resident-carlog-page) .vehicle-summary-card,
    .resident-board:not(.resident-carlog-page) .recent-log-card,
    .resident-board:not(.resident-carlog-page) .parking-card {
        padding-right: 20px;
        padding-left: 20px;
    }
    .resident-board:not(.resident-carlog-page) .board-info-grid {
        grid-template-columns: 1fr;
    }
    .resident-board:not(.resident-carlog-page) .board-summary-card {
        border-left: 0;
    }
}

/* 웹에서는 공지사항도 내 정보·차량현황처럼 사각형 영역 안에 표시한다. */
@media (min-width: 901px) {
    .resident-board:not(.resident-carlog-page) .board-info-grid {
        border-bottom: 0;
    }

    .resident-board:not(.resident-carlog-page) .board-summary-card {
        border: 0;
        border-radius: 0;
        background: #ffffff;
        box-shadow: 0 8px 22px rgba(39, 79, 113, .1);
    }
}

.unread-notification-dialog { width: min(430px, calc(100vw - 32px)); padding: 0; border: 1px solid #cbd8e5; border-radius: 8px; background: #fff; box-shadow: 0 20px 55px rgba(20, 48, 74, .26); }
.unread-notification-dialog::backdrop { background: rgba(19, 35, 51, .48); }
.unread-dialog-body { padding: 26px; }
.unread-dialog-heading { display: flex; align-items: center; gap: 10px; }
.unread-dialog-heading h2 { margin: 0; color: #18344e; font-size: 21px; }
.unread-dialog-indicator { width: 5px; height: 24px; border-radius: 2px; background: #2387d9; }
.unread-dialog-body p { margin: 22px 0 26px; color: #526b80; line-height: 1.7; }
.unread-dialog-body strong { color: #1876c5; }
.unread-dialog-hide-today { width: fit-content; margin: -8px 0 22px; display: flex; align-items: center; gap: 8px; color: #60778a; font-size: 13px; font-weight: 700; cursor: pointer; }
.unread-dialog-hide-today input { width: 16px; height: 16px; margin: 0; accent-color: #2387d9; cursor: pointer; }
.unread-dialog-actions { display: flex; justify-content: flex-end; gap: 8px; }
.unread-dialog-actions button { min-width: 86px; height: 38px; border: 1px solid #cad7e3; border-radius: 6px; background: #fff; cursor: pointer; }
.unread-dialog-actions .unread-dialog-primary { border-color: #2387d9; color: #fff; background: #2387d9; }

/* 입주민 대시보드의 일반 문자는 검정으로 통일한다. */
.resident-board :is(h1, h2, h3, h4, p, dt, dd, small, th, td) {
    color: #111 !important;
}

.dashboard-board-copy strong,
.vehicle-group-title strong,
.vehicle-info-section,
.vehicle-info-section small,
.alert-copy strong,
.alert-copy small,
.recent-log-item b,
.recent-log-item > span:not(.log-label),
.recent-log-car small,
.recent-log-times,
.zone-space-count,
.visit-registration-remaining {
    color: #111 !important;
}

/* 하늘색 포인트 문자는 유지한다. */
.member-summary-list div:nth-child(1) dd,
.member-summary-list div:nth-child(2) dd,
.member-summary-list div:nth-child(3) dd,
.vehicle-group-title > span,
.vehicle-number-section strong,
.recent-log-car strong {
    color: var(--resident-accent) !important;
}

/* 상태와 경고 색은 의미를 유지한다. */
.member-status-badge,
.dashboard-board-status,
.vehicle-parking-state.parking,
.log-out,
.log-direction.out,
.carlog-state.parking {
    color: #198754 !important;
}

.vehicle-expiry-badge {
    color: #c96d00 !important;
}

.recent-log-item small.parking-movement-text,
.board-error {
    color: #b83e3e !important;
}
</style>
