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
import { onMounted } from 'vue'
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

onMounted(async () => {
  const cameraDataNo = route.params.cameraDataNo
  await dStore.loadDetail(cameraDataNo)
})
</script>