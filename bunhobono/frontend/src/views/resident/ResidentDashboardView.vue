<template>
    <section class="resident-board-page">
        <div v-if="loading" class="board-state">입주민 정보를 불러오는 중입니다.</div>
        <div v-else-if="errorMessage" class="board-state board-error">
            <p>{{ errorMessage }}</p>
            <button type="button" @click="loadDashboard">다시 불러오기</button>
        </div>

        <article v-else class="resident-board" :class="{ 'resident-carlog-page': mode === 'carlogs' }">
            <template v-if="mode === 'dashboard'">
            <header class="board-header">
                <div class="board-welcome">
                    <span class="profile-icon">●</span>
                    <div>
                        <h1>{{ dashboard.member.memName || "입주민" }}님 반갑습니다.</h1>
                    </div>
                </div>

            </header>

            <div class="board-info-grid">
                <section
                    class="member-summary-card"
                >
                    <header class="summary-card-header">
                        <h2>내 정보</h2>
                        <button type="button" @click.stop="goMypage">상세보기</button>
                    </header>
                    <dl class="member-summary-list">
                        <div><dt>이름</dt><dd>{{ dashboard.member.memName || "-" }}</dd></div>
                        <div><dt>동·호수</dt><dd>{{ residenceText }}</dd></div>
                        <div><dt>연락처</dt><dd>{{ dashboard.member.memPhone || "-" }}</dd></div>
                        <div><dt>상태</dt><dd><span class="member-status-badge">{{ dashboard.member.memStatus || "-" }}</span></dd></div>
                    </dl>
                </section>

                <section
                    class="vehicle-summary-card"
                >
                    <header class="summary-card-header">
                        <h2>차량현황</h2>
                        <button type="button" @click.stop="openVehicleManagement">차량관리</button>
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
                                        <small>차량번호</small>
                                        <strong>{{ vehicle.carNo || vehicle.vehicleCarNo || "차량번호 없음" }}</strong>
                                    </div>
                                    <div class="vehicle-info-section">
                                        <small>등록기간</small>
                                        <span>{{ approvalPeriodText(vehicle) }}</span>
                                    </div>
                                    <div class="vehicle-info-section">
                                        <small>승인여부</small>
                                        <span>{{ vehicle.vehicleStatusText || vehicle.vehicleStatus || "-" }}</span>
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
                                        <small>차량번호</small>
                                        <strong>{{ vehicle.carNo || vehicle.vehicleCarNo || "차량번호 없음" }}</strong>
                                    </div>
                                    <div class="vehicle-info-section">
                                        <small>등록기간</small>
                                        <span>{{ approvalPeriodText(vehicle) }}</span>
                                    </div>
                                    <div class="vehicle-info-section">
                                        <small>승인여부</small>
                                        <span>{{ vehicle.vehicleStatusText || vehicle.vehicleStatus || "-" }}</span>
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
                    class="recent-log-card recent-log-link"
                    role="button"
                    tabindex="0"
                    @click="openCarlogs"
                    @keyup.enter="openCarlogs"
                >
                    <h2>최근 입출차</h2>
                    <div class="recent-log-columns">
                        <div class="recent-log-item">
                            <span class="log-label log-in">↓ 입차</span>
                            <b>{{ latestInLog ? timeOnly(latestInLog.inTime) : "-" }}</b>
                            <span>{{ latestInLog?.carNo || "기록 없음" }}</span>
                            <small class="parking-movement-text">
                                {{ latestInLog ? `${latestInLog.parkingName || "주차장"} 입차` : "-" }}
                            </small>
                        </div>
                        <div class="recent-log-item">
                            <span class="log-label log-out">↑ 출차</span>
                            <b>{{ latestOutLog ? timeOnly(latestOutLog.outTime) : "-" }}</b>
                            <span>{{ latestOutLog?.carNo || "기록 없음" }}</span>
                            <small class="parking-movement-text">{{ latestOutLog ? `${latestOutLog.parkingName || "주차장"} 출차` : "-" }}</small>
                        </div>
                    </div>
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
                                <span>{{ parking.parkingName }}</span>
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
                <header class="resident-carlog-header detail-header">
                    <div>
                        <h2>입출차내역</h2>
                    </div>
                    <div class="detail-actions">
                        <button type="button" @click="openDashboard">홈으로 돌아가기</button>
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
    </section>
