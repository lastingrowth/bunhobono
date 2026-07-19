<template>
    <section class="resident-board-page">
        <div v-if="loading" class="board-state">입주민 정보를 불러오는 중입니다.</div>
        <div v-else-if="errorMessage" class="board-state board-error">
            <p>{{ errorMessage }}</p>
            <button type="button" @click="loadDashboard">다시 불러오기</button>
        </div>

        <article v-else class="resident-board">
            <header class="board-header">
                <div class="board-welcome">
                    <span class="profile-icon">●</span>
                    <div>
                        <h1>{{ dashboard.member.memName || "입주민" }}님 반갑습니다.</h1>
                    </div>
                    <div class="welcome-actions">
                        <button type="button" @click="openVehicleManagement">우리호실 차량관리</button>
                        <button type="button" @click="goMypage">내 정보 수정</button>
                    </div>
                </div>

                <div class="board-date-time">
                    <span>▣&nbsp; {{ formattedDate }}</span>
                    <i></i>
                    <span>◷&nbsp; {{ formattedTime }}</span>
                </div>
            </header>

            <div class="board-top-grid">
                <nav class="quick-menu" aria-label="대시보드 바로가기">
                    <button type="button" class="quick-card quick-visit" @click="openVehicleManagement">
                        <span class="quick-copy">
                            <strong>방문차량 등록</strong>
                        </span>
                        <b>›</b>
                    </button>

                    <button type="button" class="quick-card quick-home" @click="goWelcome">
                        <span class="quick-copy">
                            <strong>초기화면으로 이동</strong>
                        </span>
                        <b>›</b>
                    </button>
                </nav>

                <div class="apartment-card">
                    <img src="@/assets/images/resident-welcome-apartment.png" alt="아파트 전경" />
                    <div class="apartment-caption">
                        <span class="shield">♥</span>
                        <span><strong>안전하고 편리한 주차 생활</strong><small>매너와 함께 만들어가는 쾌적한 아파트 환경</small></span>
                    </div>
                </div>

                <section
                    class="alert-card alert-card-link"
                    role="button"
                    tabindex="0"
                    @click="openNotifications"
                    @keyup.enter="openNotifications"
                >
                    <header>
                        <h2>최근 알림</h2>
                        <button type="button" @click.stop="openNotifications">전체 알림 보기 ›</button>
                    </header>
                    <ul>
                        <li v-for="alert in recentAlerts" :key="alert.key">
                            <span :class="['alert-icon', alert.color]">{{ alert.icon }}</span>
                            <span class="alert-copy"><strong>{{ alert.title }}</strong><small>{{ alert.description }}</small></span>
                            <time>{{ alert.time }}</time>
                        </li>
                    </ul>
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
                            <span class="log-label log-in">↓ 최근 입차</span>
                            <b>{{ latestInLog ? timeOnly(latestInLog.inTime) : "-" }}</b>
                            <span>{{ latestInLog?.carNo || "기록 없음" }}</span>
                            <small>{{ latestInLog ? `${latestInLog.parkingName || "주차장"} 입차` : "-" }}</small>
                        </div>
                        <div class="recent-log-item">
                            <span class="log-label log-out">↑ 최근 출차</span>
                            <b>{{ latestOutLog ? timeOnly(latestOutLog.outTime) : "-" }}</b>
                            <span>{{ latestOutLog?.carNo || "기록 없음" }}</span>
                            <small>{{ latestOutLog ? `${latestOutLog.parkingName || "주차장"} 출차` : "-" }}</small>
                        </div>
                    </div>
                </section>

                <section class="parking-card" role="button" tabindex="0" @click="openParkingStatus" @keyup.enter="openParkingStatus">
                    <h2>구역별 주차 현황</h2>
                    <div v-if="parkingStatusList.length" class="parking-zones">
                        <div
                            v-for="parking in parkingStatusList.slice(0, 4)"
                            :key="parking.parkingNo"
                            class="parking-zone"
                            :style="{ '--zone-color': parkingColor(parking.usageRate) }"
                        >
                            <div class="zone-heading">
                                <strong class="zone-name">{{ shortParkingName(parking.parkingName) }}</strong>
                                <span>{{ parking.parkingName }}</span>
                            </div>
                            <div class="zone-usage">
                                <span>현재 사용률</span>
                                <b>{{ parking.usageRate }}%</b>
                            </div>
                            <div class="zone-progress" role="progressbar" :aria-valuenow="parking.usageRate" aria-valuemin="0" aria-valuemax="100">
                                <span :style="{ width: `${parking.usageRate}%` }"></span>
                            </div>
                            <div class="zone-space-count">
                                <strong>{{ parking.available }}</strong>
                                <span>/ {{ parking.total }}면 가능</span>
                            </div>
                            <small class="zone-state" :class="{ crowded: parking.usageRate >= 80 }">{{ parking.available > 0 ? `주차 가능 ${parking.available}면` : "현재 만차" }}</small>
                            <div class="my-parked-cars">
                                <b v-for="carNo in parkedCarNumbers(parking)" :key="carNo">내 차 {{ carNo }}</b>
                                <span v-if="parkedCarNumbers(parking).length === 0">내 차량 없음</span>
                            </div>
                        </div>
                    </div>
                    <p v-else class="parking-empty">등록된 주차장이 없습니다.</p>
                </section>
            </div>
        </article>
    </section>
