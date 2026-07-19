<template>
  <main class="trash-detail-page">
    <section class="trash-detail-dialog">
    <div class="detail-header">
      <div>
        <h2>지난기록 상세</h2>
        <p>삭제된 데이터의 상세 내용을 확인합니다.</p>
      </div>

      <button type="button" @click="goList">
        목록으로
      </button>
    </div>

    <div v-if="!trashStore.trashDetail" class="loading">
      데이터를 불러오는 중입니다.
    </div>

    <template v-else>
      <!-- 삭제 정보 -->
      <section class="detail-card">
        <h3>삭제 정보</h3>

        <table class="detail-table">
          <tbody>
            <tr
              v-for="row in commonRows"
              :key="row.label"
            >
              <th>{{ row.label }}</th>
              <td>{{ row.value }}</td>
            </tr>
          </tbody>
        </table>
      </section>

      <!-- 원본 데이터 정보 -->
      <section class="detail-card">
        <h3>{{ detailTitle }}</h3>

        <table class="detail-table">
          <tbody>
            <tr
              v-for="row in detailRows"
              :key="row.label"
            >
              <th>{{ row.label }}</th>
              <td>{{ row.value }}</td>
            </tr>
          </tbody>
        </table>
      </section>

      <!-- 개발 확인용 JSON -->
      <details class="json-card">
        <summary>원본 JSON 보기</summary>

        <pre>{{ formatJson(trashStore.trashDetail.dataJson) }}</pre>
      </details>

      <div class="detail-actions">
        <button type="button" @click="goList">
          목록으로
        </button>

        <button
          type="button"
          class="restore-button"
          :disabled="restoring"
          @click="handleRestore"
        >
          {{ restoring ? "복원 중..." : "이 기록 복원" }}
        </button>
      </div>
    </template>
    </section>
  </main>
</template>

<script setup>
import { computed, onMounted, ref } from "vue";
import { useRoute, useRouter } from "vue-router";

import { useTrashStore } from "./trashStore";

import {
  getDataTypeText,
  getDeleteTypeText,
  formatDate,
  formatJson,
  showValue,
  getRecognitionText,
  showConfidence,
  getCarKindText,
  parseDataJson,
} from "./trashFormat";

const route = useRoute();
const router = useRouter();
const trashStore = useTrashStore();
const restoring = ref(false);

const originalData = computed(() => {
  return parseDataJson(
    trashStore.trashDetail?.dataJson
  );
});

const commonRows = computed(() => {
  const detail = trashStore.trashDetail;

  if (!detail) {
    return [];
  }

  return [
    {
      label: "지난기록 번호",
      value: showValue(detail.trashNo),
    },
    {
      label: "데이터 종류",
      value: getDataTypeText(detail.dataType),
    },
    {
      label: "기존 데이터 번호",
      value: showValue(detail.originalNo),
    },
    {
      label: "삭제 방식",
      value: getDeleteTypeText(detail.deleteType),
    },
    {
      label: "삭제 일시",
      value: formatDate(detail.deletedAt),
    },
    {
      label: "영구삭제 예정일",
      value: formatDate(detail.purgeAt),
    },
  ];
});

const detailTitle = computed(() => {
  const dataType = trashStore.trashDetail?.dataType;

  const title = {
    CAMERA_DATA: "카메라 데이터",
    CAR_LOG: "입출차 기록",
    NOTICE: "알림 정보",
  };

  return title[dataType] ?? "원본 데이터";
});

