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

            <!-- 본인 차량과 방문 차량 수 -->
            <div class="resident-summary">
                <article class="resident-summary-card">
                    <span>본인 차량</span>
                    <strong>{{ dashboard.normalVehicleCount }}대</strong>
                </article>

                <article class="resident-summary-card">
                    <span>방문 차량</span>
                    <strong>{{ dashboard.visitVehicleCount }}대</strong>
                </article>

                <article class="resident-summary-card">
                    <span>전체 주차칸</span>
                    <strong>330칸</strong>
                </article>

                <article class="resident-summary-card">
                    <span>주차 가능</span>
                    <strong>296칸</strong>
                </article>
            </div>

            <!-- 본인 차량 목록 -->
            <article class="resident-vehicle-card">
                <div class="resident-section-header">
                    <h3>본인 차량 목록</h3>

                    <button type="button" @click="openVehicleManagement('normal')">
                        내 차량 관리
                    </button>
                </div>

                <table>
                    <thead>
                        <tr>
                            <th>차량번호</th>
                            <th>차량종류</th>
                            <th>등록일</th>
                            <th>승인일</th>
                        </tr>
                    </thead>

                    <tbody>
                        <tr
                            v-for="vehicle in normalVehicles"
                            :key="vehicle.vehicleCarNo"
                        >
                            <td>{{ vehicle.carNo || "-" }}</td>
                            <td>{{ vehicle.vehicleTypeText || vehicle.vehicleType || "-" }}</td>
                            <td>{{ vehicle.startDateText || "-" }}</td>
                            <td>{{ vehicle.approvedAtText || "-" }}</td>
                        </tr>

                        <tr v-if="normalVehicles.length === 0">
                            <td colspan="4">
                                등록한 본인 차량이 없습니다.
                            </td>
                        </tr>
                    </tbody>
                </table>
            </article>

            <!-- 방문 차량 목록 -->
            <article class="resident-vehicle-card resident-visit-card">
                <div class="resident-section-header">
                    <h3>방문 차량 목록</h3>

                    <button type="button" @click="openVehicleManagement('visit')">
                        방문 차량 관리
                    </button>
                </div>

                <table>
                    <thead>
                        <tr>
                            <th>차량번호</th>
                            <th>방문 시작</th>
                            <th>방문 종료</th>
                            <th>남은 시간</th>
                        </tr>
                    </thead>

                    <tbody>
                        <tr
                            v-for="vehicle in visitVehicles"
                            :key="vehicle.vehicleCarNo"
                        >
                            <td>{{ vehicle.carNo || "-" }}</td>
                            <td>{{ vehicle.startDateText || "-" }}</td>
                            <td>{{ vehicle.endDateText || "-" }}</td>
                            <td>{{ vehicle.remainingTimeText || "-" }}</td>
                        </tr>

                        <tr v-if="visitVehicles.length === 0">
                            <td colspan="4">
                                등록한 방문 차량이 없습니다.
                            </td>
                        </tr>
                    </tbody>
                </table>
            </article>

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
import { computed, onMounted, ref } from "vue";
import { useRouter } from "vue-router";

import { getResidentDashboard } from "@/features/resident-dashboard/residentDashboardApi";
import { toVehicleView } from "@/features/vehicle/vehicleFormat";

const router = useRouter();

const loading = ref(false);
const errorMessage = ref("");
const dashboard = ref({
    member: {},
    normalVehicleCount: 0,
    visitVehicleCount: 0,
    totalParkingSpaces: 0,
    availableParkingSpaces: 0,
    vehicles: [],
    parkings: [],
    recentCarLogs: []
});

// 차량 관리 화면에 표시할 차량 종류를 URL Query로 전달한다.
const openVehicleManagement = (type) => {
    router.push({
        path: "/resident/vehicles",
        query: { type }
    });
};

// 로그인한 입주민의 동·호수
const residenceText = computed(() => {
    const member = dashboard.value.member;

    if (!member.memDong || !member.memHo) {
        return "-";
    }

    return `${member.memDong}동 ${member.memHo}호`;
});

// 로그인한 입주민이 등록한 본인 차량
const normalVehicles = computed(() => {
    return dashboard.value.vehicles.filter((vehicle) => {
        return vehicle.vehicleType === "normal";
    });
});

// 로그인한 입주민이 등록한 방문 차량
const visitVehicles = computed(() => {
    return dashboard.value.vehicles.filter((vehicle) => {
        return vehicle.vehicleType === "visit";
    });
});

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

// 본인 정보와 본인이 등록한 차량 목록 조회
// 여러 화면 Store를 직접 조합하지 않고 대시보드 전용 집계 API를 한 번만 호출한다.
const loadDashboard = async () => {
    loading.value = true;
    errorMessage.value = "";

    try {
        const response = await getResidentDashboard();
        dashboard.value = {
            ...dashboard.value,
            ...response.data,
            member: response.data.member || {},
            vehicles: (response.data.vehicles || []).map(toVehicleView),
            parkings: response.data.parkings || [],
            recentCarLogs: response.data.recentCarLogs || []
        };
    } catch (error) {
        console.error(error);
        errorMessage.value = "입주민 정보를 불러오지 못했습니다.";
    } finally {
        loading.value = false;
    }
};

onMounted(loadDashboard);
</script>
