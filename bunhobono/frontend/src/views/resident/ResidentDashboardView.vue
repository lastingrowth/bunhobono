<template>
    <section class="resident-dashboard">
        <div class="resident-dashboard-header">
            <div>
                <h2>입주민 대시보드</h2>

                <p v-if="dashboard.member.memName">
                    {{ dashboard.member.memName }}님, 안녕하세요.
                </p>
            </div>

            <button type="button" @click="loadDashboard">
                새로고침
            </button>
        </div>

        <p v-if="loading">
            입주민 정보를 불러오는 중입니다.
        </p>

        <p v-else-if="errorMessage">
            {{ errorMessage }}
        </p>

        <template v-else>
            <!-- 로그인한 입주민 정보 -->
            <article class="resident-member-card">
                <div class="resident-section-header">
                    <h3>본인 정보</h3>

                    <button type="button" @click="router.push('/resident/mypage')">
                        내 정보 관리
                    </button>
                </div>

                <ul class="resident-member-list">
                    <li>
                        <span>이름</span>
                        <strong>{{ dashboard.member.memName || "-" }}</strong>
                    </li>

                    <li>
                        <span>동·호수</span>
                        <strong>{{ residenceText }}</strong>
                    </li>

                    <li>
                        <span>연락처</span>
                        <strong>{{ dashboard.member.memPhone || "-" }}</strong>
                    </li>

                    <li>
                        <span>회원 상태</span>
                        <strong>{{ dashboard.member.memStatus || "-" }}</strong>
                    </li>
                </ul>
            </article>

            <!-- 좌측: 차량 목록 / 우측: 관리자 대시보드와 같은 주차장 현황 2열 배치. -->
            <div class="resident-main-grid">
                <div class="resident-vehicle-column">
                    <!-- 본인 차량 목록 -->
                    <article class="resident-vehicle-card">
                        <div class="resident-section-header">
                            <h3>본인 차량 목록 <span class="resident-list-count">{{ dashboard.normalVehicleCount }}대</span></h3>
                            <button type="button" @click="openVehicleManagement('normal')">내 차량 관리</button>
                        </div>
                        <table>
                            <thead>
                                <tr>
                                    <th>차량번호</th>
                                    <th>승인여부</th>
                                    <th>기간</th>
                                    <th>주차현황</th>
                                    <th>주차장소</th>
                                </tr>
                            </thead>
                            <tbody>
                                <tr v-for="vehicle in normalVehicles" :key="vehicle.vehicleCarNo">
                                    <td>{{ vehicle.carNo || "-" }}</td>
                                    <td>{{ vehicle.vehicleStatusText || vehicle.vehicleStatus || "-" }}</td>
                                    <td>{{ vehiclePeriodText(vehicle) }}</td>
                                    <td>{{ parkingStateText(vehicle.parkingState) }}</td>
                                    <td>{{ vehicle.parkingName || "-" }}</td>
                                </tr>
                                <tr v-if="normalVehicles.length === 0"><td colspan="5">등록한 본인 차량이 없습니다.</td></tr>
                            </tbody>
                        </table>
                    </article>

                    <!-- 방문 차량 목록 -->
                    <article class="resident-vehicle-card resident-visit-card">
                        <div class="resident-section-header">
                            <h3>방문 차량 목록 <span class="resident-list-count">{{ dashboard.visitVehicleCount }}대</span></h3>
                            <button type="button" @click="openVehicleManagement('visit')">방문 차량 관리</button>
                        </div>
                        <table>
                            <thead>
                                <tr>
                                    <th>차량번호</th>
                                    <th>승인여부</th>
                                    <th>기간</th>
                                    <th>주차현황</th>
                                    <th>주차장소</th>
                                </tr>
                            </thead>
                            <tbody>
                                <tr v-for="vehicle in visitVehicles" :key="vehicle.vehicleCarNo">
                                    <td>{{ vehicle.carNo || "-" }}</td>
                                    <td>{{ vehicle.vehicleStatusText || vehicle.vehicleStatus || "-" }}</td>
                                    <td>{{ vehiclePeriodText(vehicle) }}</td>
                                    <td>{{ parkingStateText(vehicle.parkingState) }}</td>
                                    <td>{{ vehicle.parkingName || "-" }}</td>
                                </tr>
                                <tr v-if="visitVehicles.length === 0"><td colspan="5">등록한 방문 차량이 없습니다.</td></tr>
                            </tbody>
                        </table>
                    </article>
                </div>

                <!-- 관리자 대시보드의 원형 주차장 현황을 입주민 화면에도 표시한다. -->
                <article
                    class="parking-usage-card parking-usage-link"
                    role="button"
                    tabindex="0"
                    @click="openParkingStatus"
                    @keyup.enter="openParkingStatus"
                >
                    <div class="resident-section-header">
                        <h3>주차장 현황</h3>
                    </div>

                    <div v-if="parkingStatusList.length > 0" class="parking-zone-grid">
                        <div
                            v-for="parking in parkingStatusList"
                            :key="parking.parkingNo"
                            class="parking-zone"
                        >
                            <strong class="parking-zone-name">{{ parking.parkingName }}</strong>
                            <div
                                class="parking-zone-donut"
                                :style="{ '--parking-rate': `${parking.usageRate}%` }"
                            >
                                <strong>{{ parking.usageRate }}%</strong>
                            </div>
                            <span class="parking-zone-count">{{ parking.occupied }} / {{ parking.total }}면</span>
                            <span class="parking-zone-available">주차 가능 {{ parking.available }}면</span>
                        </div>
                    </div>

                    <p v-else class="parking-empty">등록된 주차장이 없습니다.</p>
                </article>
            </div>

            <!-- 본인 차량 최근 입출차 및 주차 위치 -->
            <article class="resident-vehicle-card resident-visit-card">
                <div class="resident-section-header">
                    <h3>최근 입출차 기록</h3>
                </div>

                <table>
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
                        <tr v-for="log in dashboard.recentCarLogs" :key="log.carLogNo">
                            <td>{{ log.carNo || "-" }}</td>
                            <td>{{ log.parkingName || "-" }}</td>
                            <td>{{ dateTimeText(log.inTime) }}</td>
                            <td>{{ dateTimeText(log.outTime) }}</td>
                            <td>{{ log.parkingState === "PARKING" ? "주차 중" : "출차" }}</td>
                        </tr>
                        <tr v-if="dashboard.recentCarLogs.length === 0">
                            <td colspan="5">최근 입출차 기록이 없습니다.</td>
                        </tr>
                    </tbody>
                </table>
            </article>
        </template>
    </section>