</template>

<script setup>
import { computed, onMounted, onUnmounted, ref } from "vue";
import { useRouter } from "vue-router";
import { storeToRefs } from "pinia";
import { useResidentDashboardStore } from "@/stores/residentDashboard";

const router = useRouter();
const dashboardStore = useResidentDashboardStore();
const now = ref(new Date());
let clockTimer;

const { loading, errorMessage, dashboard, normalVehicles, parkingStatusList } = storeToRefs(dashboardStore);

const formattedDate = computed(() => new Intl.DateTimeFormat("ko-KR", {
    year: "numeric", month: "2-digit", day: "2-digit", weekday: "short",
}).format(now.value));

const formattedTime = computed(() => new Intl.DateTimeFormat("ko-KR", {
    hour: "numeric", minute: "2-digit", hour12: true,
}).format(now.value));

const latestInLog = computed(() => dashboard.value.recentCarLogs.find((log) => log.inTime) || null);
const latestOutLog = computed(() => dashboard.value.recentCarLogs.find((log) => log.outTime) || null);

const recentAlerts = computed(() => {
    const alerts = dashboard.value.recentCarLogs.slice(0, 4).map((log, index) => {
        const isOut = Boolean(log.outTime);
        return {
            key: log.carLogNo || index,
            icon: isOut ? "↑" : "↓",
            color: isOut ? "red" : "orange",
            title: isOut ? "출차 완료" : "입차 완료",
            description: `${log.carNo || "차량"} · ${log.parkingName || "주차장"}`,
            time: timeOnly(log.outTime || log.inTime),
        };
    });

    if (!alerts.length) {
        alerts.push({ key: "empty", icon: "i", color: "blue", title: "새로운 알림이 없습니다.", description: "안전한 주차 생활을 응원합니다.", time: "" });
    }
    return alerts;
});

const timeOnly = (value) => value ? new Intl.DateTimeFormat("ko-KR", {
    hour: "2-digit", minute: "2-digit", hour12: false,
}).format(new Date(value)) : "-";

const shortParkingName = (name) => String(name || "P").replace("주차장", "") || "P";
const parkingColor = (rate) => rate >= 80 ? "#f04f4f" : rate >= 60 ? "#f29b18" : rate >= 40 ? "#35a554" : "#2f7ddd";
const parkedCarNumbers = (parking) => normalVehicles.value
    .filter((vehicle) => {
        return vehicle.parkingState === "PARKING"
            && String(vehicle.parkingName || "").trim() === String(parking.parkingName || "").trim();
    })
    .map((vehicle) => vehicle.carNo || vehicle.vehicleCarNo)
    .filter(Boolean);
const loadDashboard = () => dashboardStore.loadDashboard();
const openVehicleManagement = () => router.push("/resident/vehicles");
const openParkingStatus = () => router.push("/resident/parkings");
const openCarlogs = () => router.push("/resident/notice");
const openNotifications = () => router.push("/resident/notice");
const goMypage = () => router.push("/resident/mypage");
const goWelcome = () => router.push("/resident");

onMounted(() => {
    loadDashboard();
    clockTimer = window.setInterval(() => { now.value = new Date(); }, 1000);
});
onUnmounted(() => window.clearInterval(clockTimer));
</script>

