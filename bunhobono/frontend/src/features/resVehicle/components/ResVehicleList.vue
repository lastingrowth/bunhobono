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
        <col v-if="showManage || showCancel || showExtend" class="vehicle-col-manage">
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
          <th v-if="showManage || showCancel || showExtend">관리</th>
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
          <td>
            <span class="vehicle-cell-lines vehicle-cell-status-text">
              <span>{{ splitStatusText(vehicle.vehicleStatusText || vehicle.vehicleStatus)[0] }}</span>
              <span v-if="splitStatusText(vehicle.vehicleStatusText || vehicle.vehicleStatus)[1]">
                {{ splitStatusText(vehicle.vehicleStatusText || vehicle.vehicleStatus)[1] }}
              </span>
            </span>
          </td>
          <td>
            <span class="vehicle-cell-lines">
              <span>{{ splitDateTime(vehicle.approvedAtText)[0] }}</span>
              <span v-if="splitDateTime(vehicle.approvedAtText)[1]">
                {{ splitDateTime(vehicle.approvedAtText)[1] }}
              </span>
            </span>
          </td>
          <td>
            <span class="vehicle-cell-period-text">
              {{ vehicle.periodText || '-' }}
            </span>
          </td>
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

          <td v-if="showManage || showCancel || showExtend">
            <template v-if="showManage">
              <button @click="$emit('edit', vehicle)">수정</button>
              <button @click="$emit('remove', vehicle.vehicleCarNo)">삭제</button>
            </template>

            <button
              v-if="showExtend && canExtendNormalVehicle(vehicle)"
              type="button"
              @click="$emit('extend-normal', vehicle)"
            >
              기간 연장
            </button>

            <button
              v-if="showCancel && !vehicle.inTime"
              type="button"
              @click="$emit('edit-visit-time', vehicle)"
            >
             수정
            </button>

            <button
              v-if="showCancel && !vehicle.inTime"
              type="button"
              @click="$emit('cancel-visit', vehicle)"
            >
             취소
            </button>

            <button
              v-if="showCancel && vehicle.inTime && !vehicle.outTime"
              type="button"
              @click="$emit('extend-visit-one-day', vehicle)"
            >
              1일 연장
            </button>

            <span v-else-if="showCancel && vehicle.inTime">출차 완료</span>
          </td>
        </tr>

        <tr v-if="vehicles.length === 0">
          <td :colspan="showManage || showCancel || showExtend ? 8 : 7" align="center">
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
  },
  showCancel: {
    type: Boolean,
    default: false
  },
  showExtend: {
    type: Boolean,
    default: false
  }
});

defineEmits(["edit", "remove", "extend-normal", "edit-visit-time", "cancel-visit", "extend-visit-one-day", "empty-action"]);

const normalizedText = (value) => String(value || "-").trim();

// 일반차량은 만기 전 14일 이내에서만 기간을 연장할 수 있습니다.
const canExtendNormalVehicle = (vehicle) => {
  const endDate = new Date(vehicle.endDate);

  if (Number.isNaN(endDate.getTime())) {
    return false;
  }

  const remainingMilliseconds = endDate.getTime() - Date.now();
  const fourteenDays = 14 * 24 * 60 * 60 * 1000;

  return remainingMilliseconds >= 0
    && remainingMilliseconds <= fourteenDays;
};

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

