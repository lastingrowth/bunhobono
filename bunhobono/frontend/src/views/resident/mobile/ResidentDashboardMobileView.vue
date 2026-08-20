<template>
    <main class="mobile-dashboard">
        <div v-if="loading" class="mobile-state">대시보드를 불러오는 중입니다.</div>
        <div v-else-if="errorMessage" class="mobile-state mobile-error">
            <p>{{ errorMessage }}</p>
            <button type="button" @click="loadPage">다시 불러오기</button>
        </div>

        <template v-else>
            <section class="mobile-dashboard-greeting">
                <div class="greeting-copy">
                    <span class="residence-badge">{{ residenceText }}</span>
                    <strong>안녕하세요, {{ dashboard.member.memName || "입주민" }}님</strong>
                    <p>Bono와 쾌적한 주차관리서비스를 경험해보세요</p>
                </div>
                <div class="greeting-weather">
                    <span class="weather-icon">{{ greetingWeatherIcon }}</span>
                    <strong>{{ greetingTemperature }}</strong>
                    <span>{{ greetingWeatherText }}</span>
                </div>
            </section>

            <section class="vehicle-status-card">
                <div class="mobile-vehicle-status">
                    <small>차량현황</small>
                    <template v-if="mainVehicle">
                        <h1>{{ mainVehicle.carNo || mainVehicle.vehicleCarNo }}</h1>
                        <p>{{ mainVehicleStatus }}</p>
                        <span v-if="mainVehicleExpiry" class="vehicle-expiry-badge">{{ mainVehicleExpiry }}</span>
                        <button v-if="mainVehicle.parkingState === 'PARKING'" type="button" class="exit-button" @click="go('/resident/exit-request')">출차</button>
                        <button v-else type="button" class="vehicle-manage-button" @click="go('/resident/vehicles')">관리</button>
                    </template>
                    <template v-else>
                        <h1>차량 없음</h1>
                        <button type="button" class="vehicle-manage-button" @click="go('/resident/vehicles?mode=form')">등록</button>
                    </template>
                </div>
            </section>

            <section class="dashboard-card quick-card">
                <div class="quick-tabs" role="tablist" aria-label="자주 찾는 서비스 분류">
                    <button
                        v-for="tab in quickTabs"
                        :key="tab.value"
                        type="button"
                        role="tab"
                        :aria-selected="activeQuickTab === tab.value"
                        :class="{ active: activeQuickTab === tab.value }"
                        @click="selectQuickTab(tab.value)"
                    >{{ tab.label }}</button>
                </div>
                <div class="quick-grid">
                    <button v-for="item in visibleQuickMenus" :key="item.path || item.action" type="button" @click="openQuickMenu(item)">
                        <strong>{{ item.label }}</strong>
                        <small v-if="item.showVisitRemaining">{{ dashboard.visitRegistrationRemaining ?? 10 }}회 남음</small>
                    </button>
                </div>
            </section>

            <section class="dashboard-card parking-status-card">
                <header><h2>주차현황</h2></header>
                <div class="parking-donuts">
                    <article v-for="(parking, index) in mobileParkingStatus" :key="parking.parkingNo">
                        <div
                            class="parking-donut"
                            :style="{ '--parking-rate': `${parking.usageRate * 3.6}deg` }"
                            role="progressbar"
                            :aria-label="`${parking.parkingName} 사용률`"
                            :aria-valuenow="parking.usageRate"
                            aria-valuemin="0"
                            aria-valuemax="100"
                        >
                            <span>{{ parking.usageRate }}%</span>
                        </div>
                        <strong>{{ parkingLabel(index) }}</strong>
                        <small>가능 {{ parking.available }}면</small>
                        <div class="parking-car-numbers">
                            <span v-for="carNo in parkedCarNumbers(parking)" :key="carNo">✓ {{ carNo }}</span>
                            <span v-if="parkedCarNumbers(parking).length === 0" class="no-parking-car">주차 중 차량 없음</span>
                        </div>
                    </article>
                </div>
            </section>

            <section class="dashboard-card notice-card">
                <header>
                    <h2>공지사항</h2>
                    <button type="button" @click="go('/resident/boards')">더보기 〉</button>
                </header>
                <ul v-if="recentBoards.length">
                    <li v-for="board in recentBoards" :key="board.boardNo">
                        <button type="button" @click="go(`/resident/boards/${board.boardNo}/detail`)">
                            <span>
                                <strong>
                                    {{ board.title }}
                                    <em v-if="isNewBoard(board)">NEW</em>
                                </strong>
                            </span>
                            <b>〉</b>
                        </button>
                    </li>
                </ul>
                <p v-else class="empty-message">게시 중인 공지사항이 없습니다.</p>
            </section>

            <section v-if="posterBoards.length" class="notice-poster-section" aria-label="공지사항 포스터">
                <div class="notice-poster-track">
                    <button
                        v-for="board in posterBoards"
                        :key="board.boardNo"
                        type="button"
                        class="notice-poster-card"
                        @click="go(`/resident/boards/${board.boardNo}/detail`)"
                    >
                        <img :src="board.imageUrl" :alt="`${board.title} 포스터`">
                        <span class="notice-poster-copy">
                            <small>NOTICE</small>
                            <strong>{{ board.title }}</strong>
                        </span>
                    </button>
                </div>
                <p class="notice-poster-guide">옆으로 밀어 더 보기</p>
            </section>

        </template>

        <!-- [공지 팝업] 모바일 로그인 후 최신 공지 포스터를 보여준다. -->
        <dialog ref="boardPopupDialog" class="mobile-board-popup">
            <article v-if="popupBoard" class="mobile-board-popup-card">
                <button
                    type="button"
                    class="mobile-board-popup-poster"
                    :aria-label="`${popupBoard.title} 상세보기`"
                    @click="openPopupBoardDetail"
                >
                    <img :src="popupImageUrl" :alt="popupBoard.title">
                </button>

                <div class="mobile-board-popup-actions">
                    <label>
                        <input
                            v-model="hidePopupTodayChecked"
                            type="checkbox"
                            @change="changePopupTodayHidden"
                        >
                        <span>오늘 하루 보지 않기</span>
                    </label>
                    <button type="button" class="mobile-board-popup-close" @click="closeBoardPopup">
                        닫기
                    </button>
                </div>
            </article>
        </dialog>

    </main>
