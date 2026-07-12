<template>
    <section class="resident-dashboard">
        <div class="resident-dashboard-header">
            <div>
                <h2>입주민 대시보드</h2>

                <p v-if="resVehicleStore.member.memName">
                    {{ resVehicleStore.member.memName }}님, 안녕하세요.
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
                        <strong>{{ resVehicleStore.member.memName || "-" }}</strong>
                    </li>

                    <li>
                        <span>동·호수</span>
                        <strong>{{ residenceText }}</strong>
                    </li>

                    <li>
                        <span>연락처</span>
                        <strong>{{ resVehicleStore.member.memPhone || "-" }}</strong>
                    </li>

                    <li>
                        <span>회원 상태</span>
                        <strong>{{ resVehicleStore.member.memStatus || "-" }}</strong>
                    </li>
                </ul>
            </article>

            <!-- 본인 차량과 방문 차량 수 -->
            <div class="resident-summary">
                <article class="resident-summary-card">
                    <span>본인 차량</span>
                    <strong>{{ normalVehicles.length }}대</strong>
                </article>

                <article class="resident-summary-card">
                    <span>방문 차량</span>
                    <strong>{{ visitVehicles.length }}대</strong>
                </article>
            </div>

            <!-- 본인 차량 목록 -->
            <article class="resident-vehicle-card">
                <div class="resident-section-header">
                    <h3>본인 차량 목록</h3>

                    <button type="button" @click="router.push('/resident/vehicles')">
                        차량 관리
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

                    <button type="button" @click="router.push('/resident/vehicles')">
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
        </template>
    </section>
</template>

<script setup>
import { computed, onMounted, ref } from "vue";
import { useRouter } from "vue-router";

import { useResVehicleStore } from "@/features/resVehicle/resVehicleStore";

const router = useRouter();
const resVehicleStore = useResVehicleStore();

const loading = ref(false);
const errorMessage = ref("");

// 로그인한 입주민의 동·호수
const residenceText = computed(() => {
    const member = resVehicleStore.member;

    if (!member.memDong || !member.memHo) {
        return "-";
    }

    return `${member.memDong}동 ${member.memHo}호`;
});

// 로그인한 입주민이 등록한 본인 차량
const normalVehicles = computed(() => {
    return resVehicleStore.vehicleList.filter((vehicle) => {
        return vehicle.vehicleType === "normal";
    });
});

// 로그인한 입주민이 등록한 방문 차량
const visitVehicles = computed(() => {
    return resVehicleStore.vehicleList.filter((vehicle) => {
        return vehicle.vehicleType === "visit";
    });
});

// 본인 정보와 본인이 등록한 차량 목록 조회
const loadDashboard = async () => {
    loading.value = true;
    errorMessage.value = "";

    try {
        await resVehicleStore.loadMyInfo();
        await resVehicleStore.loadVehicleList();
    } catch (error) {
        console.error(error);
        errorMessage.value = "입주민 정보를 불러오지 못했습니다.";
    } finally {
        loading.value = false;
    }
};

onMounted(loadDashboard);
</script>