// 만기 상태는 의미가 잘 보이도록 두 줄로 나눕니다.
const splitStatusText = (value) => {
  const text = normalizedText(value);

  if (text === "미입차 만기") {
    return ["미입차", "만기"];
  }

  if (text === "주차시간 만기") {
    return ["주차시간", "만기"];
  }

  return [text, ""];
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

.vehicle-col-number { width: 12%; }
.vehicle-col-type { width: 10%; }
.vehicle-col-status { width: 11%; }
.vehicle-col-date { width: 12%; }
.vehicle-col-period { width: 13%; }
.vehicle-col-remaining { width: 16%; }
.vehicle-col-manage { width: 14%; }

.resident-vehicle-table--manage .vehicle-col-number { width: 11%; }
.resident-vehicle-table--manage .vehicle-col-type { width: 12%; }
.resident-vehicle-table--manage .vehicle-col-status { width: 10%; }
.resident-vehicle-table--manage .vehicle-col-date { width: 12%; }
.resident-vehicle-table--manage .vehicle-col-period { width: 12%; }
.resident-vehicle-table--manage .vehicle-col-remaining { width: 15%; }

.resident-vehicle-table th,
.resident-vehicle-table td {
  padding: 12px 8px;
  text-align: center;
  font-size: clamp(13px, 0.95vw, 16px);
  line-height: 1.45;
}

.resident-vehicle-table th {
  height: 50px;
}

.resident-vehicle-table .vehicle-data-row,
.resident-vehicle-table .vehicle-data-row td {
  height: 88px;
}

.vehicle-cell-one-line,
.vehicle-cell-lines {
  word-break: keep-all;
  white-space: nowrap;
}

.vehicle-cell-period-text {
  max-width: 100%;
  display: block;
  overflow-wrap: normal;
  word-break: keep-all;
  white-space: normal;
  line-height: 1.45;
}

.vehicle-cell-status-text {
  align-items: center;
  text-align: center;
}

.vehicle-cell-lines {
  min-height: 48px;
  display: flex;
  flex-direction: column;
  justify-content: center;
  gap: 4px;
  line-height: 1.35;
}

.resident-vehicle-table td button {
  width: 100%;
  min-width: 0;
  max-width: 100%;
  box-sizing: border-box;
  padding: 10px 12px;
  white-space: nowrap;
  font-size: clamp(13px, 0.9vw, 15px);
}

.resident-vehicle-table td button + button {
  margin-top: 6px;
  margin-left: 0;
}

.empty-action-link {
  margin-left: 5px;
  color: var(--resident-accent);
  font-size: inherit;
  font-weight: 700;
  text-decoration: underline;
  text-underline-offset: 3px;
}

.empty-action-link:hover {
  color: var(--resident-accent-hover);
}

@media (any-pointer: coarse) and (max-width: 820px),
       (any-pointer: coarse) and (max-height: 820px) {
  .resident-vehicle-table-wrap { overflow: visible; }
  .resident-vehicle-table,
  .resident-vehicle-table tbody,
  .resident-vehicle-table tr,
  .resident-vehicle-table td { display: block; width: 100%; }
  .resident-vehicle-table { border: 0; box-shadow: none; background: transparent; }
  .resident-vehicle-table colgroup,
  .resident-vehicle-table thead { display: none; }
  .resident-vehicle-table tbody { display: grid; gap: 12px; }
  .resident-vehicle-table .vehicle-data-row {
    height: auto;
    padding: 14px;
    border: 1px solid #d9e5ee;
    border-radius: 12px;
    background: #fff;
  }
  .resident-vehicle-table .vehicle-data-row td {
    height: auto;
    padding: 7px 0;
    border: 0;
    text-align: left;
  }
  .resident-vehicle-table .vehicle-data-row td::before {
    display: inline-block;
    min-width: 72px;
    margin-right: 8px;
    color: #71879a;
    font-size: 12px;
    font-weight: 800;
  }
  .resident-vehicle-table .vehicle-data-row td:nth-child(1)::before { content: "차량번호"; }
  .resident-vehicle-table .vehicle-data-row td:nth-child(2)::before { content: "구분"; }
  .resident-vehicle-table .vehicle-data-row td:nth-child(3)::before { content: "상태"; }
  .resident-vehicle-table .vehicle-data-row td:nth-child(4)::before { content: "신청일"; }
  .resident-vehicle-table .vehicle-data-row td:nth-child(5)::before { content: "기간"; }
  .resident-vehicle-table .vehicle-data-row td:nth-child(6)::before { content: "만료일"; }
  .resident-vehicle-table .vehicle-data-row td:nth-child(7)::before { content: "남은기간"; }
  .resident-vehicle-table .vehicle-data-row td:nth-child(8)::before { content: "관리"; }
  .vehicle-cell-one-line,
  .vehicle-cell-lines { min-height: 0; display: inline-flex; white-space: normal; vertical-align: top; }
  .resident-vehicle-table .vehicle-data-row td:last-child button { width: 100%; min-height: 42px; margin-top: 5px; }
}
</style>