</template>

<script setup>
import { computed, onMounted, onUnmounted, ref } from "vue";
import { storeToRefs } from "pinia";
import { useRouter } from "vue-router";
import { useResidentDashboardStore } from "@/stores/residentDashboard";
import { useBoardStore } from "@/features/board/boardStore";
import { vehicleExpiryText } from "@/features/vehicle/vehicleFormat";

const router = useRouter();
const dashboardStore = useResidentDashboardStore();
const boardStore = useBoardStore();
const { loading, errorMessage, dashboard, residenceText, normalVehicles, parkingStatusList, weather } = storeToRefs(dashboardStore);
const vehicleStatusNow = ref(Date.now());
const boardPopupDialog = ref(null);
const popupBoard = ref(null);
const popupImageUrl = ref("");
const hidePopupTodayChecked = ref(false);
let vehicleStatusTimer;

const QUICK_TAB_STORAGE_KEY = "resident-dashboard-quick-tab";
const savedQuickTab = sessionStorage.getItem(QUICK_TAB_STORAGE_KEY);
const activeQuickTab = ref(["vehicle", "resident"].includes(savedQuickTab) ? savedQuickTab : "vehicle");
const quickTabs = [
    { label: "차량관리", value: "vehicle" },
    { label: "생활편의", value: "resident" }
];
const quickMenus = {
    vehicle: [
        { label: "방문차량 신청", path: "/resident/vehicles?mode=form", showVisitRemaining: true },
        { label: "차량 관리", path: "/resident/vehicles" },
        { label: "차량 알림", path: "/resident/vehicles?mode=notification" },
        { label: "입출차 내역", path: "/resident/carlogs" }
    ],
    resident: [
        { label: "공지사항", path: "/resident/boards" },
        { label: "1:1 문의", path: "/resident/inquiries" },
        { label: "요금납부", action: "none" },
        { label: "마이페이지", path: "/resident/mypage" }
    ]
};
const visibleQuickMenus = computed(() => quickMenus[activeQuickTab.value]);
const selectQuickTab = (tab) => {
    activeQuickTab.value = tab;
    sessionStorage.setItem(QUICK_TAB_STORAGE_KEY, tab);
};
const mobileParkingStatus = computed(() => parkingStatusList.value.slice(0, 2));
const greetingTemperature = computed(() => weather.value.temperature === null || weather.value.temperature === undefined ? "--°C" : `${weather.value.temperature}°C`);
const greetingWeatherText = computed(() => weather.value.precipitation === "강수 없음" ? "맑음" : weather.value.precipitation || "맑음");
const greetingWeatherIcon = computed(() => {
    if (weather.value.precipitation === "눈") return "❄️";
    if (weather.value.precipitation && weather.value.precipitation !== "강수 없음") return "🌧️";
    return "☀️";
});
const parkingLabel = (index) => `B${index + 1} 주차장`;
const parkedCarNumbers = (parking) => normalVehicles.value
    .filter((vehicle) => vehicle.parkingState === "PARKING"
        && String(vehicle.parkingName || "").trim() === String(parking.parkingName || "").trim())
    .map((vehicle) => vehicle.carNo || vehicle.vehicleCarNo)
    .filter(Boolean);