<style scoped>
.resident-board-page { min-height: calc(100vh - 70px); display: grid; place-items: center; padding: 10px; background: #f2f7fc; }
.resident-board { width: min(1180px, 100%); padding: 20px; border: 1px solid #dce8f4; border-radius: 22px; background: #fff; box-shadow: 0 12px 35px rgba(38,78,118,.12); }
.board-header { display: flex; align-items: center; justify-content: space-between; margin-bottom: 14px; }
.board-welcome { display: flex; align-items: center; gap: 11px; }
.profile-icon { display: grid; place-items: center; width: 42px; height: 42px; border-radius: 50%; color: #fff; background: #3d86e6; font-size: 0; }
.profile-icon::before { content: "●"; font-size: 22px; }
.board-welcome h1 { margin: 0; color: #203c58; font-size: 27px; }
.welcome-actions { display: flex; align-items: center; gap: 7px; margin-left: 10px; }
.welcome-actions button { padding: 8px 12px; border: 1px solid #c9dcef; border-radius: 9px; color: #315c86; background: #f5faff; font-size: 12px; font-weight: 700; cursor: pointer; }
.welcome-actions button:hover { border-color: #76a9dd; color: #1768bd; background: #eaf4ff; }
.board-date-time { display: flex; align-items: center; gap: 14px; padding: 9px 15px; border-radius: 13px; color: #38536d; background: #f4f8fc; font-size: 16px; font-weight: 700; }
.board-date-time i { width: 1px; height: 16px; background: #d7e1eb; }
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
.board-bottom-grid { display: grid; grid-template-columns: 38% 1fr; gap: 14px; margin-top: 14px; }
.recent-log-card,.parking-card { padding: 13px 15px; border: 1px solid #dfe9f2; border-radius: 15px; background: #fff; }
.recent-log-link { cursor: pointer; transition: border-color .2s ease, box-shadow .2s ease, transform .2s ease; }
.recent-log-link:hover { border-color: #8bb9e5; box-shadow: 0 7px 18px rgba(39, 91, 140, .12); transform: translateY(-1px); }
.recent-log-link:focus-visible { outline: 3px solid rgba(47, 127, 223, .28); outline-offset: 2px; }
.recent-log-card h2,.parking-card h2 { margin-bottom: 10px; }
.recent-log-columns { display: grid; grid-template-columns: 1fr 1fr; }
.recent-log-item { display: grid; justify-items: center; gap: 3px; padding: 0 10px; border-right: 1px solid #e2e9f0; }
.recent-log-item:last-child { border-right: 0; }
.log-label { padding: 6px 14px; border-radius: 10px; font-size: 15px; font-weight: 800; }
.log-in { color: #176fd0; background: #e4f0ff; }
.log-out { color: #258b45; background: #e5f6e9; }
.recent-log-item b { color: #213d58; font-size: 24px; }.recent-log-item > span:not(.log-label) { color: #344f69; font-size: 14px; }.recent-log-item small { color: #6d91b8; font-size: 11px; }
.parking-card { cursor: pointer; transition: border-color .2s ease, box-shadow .2s ease, transform .2s ease; }
.parking-card:hover { border-color: #8bb9e5; box-shadow: 0 7px 18px rgba(39, 91, 140, .12); transform: translateY(-1px); }
.parking-card:focus-visible { outline: 3px solid rgba(47, 127, 223, .28); outline-offset: 2px; }
.parking-zones { display: grid; grid-template-columns: repeat(4,1fr); gap: 8px; }
.parking-zone { position: relative; display: grid; justify-items: center; gap: 4px; overflow: hidden; padding: 8px 6px 7px; border: 1px solid color-mix(in srgb, var(--zone-color) 26%, white); border-radius: 12px; background: linear-gradient(180deg, color-mix(in srgb, var(--zone-color) 8%, white), #fff 58%); box-shadow: 0 5px 12px rgba(48,78,108,.07); }
.parking-zone::before { content: ""; position: absolute; inset: 0 0 auto; height: 3px; background: var(--zone-color); }
.zone-heading { display: flex; align-items: center; gap: 5px; width: 100%; }
.zone-heading > span { overflow: hidden; color: #60758a; font-size: 9px; text-overflow: ellipsis; white-space: nowrap; }
.zone-name { display: grid; place-items: center; flex: 0 0 22px; height: 22px; border-radius: 50%; color: #fff; background: var(--zone-color); font-size: 10px; }
.zone-usage { display: flex; align-items: baseline; justify-content: space-between; width: 100%; margin-top: 3px; }
.zone-usage span { color: #73869a; font-size: 9px; }
.zone-usage b { color: var(--zone-color); font-size: 18px; }
.zone-progress { overflow: hidden; width: 100%; height: 10px; border-radius: 999px; background: #e7edf3; box-shadow: inset 0 1px 2px rgba(37,61,85,.1); }
.zone-progress span { display: block; height: 100%; border-radius: inherit; background: linear-gradient(90deg, color-mix(in srgb, var(--zone-color) 68%, white), var(--zone-color)); transition: width .35s ease; }
.zone-space-count { display: flex; align-items: baseline; gap: 2px; color: #6f8193; font-size: 9px; }
.zone-space-count strong { color: #2d4964; font-size: 15px; }
.zone-state { padding: 3px 10px; border-radius: 999px; color: #328847; background: #e7f6e8; font-size: 9px; font-weight: 700; }.zone-state.crowded { color: #c74a43; background: #ffe8e6; }
.my-parked-cars { display: grid; justify-items: center; gap: 2px; width: 100%; min-height: 17px; margin-top: 1px; }
.my-parked-cars b { width: 100%; padding: 3px 4px; border-radius: 6px; color: #1768ce; background: #e7f1ff; font-size: 9px; text-align: center; }
.my-parked-cars span { color: #9aa8b5; font-size: 8px; }
.parking-empty,.board-state { color: #667d92; text-align: center; }.board-state { padding: 40px; border-radius: 18px; background: #fff; }.board-error { color: #b83e3e; }.board-state button { padding: 8px 14px; border: 1px solid #ccddeb; border-radius: 10px; background: #fff; cursor: pointer; }
@media (max-height:760px) and (min-width:901px){
    .resident-board-page{min-height:calc(100vh - 58px);padding:5px 10px}
    .resident-board{padding:12px 14px;border-radius:17px}
    .board-header{margin-bottom:8px}
    .profile-icon{width:34px;height:34px}.profile-icon::before{font-size:18px}
    .board-welcome h1{font-size:22px}
    .welcome-actions{gap:5px;margin-left:6px}.welcome-actions button{padding:6px 9px;font-size:10px}
    .board-date-time{padding:7px 12px;font-size:14px}
    .board-top-grid{height:205px;gap:9px}
    .quick-menu{gap:8px}.quick-card{padding:8px 11px;border-radius:12px}
    .quick-icon{flex-basis:42px;height:42px;font-size:21px}
    .quick-copy strong{font-size:20px}.quick-card b{font-size:23px}
    .alert-card{padding:8px 10px}.alert-card li{padding:4px 0}.alert-icon{width:23px;height:23px}
    .board-bottom-grid{gap:9px;margin-top:9px}
    .recent-log-card,.parking-card{padding:9px 11px}
    .recent-log-card h2,.parking-card h2{margin-bottom:6px}
    .log-car{font-size:22px}.recent-log-item b{font-size:17px}
    .zone-usage b{font-size:16px}.zone-progress{height:8px}
    .parking-zone{gap:2px}.parking-zone small{padding:2px 9px}
}
@media (max-width:900px){.board-top-grid{grid-template-columns:1fr 1fr;height:auto}.alert-card{grid-column:1/-1}.apartment-card{min-height:220px}.board-bottom-grid{grid-template-columns:1fr}.parking-zones{min-height:120px}}
@media (max-width:600px){.resident-board-page{padding:6px}.resident-board{padding:14px}.board-header{align-items:flex-start;flex-direction:column;gap:10px}.board-welcome{align-items:flex-start;flex-wrap:wrap}.welcome-actions{width:100%;margin-left:0}.board-date-time{align-self:stretch;justify-content:center}.board-top-grid{grid-template-columns:1fr}.apartment-card{min-height:190px}.alert-card{grid-column:auto}.board-bottom-grid{grid-template-columns:1fr}.parking-zones{grid-template-columns:1fr 1fr;gap:14px}.parking-zone:nth-child(2){border-right:0}.quick-card{min-height:94px}}
</style>
