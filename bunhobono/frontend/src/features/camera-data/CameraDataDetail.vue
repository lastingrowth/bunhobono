<template>
  <section v-if="dStore.detail" class="info-detail-card">
    <div class="info-detail-header">
      <div>
        <span class="info-detail-category">CAMERA DATA</span>
        <h2>카메라 데이터 상세</h2>
      </div>
    </div>

    <div class="info-detail-highlight">
      <span>인식 차량번호</span>
      <strong>{{ dStore.detail.carNo || '-' }}</strong>
    </div>

    <dl class="info-detail-list">
      <div>
        <dt>데이터 번호</dt>
        <dd>{{ dStore.detail.cameraDataNo }}</dd>
      </div>

      <div>
        <dt>촬영 시간</dt>
        <dd>{{ formatDate(dStore.detail.captureTime) }}</dd>
      </div>

      <div>
        <dt>이미지 경로</dt>
        <dd>{{ dStore.detail.imagePath || '-' }}</dd>
      </div>

      <div>
        <dt>인식 신뢰도</dt>
        <dd class="confidence-value">
          {{ dStore.detail.confidenceScore ?? '-' }}
          <span v-if="dStore.detail.confidenceScore !== null">%</span>
        </dd>
      </div>

      <div>
        <dt>차량 구분</dt>
        <dd>{{ dStore.detail.vehicleCarNo ? '등록 차량' : '미등록 차량' }}</dd>
      </div>

      <template v-if="dStore.detail.vehicleCarNo">
        <div>
          <dt>등록 기간</dt>
          <dd>{{ registrationPeriod }}</dd>
        </div>

        <div>
          <dt>만료일</dt>
          <dd>{{ formatDate(dStore.detail.endDate) }}</dd>
        </div>

        <div>
          <dt>남은 시간</dt>
          <dd>{{ remainingTime }}</dd>
        </div>
      </template>
    </dl>

    <div class="info-detail-actions">
      <button @click="router.back()">뒤로가기</button>
    </div>
  </section>

  <div v-else>
    <p>데이터를 불러오는 중...</p>
  </div>
</template>

<script setup>
import { computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useCameraDataStore } from './cameraDataStore'

const route = useRoute()
const router = useRouter()
const dStore = useCameraDataStore()

const formatDate = value => {
  if (!value) {
    return '-'
  }

  const date = new Date(value)

  if (Number.isNaN(date.getTime())) {
    return value
  }

  return date.toLocaleString('ko-KR')
}

const registrationPeriod = computed(() => {
  const { startDate, endDate } = dStore.detail || {};
  if (!startDate && !endDate) return '-';
  if (!endDate) return '기간 제한 없음';
  return `${formatDate(startDate)} ~ ${formatDate(endDate)}`;
});

const remainingTime = computed(() => {
  const endDate = dStore.detail?.endDate;
  if (!endDate) return '기간 제한 없음';

  const minutes = Math.floor((new Date(endDate) - new Date()) / 60000);
  if (minutes <= 0) return '만료됨';

  const days = Math.floor(minutes / 1440);
  const hours = Math.floor((minutes % 1440) / 60);
  const remainMinutes = minutes % 60;
  return days > 0 ? `${days}일 ${hours}시간` : `${hours}시간 ${remainMinutes}분`;
});

onMounted(async () => {
  const cameraDataNo = route.params.cameraDataNo
  await dStore.loadDetail(cameraDataNo)
})




</script>
