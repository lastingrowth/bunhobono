<template>
  <div class="resident-vehicle-table-wrap">
    <table
      class="resident-vehicle-table"
      :class="{ 'resident-vehicle-table--manage': showManage }"
    >
      <colgroup>
        <col class="vehicle-col-number">
        <col class="vehicle-col-date">
        <col class="vehicle-col-date">
        <col class="vehicle-col-period">
        <col class="vehicle-col-remaining">
        <col v-if="showManage || showCancel || showExtend" class="vehicle-col-manage">
      </colgroup>

      <thead>
        <tr>
          <th>차량번호</th>
          <th>등록일</th>
          <th>만기일</th>
          <th>등록기간</th>
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
          <td>
            <span class="vehicle-cell-lines vehicle-cell-date">
              <span v-if="showExtend">{{ formatKoreanDate(vehicle.approvedAt || vehicle.approvedAtText) }}</span>
              <span v-else>{{ splitKoreanDateTime(vehicle.approvedAtText || vehicle.approvedAt)[0] }}</span>
              <span v-if="!showExtend && splitKoreanDateTime(vehicle.approvedAtText || vehicle.approvedAt)[1]">
                {{ splitKoreanDateTime(vehicle.approvedAtText || vehicle.approvedAt)[1] }}
              </span>
            </span>
          </td>
          <td>
            <span class="vehicle-cell-lines vehicle-cell-date">
              <span v-if="showExtend">{{ formatKoreanDate(vehicle.endDate || vehicle.endDateText) }}</span>
              <span v-else>{{ splitKoreanDateTime(vehicle.endDateText || vehicle.realEndDate || vehicle.endDate)[0] }}</span>
              <span v-if="!showExtend && splitKoreanDateTime(vehicle.endDateText || vehicle.realEndDate || vehicle.endDate)[1]">
                {{ splitKoreanDateTime(vehicle.endDateText || vehicle.realEndDate || vehicle.endDate)[1] }}
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
              v-if="showExtend"
              type="button"
              class="extend-action-button"
              :disabled="!canExtendNormalVehicle(vehicle)"
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
              class="extend-action-button"
              @click="$emit('extend-visit-hours', vehicle)"
            >
              시간 연장
            </button>

            <span v-else-if="showCancel && vehicle.inTime">출차 완료</span>
          </td>
        </tr>

        <tr v-if="vehicles.length === 0">
          <td :colspan="showManage || showCancel || showExtend ? 6 : 5" align="center">
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

defineEmits(["edit", "remove", "extend-normal", "edit-visit-time", "cancel-visit", "extend-visit-hours", "empty-action"]);

const normalizedText = (value) => String(value || "-").trim();

// 본인 차량의 승인일과 만기일은 시간 없이 연·월·일만 표시합니다.
const formatKoreanDate = (value) => {
  if (!value) return "-";

  const text = String(value).trim();
  const matched = text.match(/^(\d{2,4})[.\-/]\s*(\d{1,2})[.\-/]\s*(\d{1,2})/);

  if (matched) {
    const year = matched[1].length === 2 ? `20${matched[1]}` : matched[1];
    const month = matched[2].padStart(2, "0");
    const day = matched[3].padStart(2, "0");
    return `${year}년 ${month}월 ${day}일`;
  }

  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return "-";

  const month = String(date.getMonth() + 1).padStart(2, "0");
  const day = String(date.getDate()).padStart(2, "0");
  return `${date.getFullYear()}년 ${month}월 ${day}일`;
};

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

// 방문 차량 날짜는 연·월·일과 시간을 각각 한 줄씩 표시합니다.
const splitKoreanDateTime = (value) => {
  if (!value) return ["-", ""];

  const text = String(value).trim();
  const time = text.match(/(?:^|[T\s])(\d{1,2}:\d{2}(?::\d{2})?)/)?.[1] || "";
  const date = formatKoreanDate(value);

  // "입차 X"처럼 날짜가 아닌 상태 문구는 기존 문구를 유지합니다.
  if (date === "-") return [text, ""];

  return [date, time];
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

.vehicle-col-number { width: 15%; }
.vehicle-col-date { width: 17%; }
.vehicle-col-period { width: 16%; }
.vehicle-col-remaining { width: 18%; }
.vehicle-col-manage { width: 17%; }

.resident-vehicle-table--manage .vehicle-col-number { width: 14%; }
.resident-vehicle-table--manage .vehicle-col-date { width: 15%; }
.resident-vehicle-table--manage .vehicle-col-period { width: 14%; }
.resident-vehicle-table--manage .vehicle-col-remaining { width: 15%; }

.resident-vehicle-table th,
.resident-vehicle-table td {
  padding: 8px 7px;
  text-align: center;
  font-size: clamp(13px, 0.95vw, 16px);
  line-height: 1.45;
}

.resident-vehicle-table th {
  height: 50px;
}

.resident-vehicle-table .vehicle-data-row,
.resident-vehicle-table .vehicle-data-row td {
  height: 68px;
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

.vehicle-cell-lines {
  min-height: 38px;
  display: flex;
  flex-direction: column;
  justify-content: center;
  gap: 2px;
  line-height: 1.3;
}

.vehicle-cell-date {
  font-size: clamp(12px, 0.85vw, 14px);
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

.resident-vehicle-table td .extend-action-button {
  width: auto;
  min-height: 0;
  padding: 2px 0;
  border: 0;
  border-radius: 0;
  color: #1677d2;
  background: transparent;
  box-shadow: none;
  font-weight: 800;
}

.resident-vehicle-table td .extend-action-button:hover {
  color: #0f5fae;
  background: transparent;
  text-decoration: underline;
  text-underline-offset: 3px;
}

.resident-vehicle-table td .extend-action-button:disabled,
.resident-vehicle-table td .extend-action-button:disabled:hover {
  color: #a8b3bc;
  background: transparent;
  opacity: 1;
  cursor: not-allowed;
  text-decoration: none;
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
  .resident-vehicle-table .vehicle-data-row td:nth-child(2)::before { content: "등록일"; }
  .resident-vehicle-table .vehicle-data-row td:nth-child(3)::before { content: "만기일"; }
  .resident-vehicle-table .vehicle-data-row td:nth-child(4)::before { content: "등록기간"; }
  .resident-vehicle-table .vehicle-data-row td:nth-child(5)::before { content: "남은기간"; }
  .resident-vehicle-table .vehicle-data-row td:nth-child(6)::before { content: "관리"; }
  .vehicle-cell-one-line,
  .vehicle-cell-lines { min-height: 0; display: inline-flex; white-space: normal; vertical-align: top; }
  .resident-vehicle-table .vehicle-data-row td:last-child button { width: 100%; min-height: 42px; margin-top: 5px; }
  .resident-vehicle-table .vehicle-data-row td:last-child .extend-action-button { width: auto; min-height: 0; margin-top: 5px; }
}
</style>