const recentBoards = computed(() => boardStore.list.slice(0, 3));
const posterBoards = computed(() => boardStore.list
    .filter((board) => board.hasImage && boardStore.imageUrls[board.boardNo])
    .slice(0, 5)
    .map((board) => ({ ...board, imageUrl: boardStore.imageUrls[board.boardNo] })));
const mainVehicle = computed(() => {
    return normalVehicles.value.find((vehicle) => vehicle.parkingState === "PARKING")
        || normalVehicles.value[0]
        || null;
});
const mainVehicleStatus = computed(() => {
    if (mainVehicle.value?.parkingState !== "PARKING") return "미주차";

    const location = mainVehicle.value.parkingLocation;
    if (location?.spaceCode) {
        return `${location.parkingCode ? `${location.parkingCode} · ` : ""}${location.spaceCode}`;
    }

    return "주차 중";
});
const mainVehicleExpiry = computed(() => {
    if (!mainVehicle.value) return "";
    const text = vehicleExpiryText(mainVehicle.value, vehicleStatusNow.value);
    return text.startsWith("만료 임박") ? text : "";
});
const isNewBoard = (board) => {
    const createdAt = new Date(board.createdAt).getTime();
    return !Number.isNaN(createdAt) && Date.now() - createdAt >= 0 && Date.now() - createdAt < 259200000;
};

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
    if (boardNo) go(`/resident/boards/${boardNo}/detail`);
};

// [공지 팝업] 체크한 공지를 오늘 하루 동안 다시 표시하지 않는다.
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

// [공지 팝업] 이미지가 있는 최신 공지사항을 모바일에서 한 번 표시한다.
const showLatestBoardPopup = () => {
    const board = boardStore.list.find(
        (item) => item.hasImage && boardStore.imageUrls[item.boardNo]
    );
    if (!board) return;

    const hiddenValue = localStorage.getItem("residentBoardPopupHidden");
    if (hiddenValue === `${board.boardNo}|${todayKey()}`) return;

    popupBoard.value = board;
    popupImageUrl.value = boardStore.imageUrls[board.boardNo];
    hidePopupTodayChecked.value = false;
    boardPopupDialog.value?.showModal();
};

const go = (path) => router.push(path);
const openAiChat = () => window.dispatchEvent(new CustomEvent("open-ai-chat"));
const openQuickMenu = (item) => {
    sessionStorage.setItem(QUICK_TAB_STORAGE_KEY, activeQuickTab.value);

    if (item.action === "none") {
        return;
    }

    if (item.action === "aiChat") {
        openAiChat();
        return;
    }

    go(item.path);
};
const loadPage = async () => {
    await Promise.all([dashboardStore.loadDashboard(), boardStore.loadList().catch(() => [])]);
    const imageBoards = boardStore.list.filter((board) => board.hasImage).slice(0, 5);
    await Promise.all(imageBoards.map((board) => boardStore.loadImage(board.boardNo).catch(() => "")));
    showLatestBoardPopup();
};

onMounted(() => {
    loadPage();
    vehicleStatusTimer = window.setInterval(() => {
        vehicleStatusNow.value = Date.now();
    }, 60000);
});
onUnmounted(() => window.clearInterval(vehicleStatusTimer));
</script>

