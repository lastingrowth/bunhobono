<template>
  <div>
    <h3>승인 대기 차량</h3>

    <table border="">
      <thead>
        <tr>
          <th>차량번호</th>
          <th>차량종류</th>
          <th>승인상태</th>
          <th>등록기간</th>
          <th>관리</th>
        </tr>
      </thead>

      <tbody>
        <tr v-for="vehicle in paginatedItems" :key="vehicle.vehicleCarNo">
          <td>{{ vehicle.carNo }}</td>
          <td>{{ vehicle.vehicleTypeText || vehicle.vehicleType }}</td>
          <td>{{ vehicle.vehicleStatusText || vehicle.vehicleStatus }}</td>

          <td>
            <select
              v-if="vehicle.vehicleType === 'normal'"
              v-model.number="vehicle.periodMonths"
              style="width: 120px;"
            >
              <option :value="1">1개월</option>
              <option :value="3">3개월</option>
              <option :value="6">6개월</option>
              <option :value="12">12개월</option>
            </select>

            <span v-if="vehicle.vehicleType === 'visit'">
              <input
                type="number"
                min="1"
                v-model.number="vehicle.periodHours"
                :placeholder="String(getRequestedVisitHours(vehicle))"
                style="width: 60px;"
              >
              시간
              <span>
                신청 {{ getRequestedVisitHours(vehicle) }}시간
              </span>
            </span>
          </td>

          <td>
            <button @click="approve(vehicle)">승인</button>
            <button @click="reject(vehicle)">반려</button>
            <button @click="expire(vehicle)">만료</button>
          </td>
        </tr>

        <tr v-if="vehicles.length === 0">
          <td colspan="5" align="center">승인 대기 차량이 없습니다.</td>
        </tr>
      </tbody>
    </table>
    <Pagination
      :current-page="currentPage"
      :total-pages="totalPages"
      :page-numbers="pageNumbers"
      @change-page="setPage"/>
  </div>
</template>

<script setup>
import { usePagination } from "@/shared/pagination/usePagination";
import { useVehicleStore } from "../vehicleStore";
import { computed } from "vue";
import Pagination from "@/shared/pagination/Pagination.vue";


const props = defineProps({
  vehicles: {
    type : Array,
    default : () => []
  }
});

const vehicleStore = useVehicleStore();
const approveVehicles = computed(() => {
  return props.vehicles;
});

const pageSize = 10;

const {
  currentPage,
  totalPages,
  pageNumbers,
  paginatedItems,
  setPage
} = usePagination(approveVehicles, pageSize);

function getRequestedVisitHours(vehicle) {
  if (vehicle.periodHours) {
    return vehicle.periodHours;
  }

  if (!vehicle.startDate || !vehicle.endDate) {
    return 2;
  }

  const start = new Date(vehicle.startDate);
  const end = new Date(vehicle.endDate);

  const diffMs = end.getTime() - start.getTime();
  const diffHours = Math.round(diffMs / 1000 / 60 / 60);

  return diffHours > 0 ? diffHours : 2;
}

async function approve(vehicle) {
  const data = {
    vehicleStatus: "APPROVED",
    vehicleType: vehicle.vehicleType
  };

  if (vehicle.vehicleType === "normal") {
    const startDate = new Date();
    const endDate = new Date(startDate);

    endDate.setMonth(endDate.getMonth() + Number(vehicle.periodMonths || 3));

    data.startDate = formatDateTimeLocalValue(startDate);
    data.endDate = formatDateTimeLocalValue(endDate);
  }

  if (vehicle.vehicleType === "visit") {
    const startDate = vehicle.startDate
      ? new Date(vehicle.startDate)
      : new Date();

    const endDate = new Date(startDate);
    const hours = Number(vehicle.periodHours || getRequestedVisitHours(vehicle));

    endDate.setHours(endDate.getHours() + hours);

    data.startDate = formatDateTimeLocalValue(startDate);
    data.endDate = formatDateTimeLocalValue(endDate);
  }

  await vehicleStore.changeVehicleApproveStatus(vehicle.vehicleCarNo, data);
}

async function reject(vehicle) {
  if (!confirm("신청을 반려하고 목록에서 삭제할까요?")) {
    return;
  }

  await vehicleStore.removeVehicle(vehicle.vehicleCarNo);
  await vehicleStore.loadVehicleApproveList();
}

async function expire(vehicle) {
  await vehicleStore.changeVehicleApproveStatus(vehicle.vehicleCarNo, {
    vehicleStatus: "EXPIRED",
    vehicleType: vehicle.vehicleType
  });
}

function formatDateTimeLocalValue(date) {
  const yyyy = date.getFullYear();
  const mm = String(date.getMonth() + 1).padStart(2, "0");
  const dd = String(date.getDate()).padStart(2, "0");
  const hh = String(date.getHours()).padStart(2, "0");
  const mi = String(date.getMinutes()).padStart(2, "0");

  return `${yyyy}-${mm}-${dd}T${hh}:${mi}`;
}
</script>