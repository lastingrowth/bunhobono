<template>
  <div class="resident-vehicle-table-wrap">
    <table
      class="resident-vehicle-table"
      :class="{ 'resident-vehicle-table--manage': showManage }"
    >
      <colgroup>
        <col class="vehicle-col-number">
        <col class="vehicle-col-type">
        <col class="vehicle-col-status">
        <col class="vehicle-col-date">
        <col class="vehicle-col-period">
        <col class="vehicle-col-date">
        <col class="vehicle-col-remaining">
        <col v-if="showManage" class="vehicle-col-manage">
      </colgroup>

      <thead>
        <tr>
          <th>차량번호</th>
          <th>차량종류</th>
          <th>승인상태</th>
          <th>승인일</th>
          <th>등록기간</th>
          <th>만기일</th>
          <th>남은기간</th>
          <th v-if="showManage">관리</th>
        </tr>
      </thead>

      <tbody>
        <tr
          v-for="vehicle in vehicles"
          :key="vehicle.vehicleCarNo"
          class="vehicle-data-row"
        >
          <td><span class="vehicle-cell-one-line">{{ vehicle.carNo }}</span></td>
          <td><span class="vehicle-cell-one-line">{{ vehicle.vehicleTypeText || vehicle.vehicleType }}</span></td>
          <td><span class="vehicle-cell-one-line">{{ vehicle.vehicleStatusText || vehicle.vehicleStatus }}</span></td>
          <td>
            <span class="vehicle-cell-lines">
              <span>{{ splitDateTime(vehicle.approvedAtText)[0] }}</span>
              <span v-if="splitDateTime(vehicle.approvedAtText)[1]">
                {{ splitDateTime(vehicle.approvedAtText)[1] }}
              </span>
            </span>
          </td>
          <td><span class="vehicle-cell-one-line">{{ vehicle.periodText || '-' }}</span></td>
          <td>
            <span class="vehicle-cell-lines">
              <span>{{ splitDateTime(vehicle.endDateText)[0] }}</span>
              <span v-if="splitDateTime(vehicle.endDateText)[1]">
                {{ splitDateTime(vehicle.endDateText)[1] }}
              </span>
            </span>
          </td>
          <td>
            <span class="vehicle-cell-lines">
              <span>{{ splitRemainingTime(vehicle.remainingTimeText)[0] }}</span>
              <span v-if="splitRemainingTime(vehicle.remainingTimeText)[1]">
                {{ splitRemainingTime(vehicle.remainingTimeText)[1] }}
              </span>
            </span>
          </td>

          <td v-if="showManage">
            <button @click="$emit('edit', vehicle)">수정</button>
            <button @click="$emit('remove', vehicle.vehicleCarNo)">삭제</button>
          </td>
        </tr>

        <tr v-if="vehicles.length === 0">
          <td :colspan="showManage ? 8 : 7" align="center">
            {{ emptyMessage }}
            <a
              v-if="emptyActionLabel"
              href="#resident-contact"
              class="empty-action-link"
              @click.prevent="$emit('empty-action')"
            >
              {{ emptyActionLabel }}
            </a>
          </td>
        </tr>
      </tbody>
    </table>
  </div>
</template>

<script setup>
defineProps({
  vehicles: {
    type: Array,
    default: () => []
  },
  emptyMessage: {
    type: String,
    default: "조회된 차량이 없습니다."
  },
  emptyActionLabel: {
    type: String,
    default: ""
  },
  showManage: {
    type: Boolean,
    default: false
  }
});

defineEmits(["edit", "remove", "empty-action"]);

const normalizedText = (value) => String(value || "-").trim();

// 날짜와 시간을 항상 의미가 분리된 두 줄로 표시합니다.
const splitDateTime = (value) => {
  const parts = normalizedText(value).split(/\s+/);
  const time = parts.at(-1);

  if (parts.length === 2 && /^\d{2}:\d{2}:\d{2}$/.test(time)) {
    return [parts[0], time];
  }

  return [parts.join(" "), ""];
};

// 일수와 시간·분을 분리하되, 상태 문구는 한 줄 그대로 유지합니다.
const splitRemainingTime = (value) => {
  const parts = normalizedText(value).split(/\s+/);
  const dayIndex = parts.findIndex((part) => part.endsWith("일"));

  if (dayIndex < 0 || dayIndex === parts.length - 1) {
    return [parts.join(" "), ""];
  }

  return [
    parts.slice(0, dayIndex + 1).join(" "),
    parts.slice(dayIndex + 1).join(" ")
  ];
};
</script>

<style scoped>
.resident-vehicle-table-wrap {
  width: 100%;
  overflow: visible;
}

.resident-vehicle-table {
  width: 100%;
  min-width: 0;
  table-layout: fixed;
}

.resident-vehicle-table--manage {
  min-width: 0;
}

.vehicle-col-number { width: 13%; }
.vehicle-col-type { width: 14%; }
.vehicle-col-status { width: 12%; }
.vehicle-col-date { width: 14%; }
.vehicle-col-period { width: 14%; }
.vehicle-col-remaining { width: 19%; }
.vehicle-col-manage { width: 16%; }

.resident-vehicle-table--manage .vehicle-col-number { width: 11%; }
.resident-vehicle-table--manage .vehicle-col-type { width: 12%; }
.resident-vehicle-table--manage .vehicle-col-status { width: 10%; }
.resident-vehicle-table--manage .vehicle-col-date { width: 12%; }
.resident-vehicle-table--manage .vehicle-col-period { width: 12%; }
.resident-vehicle-table--manage .vehicle-col-remaining { width: 15%; }

.resident-vehicle-table th,
.resident-vehicle-table td {
  padding-top: 8px;
  padding-right: 6px;
  padding-bottom: 8px;
  padding-left: 6px;
  text-align: center;
}

.resident-vehicle-table th {
  height: 50px;
}

.resident-vehicle-table .vehicle-data-row,
.resident-vehicle-table .vehicle-data-row td {
  height: 74px;
}

.vehicle-cell-one-line,
.vehicle-cell-lines {
  word-break: keep-all;
  white-space: nowrap;
}

.vehicle-cell-lines {
  min-height: 44px;
  display: flex;
  flex-direction: column;
  justify-content: center;
  gap: 4px;
  line-height: 1.35;
}

.empty-action-link {
  margin-left: 5px;
  color: #287fd5;
  font-size: inherit;
  font-weight: 700;
  text-decoration: underline;
  text-underline-offset: 3px;
}

.empty-action-link:hover {
  color: #175fa9;
}
</style>