<style scoped>
.mobile-dashboard{min-height:0;padding:6px 14px 16px;color:#23364a;background-image:linear-gradient(rgba(243,248,252,.82),rgba(238,245,250,.9)),url("@/assets/images/back.jpg");background-position:center;background-size:cover;background-repeat:no-repeat}
.mobile-state{margin:24px 0;padding:28px 18px;border-radius:22px;text-align:center;background:#fff}.mobile-error{color:#b43f47}.mobile-error button{margin-top:14px;padding:10px 18px;border:0;border-radius:10px;color:#fff;background:#2f83d5}
.mobile-dashboard-greeting{display:grid;grid-template-columns:minmax(0,1fr) auto;align-items:center;gap:14px;margin:0 3px 12px;padding:10px 2px;color:#60778d}.greeting-copy{display:flex;min-width:0;align-items:flex-start;flex-direction:column}.residence-badge{margin-bottom:6px;padding:4px 9px;border:1px solid #8ebce3;border-radius:999px;color:#2871ad!important;font-size:13px;font-weight:850!important;background:#dceeff}.greeting-copy strong{color:#1f3850;font-size:20px;font-weight:950;line-height:1.3}.greeting-copy p{margin:5px 0 0;color:#60778d;font-size:14px;font-weight:650;line-height:1.4;word-break:keep-all}.greeting-weather{display:grid;min-width:76px;justify-items:center;padding:4px 2px}.greeting-weather .weather-icon{font-size:25px;line-height:1}.greeting-weather strong{margin-top:4px;color:#315b80;font-size:17px;font-weight:900}.greeting-weather span:last-child{color:#71889c;font-size:13px;font-weight:700}
.vehicle-status-card,.dashboard-card,.notice-poster-card{border:1px solid rgba(157,192,222,.8);border-radius:24px;background:#fff;box-shadow:0 9px 26px rgba(35,91,142,.11)}
.vehicle-status-card{overflow:hidden;text-align:center}.mobile-vehicle-status{display:flex;min-width:0;align-items:center;flex-direction:column;justify-content:center;padding:13px 18px}.mobile-vehicle-status small{margin-bottom:2px;color:#48789f;font-size:13px;font-weight:850}.mobile-vehicle-status h1{max-width:100%;margin:2px 0;font-size:22px;overflow-wrap:anywhere}.mobile-vehicle-status p{margin:0;color:#2671ae;font-size:13px;font-weight:800}.vehicle-expiry-badge{margin-top:5px;padding:4px 8px;border-radius:999px;color:#c33d45;font-size:11px;font-weight:900;background:#ffe8ea}.exit-button,.vehicle-manage-button{width:100%;margin-top:7px;border:0;border-radius:12px;color:#fff;font-weight:900}.exit-button{min-height:54px;color:#fff!important;font-size:18px!important;background:#1677d2!important}.vehicle-manage-button{min-height:40px;font-size:15px;background:#4f7595}
.dashboard-card{margin-top:9px;padding:20px}.dashboard-card header{display:flex;align-items:center;justify-content:space-between;margin-bottom:12px}.dashboard-card h2{margin:0;font-size:20px}.dashboard-card header button{border:0;color:#64778a;font-weight:700;background:transparent}
.quick-card{padding:18px;background:#fff}.quick-tabs{display:grid;grid-template-columns:1fr 1fr;margin-bottom:12px;padding:4px;border-radius:14px;background:#e3eef7}.quick-tabs button{min-height:44px;border:0;border-radius:11px;color:#537996;font-size:16px;font-weight:850;text-align:center;background:transparent}.quick-tabs button.active{color:#fff;background:#247dca;box-shadow:0 4px 10px rgba(25,93,151,.2)}.quick-grid{display:grid;grid-template-columns:repeat(2,minmax(0,1fr));overflow:hidden;border:1px solid #8fb9db;border-radius:10px;background:#fff}.quick-grid button{display:flex;min-width:0;min-height:76px;align-items:center;justify-content:center;flex-direction:column;padding:10px 5px;border:0;border-right:1px solid #8fb9db;border-bottom:1px solid #8fb9db;border-radius:0;outline:0;text-align:center;box-shadow:none;background:#fff;-webkit-tap-highlight-color:transparent}.quick-grid button:is(:focus,:focus-visible,:active){outline:0;background:#fff!important;box-shadow:none!important}.quick-grid button:nth-child(2n){border-right:0}.quick-grid button:last-child,.quick-grid button:nth-last-child(2):nth-child(odd){border-bottom:0}.quick-grid strong{display:block;width:100%;color:#124f84;font-size:15px;line-height:1.25;text-align:center;word-break:keep-all}.quick-grid small{margin-top:5px;color:#e05259;font-size:11px;font-weight:850}@media (hover:hover) and (pointer:fine){.quick-grid button:hover{background:#eef7fd}}
.parking-status-card{padding:16px 18px}.parking-status-card header{margin-bottom:8px}.parking-status-card h2{font-size:18px}.parking-donuts{display:grid;grid-template-columns:1fr 1fr;gap:14px}.parking-donuts article{display:flex;min-width:0;align-items:center;flex-direction:column;padding:10px 6px;border-radius:14px;background:#f3f8fc}.parking-donut{display:grid;width:88px;aspect-ratio:1;place-items:center;border-radius:50%;background:conic-gradient(#1677d2 0 var(--parking-rate),#dceaf5 var(--parking-rate) 360deg)}.parking-donut::before{content:"";grid-area:1/1;width:62px;aspect-ratio:1;border-radius:50%;background:#fff}.parking-donut span{z-index:1;grid-area:1/1;color:#155d98;font-size:17px;font-weight:900}.parking-donuts article>strong{margin-top:8px;color:#264e70;font-size:14px}.parking-donuts article>small{margin-top:2px;color:#6a8296;font-size:11px}.parking-car-numbers{display:grid;width:100%;gap:4px;margin-top:7px}.parking-car-numbers span{overflow:hidden;padding:5px 4px;border-radius:7px;color:#1768a7;font-size:11px;font-weight:850;text-align:center;text-overflow:ellipsis;white-space:nowrap;background:#deeffc}.parking-car-numbers .no-parking-car{color:#8293a0;font-weight:650;background:#e9f0f5}
.notice-card ul{margin:0;padding:0;list-style:none}.notice-card li+li{border-top:1px solid #edf0f3}.notice-card li>button{display:flex;width:100%;align-items:center;justify-content:space-between;padding:14px 2px;border:0;text-align:left;background:transparent}.notice-card li span{min-width:0}.notice-card li strong{display:block;overflow:hidden;font-size:15px;text-overflow:ellipsis;white-space:nowrap}.notice-card em{display:inline-block;margin-left:6px;padding:2px 6px;border-radius:7px;vertical-align:middle;color:#e44848;font-size:10px;font-style:normal;font-weight:900;background:#fff0f1}.notice-card li b{color:#98a3ad}.empty-message{color:#8796a5;text-align:center}
.notice-poster-section{width:100%;margin-top:9px}.notice-poster-track{display:flex;width:100%;gap:12px;padding:0 0 8px;overflow-x:auto;scroll-snap-type:x mandatory;scrollbar-width:none;-webkit-overflow-scrolling:touch}.notice-poster-track::-webkit-scrollbar{display:none}.notice-poster-card{position:relative;min-width:100%;height:250px;overflow:hidden;padding:0;opacity:1!important;scroll-snap-align:start;-webkit-tap-highlight-color:transparent}.notice-poster-card:is(:hover,:focus,:focus-visible,:active){border-color:rgba(157,192,222,.8)!important;outline:0!important;opacity:1!important;background:#fff!important;box-shadow:0 9px 26px rgba(35,91,142,.11)!important;transform:none!important}.notice-poster-card img{display:block;width:100%;height:100%;object-fit:contain;opacity:1!important;background:#fff}.notice-poster-copy{position:absolute;inset:auto 0 0;display:block;padding:28px 15px 14px;color:#fff;text-align:left;background:linear-gradient(transparent,rgba(13,52,85,.88))}.notice-poster-copy small{display:block;margin-bottom:3px;color:#a9d9ff;font-size:10px;font-weight:900;letter-spacing:.12em}.notice-poster-copy strong{display:block;overflow:hidden;font-size:16px;text-overflow:ellipsis;white-space:nowrap}.notice-poster-guide{margin:2px 2px 0;color:#7890a4;font-size:12px;font-weight:700;text-align:right}
.mobile-board-popup{width:min(420px,calc(100vw - 64px));max-width:calc(100vw - 64px);max-height:70dvh;padding:0;overflow:hidden;border:0;border-radius:12px;background:#fff;box-shadow:0 18px 54px rgba(15,35,52,.42)}
.mobile-board-popup::backdrop{background:rgba(13,25,36,.58)}
.mobile-board-popup-card{display:flex;width:100%;max-height:70dvh;flex-direction:column;margin:0;overflow:hidden;background:#fff}
.mobile-board-popup-poster{display:block;width:100%;max-height:calc(70dvh - 52px);margin:0;padding:0;overflow:hidden;border:0;border-radius:0;outline:0;background:#fff;line-height:0}
.mobile-board-popup-poster img{display:block;width:100%;max-height:calc(70dvh - 52px);object-fit:contain;background:#fff}
.mobile-board-popup-actions{display:flex;min-height:52px;align-items:center;justify-content:flex-end;gap:8px;padding:8px 10px;background:#fff}
.mobile-board-popup-actions label{display:inline-flex;align-items:center;gap:5px;color:#5e7182;font-size:12px;cursor:pointer}
.mobile-board-popup-actions input{width:15px;height:15px;margin:0;accent-color:#2f83d5}
.mobile-board-popup-close{padding:7px 10px;border:1px solid #2f83d5;border-radius:8px;color:#fff;font-size:12px;font-weight:800;background:#2f83d5}
</style>