</template>

<script setup>
import { onMounted } from "vue";
import { useRouter } from "vue-router";
import { storeToRefs } from "pinia";
import { useResidentDashboardStore } from "@/stores/residentDashboard";

const router = useRouter();
const dashboardStore = useResidentDashboardStore();

const { loading, errorMessage, dashboard, residenceText, normalVehicles, visitVehicles, parkingStatusList } = storeToRefs(dashboardStore);

// 차량 관리 화면에 표시할 차량 종류를 URL Query로 전달한다.
const openVehicleManagement = (type) => {
    router.push({
        path: "/resident/vehicles",
        query: { type }
    });
};

// =====
// 좌측 메뉴의 주차현황 화면으로 이동한다.
const openParkingStatus = () => {
    router.push("/resident/parkings");
};

// 일반 차량은 승인 시작일부터, 방문 차량은 시작·종료 기간을 표시한다.
const vehiclePeriodText = (vehicle) => {
    if (!vehicle.startDate && !vehicle.endDate) return "-";

    const start = dateTimeText(vehicle.startDate);
    const end = dateTimeText(vehicle.endDate);

    return vehicle.endDate ? `${start} ~ ${end}` : `${start} ~`;
};

// 가장 최근 입출차 기록으로 현재 주차 상태를 표시한다.
const parkingStateText = (state) => ({
    PARKING: "주차 중",
    OUT: "출차",
    NONE: "미주차"
}[state] || "미주차");


const dateTimeText = (value) => {
    if (!value) return "-";
    return new Intl.DateTimeFormat("ko-KR", {
        year: "2-digit",
        month: "2-digit",
        day: "2-digit",
        hour: "2-digit",
        minute: "2-digit"
    }).format(new Date(value));
};

// 화면은 Store 액션만 호출하고 데이터 조회·조합은 대시보드 Store가 담당한다.
const loadDashboard = () => dashboardStore.loadDashboard();

onMounted(loadDashboard);
</script>