const detailRows = computed(() => {
  const dataType = trashStore.trashDetail?.dataType;
  const data = originalData.value;

  if (dataType === "CAMERA_DATA") {
    return [
      {
        label: "카메라 데이터 번호",
        value: showValue(data.camera_data_no),
      },
      {
        label: "카메라 번호",
        value: showValue(data.camera_no),
      },
      {
        label: "등록 차량 번호",
        value: showValue(data.vehicle_car_no),
      },
      {
        label: "촬영 차량번호",
        value: showValue(data.car_no),
      },
      {
        label: "촬영 일시",
        value: formatDate(data.capture_time),
      },
      {
        label: "인식 여부",
        value: getRecognitionText(data.recognition_state),
      },
      {
        label: "인식 정확도",
        value: showConfidence(data.confidence_score),
      },
      {
        label: "이미지 경로",
        value: showValue(data.image_path),
      },
    ];
  }

  if (dataType === "CAR_LOG") {
    return [
      {
        label: "카로그 번호",
        value: showValue(data.car_log_no),
      },
      {
        label: "차량번호",
        value: showValue(
          data.snapshot_car_no ?? data.car_no
        ),
      },
      {
        label: "등록 차량 번호",
        value: showValue(data.vehicle_car_no),
      },
      {
        label: "카메라 데이터 번호",
        value: showValue(data.camera_data_no),
      },
      {
        label: "입차 게이트 번호",
        value: showValue(data.in_gate_no),
      },
      {
        label: "입차 일시",
        value: formatDate(data.in_time),
      },
      {
        label: "출차 게이트 번호",
        value: showValue(data.out_gate_no),
      },
      {
        label: "출차 일시",
        value: formatDate(data.out_time),
      },
    ];
  }

  if (dataType === "NOTICE") {
    return [
      {
        label: "알림 번호",
        value: showValue(data.notice_no),
      },
      {
        label: "카로그 번호",
        value: showValue(
          data.car_log_no ??
          data.snapshot_car_log_no
        ),
      },
      {
        label: "등록 차량번호",
        value: showValue(
          data.snapshot_registered_car_no
        ),
      },
      {
        label: "촬영 차량번호",
        value: showValue(
          data.snapshot_captured_car_no
        ),
      },
      {
        label: "차량 구분",
        value: getCarKindText(
          data.snapshot_car_kind
        ),
      },
      {
        label: "주차장",
        value: showValue(
          data.snapshot_parking_name
        ),
      },
      {
        label: "입차 일시",
        value: formatDate(
          data.snapshot_in_time
        ),
      },
      {
        label: "알림 감지 일시",
        value: formatDate(data.detect_at),
      },
      {
        label: "주차 일수",
        value: showValue(data.stay_days),
      },
      {
        label: "처리 상태",
        value: showValue(data.alert_stat),
      },
      {
        label: "처리 관리자 번호",
        value: showValue(
          data.handled_by_member_no
        ),
      },
      {
        label: "처리 일시",
        value: formatDate(data.handled_at),
      },
    ];
  }

  return [];
});

const goList = () => {
  router.push("/admin/trash");
};

const handleRestore = async () => {
  const trashNo = Number(route.params.trashNo);

  if (!window.confirm("이 기록을 복원하시겠습니까?")) {
    return;
  }

  restoring.value = true;

  try {
    const result = await trashStore.restoreTrashItem(trashNo);

    if (!result?.success) {
      throw new Error("복원 응답을 확인할 수 없습니다.");
    }

    window.alert("기록이 복원되었습니다.");
    router.push("/admin/trash");
  } catch (error) {
    window.alert(
      error.response?.data?.message
      ?? error.message
      ?? "기록 복원에 실패했습니다."
    );
  } finally {
    restoring.value = false;
  }
};

onMounted(async () => {
  await trashStore.loadTrashDetail(
    route.params.trashNo
  );
});
</script>

<style scoped>
.trash-detail-page {
  width: 100%;
  max-width: 900px;
  margin: 0 auto;
}

.trash-detail-dialog {
  overflow: hidden;
  border-radius: 10px;
  background: var(--bg-header);
  box-shadow: 0 20px 48px rgba(35, 52, 66, 0.18);
}

.detail-header {
  margin: 0;
  padding: 22px 24px;
  border-bottom: 1px solid var(--border-color);
}

.detail-header p {
  margin: 6px 0 0;
  color: var(--text-muted);
}

.detail-card {
  padding: 20px 24px 0;
}

.detail-card h3 {
  margin: 0 0 12px;
}

.detail-table {
  width: 100%;
  max-width: none;
  border-radius: 5px;
  box-shadow: none;
}

.detail-table th {
  width: 190px;
}

.json-card {
  margin: 20px 24px 24px;
}

.loading {
  padding: 40px 24px;
  text-align: center;
}

.detail-actions {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  padding: 0 24px 24px;
}

.restore-button {
  background: #2563eb;
  color: #fff;
}

.restore-button:disabled {
  cursor: not-allowed;
  opacity: 0.6;
}
</style>