</template>

<script setup>
import { computed, onMounted, onUnmounted } from "vue";
import { useRoute, useRouter } from "vue-router";
import { storeToRefs } from "pinia";
import { useResidentDashboardStore } from "@/stores/residentDashboard";
import { usePagination } from "@/shared/pagination/usePagination";
import Pagination from "@/shared/pagination/Pagination.vue";

const router = useRouter();
const route = useRoute();
const dashboardStore = useResidentDashboardStore();
const mode = computed(() => route.name === "ResidentCarlogList" ? "carlogs" : "dashboard");

const { loading, errorMessage, dashboard, residenceText, normalVehicles, visitVehicles, parkingStatusList } = storeToRefs(dashboardStore);

const latestInLog = computed(() => dashboard.value.recentCarLogs.find((log) => log.inTime) || null);
const latestOutLog = computed(() => dashboard.value.recentCarLogs.find((log) => log.outTime) || null);
const residentCarLogs = computed(() => dashboard.value.recentCarLogs || []);
const {
    currentPage,
    totalPages,
    pageNumbers,
    paginatedItems: paginatedCarLogs,
    setPage,
} = usePagination(residentCarLogs, 10);

const timeOnly = (value) => value ? new Intl.DateTimeFormat("ko-KR", {
    hour: "2-digit", minute: "2-digit", hour12: false,
}).format(new Date(value)) : "-";

const dateTimeText = (value) => value ? new Intl.DateTimeFormat("ko-KR", {
    year: "2-digit", month: "2-digit", day: "2-digit",
    hour: "2-digit", minute: "2-digit", hour12: false,
}).format(new Date(value)) : "-";

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

const parkingColor = () => "#39e98a";
const parkedCarNumbers = (parking) => normalVehicles.value
    .filter((vehicle) => {
        return vehicle.parkingState === "PARKING"
            && String(vehicle.parkingName || "").trim() === String(parking.parkingName || "").trim();
    })
    .map((vehicle) => vehicle.carNo || vehicle.vehicleCarNo)
    .filter(Boolean);
const loadDashboard = () => dashboardStore.loadDashboard();
const openVehicleManagement = () => router.push("/resident/vehicles");
const openCarlogs = () => router.push("/resident/carlogs");
const openDashboard = () => router.push("/resident/dashboard");
const goMypage = () => router.push("/resident/mypage");
const goWelcome = () => router.push("/resident");
const refreshFromHeader = () => loadDashboard();

onMounted(() => {
    loadDashboard();
    window.addEventListener('resident-dashboard-refresh', refreshFromHeader);
});
onUnmounted(() => {
    window.removeEventListener('resident-dashboard-refresh', refreshFromHeader);
});
</script>

