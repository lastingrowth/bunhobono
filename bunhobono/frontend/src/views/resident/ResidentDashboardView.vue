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

            <!-- 좌측: 차량 목록 / 우측: 주차장 사용 현황 2열 배치. -->
            <div class="resident-main-grid">
                <div class="resident-vehicle-column">
                    <!-- 본인 차량 목록 -->
                    <article class="resident-vehicle-card">
                        <div class="resident-section-header">
                            <h3>본인 차량 목록 <span class="resident-list-count">{{ dashboard.normalVehicleCount }}대</span></h3>
                            <button type="button" @click="openVehicleManagement('normal')">내 차량 관리</button>
                        </div>
                        <table>
                            <thead><tr><th>차량번호</th><th>상태</th><th>승인일</th></tr></thead>
                            <tbody>
                                <tr v-for="vehicle in normalVehicles" :key="vehicle.vehicleCarNo">
                                    <td>{{ vehicle.carNo || "-" }}</td>
                                    <td>{{ vehicle.vehicleStatusText || vehicle.vehicleStatus || "-" }}</td>
                                    <td>{{ vehicle.approvedAtText || "-" }}</td>
                                </tr>
                                <tr v-if="normalVehicles.length === 0"><td colspan="3">등록한 본인 차량이 없습니다.</td></tr>
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
                            <thead><tr><th>차량번호</th><th>방문 종료</th><th>남은 시간</th></tr></thead>
                            <tbody>
                                <tr v-for="vehicle in visitVehicles" :key="vehicle.vehicleCarNo">
                                    <td>{{ vehicle.carNo || "-" }}</td>
                                    <td>{{ vehicle.endDateText || "-" }}</td>
                                    <td>{{ vehicle.remainingTimeText || "-" }}</td>
                                </tr>
                                <tr v-if="visitVehicles.length === 0"><td colspan="3">등록한 방문 차량이 없습니다.</td></tr>
                            </tbody>
                        </table>
                    </article>
                </div>

                <!-- 구역별 사용량을 막대그래프로 표현. -->
                <article class="parking-usage-card">
                    <div class="resident-section-header">
                        <h3>주차장 사용 현황</h3>
                    </div>
                    <div class="parking-zone-list">
                        <div v-for="zone in parkingZones" :key="zone.name" class="parking-zone-row">
                            <strong>{{ zone.name }}</strong>
                            <div class="parking-progress" :aria-label="`${zone.name} 사용률 ${zone.usageRate}%`">
                                <span
                                    :class="{ 'parking-progress-danger': zone.usageRate > 70 }"
                                    :style="{ width: `${zone.usageRate}%` }"
                                ></span>
                            </div>
                            <span class="parking-usage-number">{{ zone.usedSpaces }} / {{ zone.totalSpaces }}</span>
                            <span class="parking-available">주차 가능 {{ zone.availableSpaces }}칸</span>
                        </div>
                    </div>
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
import { computed, onMounted } from "vue";
import { useRouter } from "vue-router";
import { storeToRefs } from "pinia";
import { useMemStore } from "@/features/member/memStore";

const router = useRouter();
const memberStore = useMemStore();
const { loading, errorMessage, dashboard } = storeToRefs(memberStore);

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

// 주차장 조회 결과의 앞 네 항목을 A~D 구역 현황으로 변환한다.
const parkingZones = computed(() => {
    return ["A구역", "B구역", "C구역", "D구역"].map((name, index) => {
        const parking = dashboard.value.parkings[index] || {};
        return {
            name,
            availableSpaces: parking.availableSpaces ?? 0,
            totalSpaces: parking.parkingSpaces ?? 0,
            usedSpaces: Math.max((parking.parkingSpaces ?? 0) - (parking.availableSpaces ?? 0), 0),
            usageRate: parking.parkingSpaces
                ? Math.round(((parking.parkingSpaces - parking.availableSpaces) / parking.parkingSpaces) * 100)
                : 0
        };
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

// 화면은 Store 액션만 호출하고 DB 데이터 조회·조합은 memStore가 담당한다.
const loadDashboard = () => memberStore.loadDashboard();

onMounted(loadDashboard);
</script>