<style scoped>
:global(.content:has(.resident-board-page)) { padding: 0; }
.resident-board-page { min-height: calc(100vh - 70px); display: grid; place-items: start center; padding: 0; background: linear-gradient(180deg,#b9e5ff 0%,#d8f0ff 38%,#f0f9ff 72%,#ffffff 100%); }
.resident-board { width: min(1500px, 100%); padding: 18px 28px; border: 0; border-radius: 0; background: transparent; box-shadow: none; }
.resident-board.resident-carlog-page { align-self: start; width: min(1500px, 100%); padding: 24px; border: 0; border-radius: 0; background: transparent; box-shadow: none; }
.board-header { display: flex; align-items: center; justify-content: space-between; margin-bottom: 14px; }
.board-navigation-actions { display: flex; align-items: center; gap: 7px; margin-left: auto; margin-right: 10px; }
.board-navigation-actions button { padding: 9px 14px; border: 1px solid transparent; border-radius: 9px; color: #fff; font-size: 14px; font-weight: 700; cursor: pointer; transition: background-color .2s ease, box-shadow .2s ease, transform .2s ease; }
.board-navigation-actions .refresh-button { background: #35a554; }
.board-navigation-actions .refresh-button:hover { border-color: #fff !important; color: #fff !important; background: #35a554 !important; box-shadow: inset 0 0 0 1px #fff,0 0 0 2px rgba(53,165,84,.25); filter: none; opacity: 1; }
.board-navigation-actions .welcome-button { background: #2f7fdf; }
.board-navigation-actions .welcome-button:hover { border-color: #fff !important; color: #fff !important; background: #2f7fdf !important; box-shadow: inset 0 0 0 1px #fff,0 0 0 2px rgba(47,127,223,.25); filter: none; opacity: 1; }
.board-welcome { display: flex; align-items: center; gap: 11px; }
.profile-icon { display: grid; place-items: center; width: 42px; height: 42px; border-radius: 50%; color: #fff; background: #3d86e6; font-size: 0; }
.profile-icon::before { content: "●"; font-size: 22px; }
.board-welcome h1 { margin: 0; color: #203c58; font-size: 27px; }
.welcome-actions { display: flex; align-items: center; gap: 7px; margin-left: 10px; }
.welcome-actions button { padding: 8px 12px; border: 1px solid #c9dcef; border-radius: 9px; color: #315c86; background: #f5faff; font-size: 12px; font-weight: 700; cursor: pointer; }
.welcome-actions button:hover { border-color: #76a9dd; color: #1768bd; background: #eaf4ff; }
.board-date-time { display: flex; align-items: center; gap: 14px; padding: 9px 15px; border-radius: 13px; color: #38536d; background: #f4f8fc; font-size: 16px; font-weight: 700; }
.board-date-time i { width: 1px; height: 16px; background: #d7e1eb; }
.board-info-grid { display: grid; grid-template-columns: 32% 1fr; gap: 18px; }
.board-info-grid > * { min-width: 0; }
.member-summary-card,.vehicle-summary-card { box-sizing: border-box; min-height: 174px; padding: 13px 15px; border: 1px solid #dfe9f2; border-radius: 15px; background: #fff; }
.member-summary-card { display: flex; flex-direction: column; border-color: #d8e6f2; background: #fff; }
.summary-card-header { display: flex; align-items: center; justify-content: space-between; margin-bottom: 12px; }
.summary-card-header h2 { margin: 0; color: #263f59; font-size: 18px; }
.summary-card-header button { padding: 6px 10px; border: 1px solid #c9dcef; border-radius: 8px; color: #315c86; background: #f5faff; font-size: 11px; font-weight: 700; cursor: pointer; }
.summary-card-header button:hover { border-color: #76a9dd; color: #1768bd; background: #eaf4ff; }
.summary-card-header h2,.recent-log-card h2,.parking-card h2 { display: flex; align-items: center; gap: 7px; }
.summary-card-header h2::before,.recent-log-card h2::before,.parking-card h2::before { width: 4px; height: 15px; border-radius: 999px; background: #4b91d6; content: ""; }
.member-summary-list { display: grid; flex: 1; grid-template-columns: 1fr; overflow: hidden; margin: 0; border: 0; border-radius: 11px; background: rgba(255,255,255,.9); }
.member-summary-list div { display: grid; grid-template-columns: 72px minmax(0,1fr); align-items: center; column-gap: 12px; min-width: 0; padding: 6px 11px; }
.member-summary-list div + div { border-top: 0; }
.member-summary-list dt { color: #8495a5; font-size: 13px; font-weight: 600; }
.member-summary-list dd { overflow: hidden; margin: 0; color: #455d72; font-size: 15px; font-weight: 600; text-overflow: ellipsis; white-space: nowrap; }
.member-summary-list div:nth-child(1) dd { color: #287fd5; font-weight: 800; }
.member-summary-list div:nth-child(2) dd { color: #23a6d5; font-weight: 800; }
.member-summary-list div:nth-child(3) dd { color: #6478cf; font-weight: 700; }
.member-status-badge { display: inline-flex; align-items: center; min-height: 22px; padding: 2px 10px; border-radius: 999px; color: #287a4a; background: #e9f7ee; font-size: 13px; font-weight: 700; }
.vehicle-status-groups { overflow: hidden; border: 0; border-radius: 11px; }
.vehicle-status-group { display: grid; grid-template-columns: 110px 1fr; align-items: center; min-height: 52px; padding: 7px 11px; background: #f7faff; }
.vehicle-status-group + .vehicle-status-group { border-top: 0; }
.vehicle-status-group.visit-group { background: #f8fbf8; }
.vehicle-group-title { display: grid; gap: 2px; padding-right: 11px; }
.vehicle-group-title strong { color: #294761; font-size: 14px; }
.vehicle-group-title span { color: #1768bd; font-size: 12px; font-weight: 800; }
.visit-group .vehicle-group-title span { color: #26844a; }
.vehicle-slots { display: grid; grid-template-columns: 1fr; align-items: center; gap: 6px; min-width: 0; }
.vehicle-summary-row { display: grid; grid-template-columns: minmax(120px,.8fr) minmax(0,1.6fr) minmax(90px,.7fr); align-items: center; gap: 12px; min-width: 0; padding: 8px 10px; border: 0; border-radius: 9px; background: #fff; box-shadow: none; }
.vehicle-info-section { display: grid; gap: 2px; min-width: 0; color: #526b81; font-size: 11px; font-weight: 600; line-height: 1.3; word-break: keep-all; }
.vehicle-info-section + .vehicle-info-section { padding-left: 12px; border-left: 0; }
.vehicle-info-section small { color: #8a9aaa; font-size: 9px; font-weight: 600; }
.vehicle-info-section span { min-width: 0; }
.vehicle-number-section strong { overflow: hidden; color: #243f58; font-size: 16px; font-weight: 900; text-overflow: ellipsis; white-space: nowrap; }
.vehicle-status-group:not(.visit-group) .vehicle-number-section strong { color: #287fd5 !important; }
.vehicle-status-group.visit-group .vehicle-number-section strong { color: #2ca66a !important; }
.vehicle-status-group:not(.visit-group) .vehicle-info-section:last-child span { color: #287fd5; font-weight: 800; }
.vehicle-status-group.visit-group .vehicle-info-section:last-child span { color: #2ca66a; font-weight: 800; }
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
.recent-log-link { cursor: pointer; transition: border-color .2s ease, box-shadow .2s ease, transform .2s ease; }
.recent-log-link:hover { border-color: #8bb9e5; box-shadow: 0 7px 18px rgba(39, 91, 140, .12); transform: translateY(-1px); }
.recent-log-link:focus-visible { outline: 3px solid rgba(47, 127, 223, .28); outline-offset: 2px; }
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
.parking-zones { display: grid; grid-template-columns: repeat(4,1fr); gap: 8px; }
.parking-zone { position: relative; display: grid; justify-items: center; gap: 4px; padding: 8px 6px 7px; border: 0; border-radius: 0; background: transparent; box-shadow: none; }
.parking-zone::before { display: none; }
.zone-heading { display: flex; align-items: center; justify-content: center; width: 100%; }
.zone-heading > span { overflow: hidden; color: #202b36; font-size: 17px; font-weight: 900; text-overflow: ellipsis; white-space: nowrap; }
.zone-donut { --zone-color: #39e98a !important; display: grid; place-items: center; width: 86px; height: 86px; margin: 3px 0; border-radius: 50%; background: conic-gradient(var(--zone-color) 0 var(--usage-rate), #e7edf3 var(--usage-rate) 360deg); box-shadow: inset 0 0 0 1px rgba(37,61,85,.05); }
.zone-donut-inner { display: grid; place-items: center; align-content: center; width: 61px; height: 61px; border-radius: 50%; background: #fff; box-shadow: 0 2px 7px rgba(48,78,108,.12); }
.zone-donut-inner small { color: #73869a; font-size: 8px; line-height: 1.1; }
.zone-donut-inner b { color: #39e98a !important; font-size: 17px; line-height: 1.2; }
.zone-space-count { display: flex; align-items: baseline; gap: 2px; color: #6f8193; font-size: 9px; }
.zone-space-count strong { color: #39e98a !important; font-size: 15px; }
.my-parked-cars { display: grid; justify-items: center; gap: 2px; width: 100%; margin-top: 1px; }
.my-parked-cars b { display: flex; align-items: center; justify-content: center; gap: 4px; width: 100%; padding: 3px 4px; border-radius: 6px; color: #294761; background: #fff0f0; font-size: 9px; text-align: center; }
.my-parked-cars i { color: #e33232; font-size: 13px; font-style: normal; font-weight: 900; line-height: 1; }
.resident-carlog-section { min-height: 520px; }
.resident-carlog-table-wrap { overflow-x: auto; padding-bottom: 8px; }
.resident-carlog-table { min-width: 720px; }
.carlog-state { display: inline-flex; padding: 3px 9px; border-radius: 999px; color: #687b8d; background: #edf1f4; font-size: 11px; font-weight: 700; }
.carlog-state.parking { color: #287a4a; background: #e9f7ee; }
.resident-carlog-empty { padding: 45px 12px !important; color: #8799aa; text-align: center; }
.parking-empty,.board-state { color: #667d92; text-align: center; }.board-state { padding: 40px; border-radius: 18px; background: #fff; }.board-error { color: #b83e3e; }.board-state button { padding: 8px 14px; border: 1px solid #ccddeb; border-radius: 10px; background: #fff; cursor: pointer; }
@media (max-height:760px) and (min-width:901px){
    .resident-board-page{min-height:calc(100vh - 58px);padding:0}
    .resident-board{width:min(1500px,100%);padding:10px 18px;border-radius:0}
    .board-header{margin-bottom:8px}
    .profile-icon{width:34px;height:34px}.profile-icon::before{font-size:18px}
    .board-welcome h1{font-size:22px}
    .welcome-actions{gap:5px;margin-left:6px}.welcome-actions button{padding:6px 9px;font-size:10px}
    .board-date-time{padding:7px 12px;font-size:14px}
    .board-info-grid{gap:9px}.member-summary-card,.vehicle-summary-card{min-height:148px;padding:9px 11px}.summary-card-header{margin-bottom:7px}.member-summary-list{gap:0}.member-summary-list div{padding:5px 9px}.vehicle-status-group{min-height:43px;padding:5px 8px}
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
@media (max-width:900px){.board-info-grid,.board-bottom-grid{grid-template-columns:1fr}.parking-zones{min-height:120px}}
@media (max-width:600px){.resident-board-page{padding:6px}.resident-board{padding:14px}.resident-board.resident-carlog-page{padding:14px}.board-header{align-items:flex-start;flex-direction:column;gap:10px}.board-welcome{align-items:flex-start;flex-wrap:wrap}.welcome-actions{width:100%;margin-left:0}.board-date-time{align-self:stretch;justify-content:center}.board-info-grid,.board-bottom-grid{grid-template-columns:1fr}.member-summary-list{grid-template-columns:1fr}.vehicle-status-group{grid-template-columns:82px 1fr}.vehicle-summary-row{grid-template-columns:1fr;gap:5px}.vehicle-info-section+.vehicle-info-section{padding-top:5px;padding-left:0;border-top:0;border-left:0}.parking-zones{grid-template-columns:1fr 1fr;gap:14px}.parking-zone:nth-child(2){border-right:0}.resident-carlog-header{align-items:flex-start;flex-direction:column}.resident-carlog-header .detail-actions{width:100%}.resident-carlog-header button{width:100%}.resident-carlog-section{min-height:0}}
</style>
