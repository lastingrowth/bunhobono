<template>
  <section v-if="dStore.detail" class="info-detail-card">
    <div class="info-detail-header">
      <div>
        <span class="info-detail-category">CAMERA DATA</span>
        <h2>카메라 데이터 상세</h2>
      </div>
    </div>

    <div class="camera-detail-content">
      <div class="camera-image-preview">
        <img
          v-if="imageUrl"
          :src="imageUrl"
          class="capture-image"
          alt="차량 촬영 이미지"
        />

        <span v-else-if="imageLoading">
          이미지를 불러오는 중...
        </span>

        <span v-else class="image-error">
          이미지를 불러올 수 없습니다.
        </span>
      </div>

      <div class="camera-detail-information">
        <div class="info-detail-highlight">
          <span>인식 차량번호</span>
          <div class="car-number-edit-row">
            <strong v-if="!isEditingCarNo">{{ dStore.detail.carNo || '-' }}</strong>

            <form v-else class="car-number-edit-form" @submit.prevent="saveCarNo">
              <input
                v-model="carNoDraft"
                type="text"
                maxlength="12"
                placeholder="예: 경기37바1083"
                aria-label="수정할 차량번호"
              />
              <div class="car-number-edit-actions">
                <button type="submit" :disabled="carNoSaving">
                  {{ carNoSaving ? '저장 중' : '저장' }}
                </button>
                <button type="button" class="cancel" :disabled="carNoSaving" @click="cancelCarNoEdit">
                  취소
                </button>
              </div>
            </form>

            <button v-if="!isEditingCarNo" type="button" class="car-number-edit-button" @click="startCarNoEdit">
              번호 수정
            </button>
          </div>
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
          <dd class="registration-period">
            <span>{{ formatDate(dStore.detail.startDate) }} ~</span>
            <span>{{ formatDate(dStore.detail.endDate) }}</span>
          </dd>
        </div>

        <div>
          <dt>남은 기간</dt>
          <dd>{{ remainingTime }}</dd>
        </div>
      </template>

      <div>
        <dt>비고</dt>
        <dd>{{ dStore.detail.remark || '-' }}</dd>
      </div>
        </dl>
      </div>
    </div>

    <div class="info-detail-actions">
      <button @click="router.back()">뒤로가기</button>
    </div>
  </section>

  <div v-else>
    <p>데이터를 불러오는 중...</p>
  </div>
</template>

<script setup>
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useCameraDataStore } from './cameraDataStore'
import { editCameraDataCarNo, getCameraDataImage } from './cameraDataApi'

const route = useRoute()
const router = useRouter()
const dStore = useCameraDataStore()
const imageUrl = ref(null)
const imageLoading = ref(false)
const isEditingCarNo = ref(false)
const carNoDraft = ref('')
const carNoSaving = ref(false)

const startCarNoEdit = () => {
  carNoDraft.value = dStore.detail?.carNo ?? ''
  isEditingCarNo.value = true
}

const cancelCarNoEdit = () => {
  carNoDraft.value = ''
  isEditingCarNo.value = false
}

const saveCarNo = async () => {
  const carNo = carNoDraft.value.trim().replace(/\s/g, '')
  const carNoPattern = /^([가-힣]{2})?\d{2,3}[가-힣]\d{4}$/

  if (!carNoPattern.test(carNo)) {
    alert('차량번호 형식을 확인하세요. 예: 123가4567, 경기37바1083')
    return
  }

  carNoSaving.value = true

  try {
    const cameraDataNo = route.params.cameraDataNo
    await editCameraDataCarNo(cameraDataNo, carNo, true)
    await dStore.loadDetail(cameraDataNo)
    cancelCarNoEdit()
  } catch (error) {
    console.error('차량번호 수정 실패:', error)
    alert(error.response?.data?.message || '차량번호 수정에 실패했습니다.')
  } finally {
    carNoSaving.value = false
  }
}

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

  imageLoading.value = true

  try {
    const response = await getCameraDataImage(cameraDataNo)
    imageUrl.value = URL.createObjectURL(response.data)
  } catch (error) {
    console.error('카메라 이미지 조회 실패:', error)
  } finally {
    imageLoading.value = false
  }
})

onBeforeUnmount(() => {
  if (imageUrl.value) {
    URL.revokeObjectURL(imageUrl.value)
  }
})


</script>

<style scoped>
.camera-image-row {
  display: block;
}

.info-detail-card {
  width: min(1180px, 100%);
  max-width: 1180px;
  margin: 0 auto;
  overflow: hidden;
  border: 0;
  border-radius: 10px;
  box-shadow: 0 20px 48px rgba(35, 52, 66, 0.18);
}

.info-detail-header {
  padding: 16px 20px;
}

.info-detail-highlight {
  padding: 20px 22px;
}

.camera-detail-content {
  height: 420px;
  display: grid;
  grid-template-columns: minmax(0, 1.15fr) minmax(380px, 1fr);
  align-items: stretch;
  border-top: 1px solid var(--border-color);
}

.camera-detail-information {
  min-width: 0;
  overflow-y: auto;
  border-left: 1px solid var(--border-color);
  background: var(--admin-surface, #ffffff);
}

.camera-detail-information .info-detail-highlight {
  border-bottom: 1px solid var(--admin-line, var(--border-color));
}

.camera-detail-information .info-detail-list > div {
  min-height: 36px;
  grid-template-columns: 130px minmax(0, 1fr);
  padding: 3px 22px;
  box-sizing: border-box;
  border-color: var(--admin-line, var(--border-color));
  background: var(--admin-surface, #ffffff);
}

.camera-detail-information .info-detail-list > div:nth-child(even) {
  background: var(--admin-surface-muted, #f3f4f6);
}

.camera-detail-information .info-detail-list dt {
  font-size: 11px;
}

.camera-detail-information .info-detail-list dd {
  font-size: 12px;
  line-height: 1.35;
}

.registration-period span {
  display: block;
}

.car-number-edit-row {
  display: flex;
  align-items: center;
  gap: 10px;
}

.car-number-edit-button,
.car-number-edit-form button {
  height: 32px;
  min-width: 64px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  padding: 0 12px;
  border: 0;
  border-radius: 7px;
  cursor: pointer;
  color: #ffffff;
  font-weight: 800;
  line-height: 1;
  white-space: nowrap;
  background: #34495e;
}

.car-number-edit-button {
  flex: 0 0 auto;
  min-width: 76px;
}

.car-number-edit-form {
  display: flex;
  flex-wrap: nowrap;
  align-items: center;
  gap: 7px;
  margin: 0;
  padding: 0 !important;
  border: 0 !important;
  background: transparent !important;
  box-shadow: none !important;
}

.car-number-edit-form input {
  width: min(180px, 100%);
  height: 36px;
  padding: 0 10px;
  border: 1px solid #9aa7b3;
  border-radius: 7px;
  font-size: 18px;
  font-weight: 800;
}

.car-number-edit-form button.cancel {
  color: #39434c;
  background: #dfe4e8;
}

.car-number-edit-form button:disabled {
  cursor: wait;
  opacity: 0.65;
}

/* 관제 대시보드의 차량번호 수정 행과 같은 규격 */
.info-detail-card .car-number-edit-row {
  min-width: 0;
  min-height: 36px;
  flex-wrap: nowrap;
  overflow: hidden;
}

.admin-layout .content .info-detail-card form.car-number-edit-form {
  width: 100%;
  min-width: 0;
  max-width: none;
  height: 36px;
  max-height: 36px;
  display: flex;
  flex: 1 1 auto;
  align-items: stretch;
  gap: 4px;
  padding: 0 !important;
  border: 0 !important;
  background: transparent !important;
  box-shadow: none !important;
  overflow: hidden;
}

.admin-layout .content .info-detail-card form.car-number-edit-form input {
  width: calc(100% - 103px);
  min-width: 0;
  max-width: none;
  min-height: 36px !important;
  height: 36px !important;
  box-sizing: border-box;
  flex: 1 1 calc(100% - 103px);
  align-self: stretch;
  margin: 0 !important;
  padding: 0 8px;
  border: 1px solid #69727a;
  color: #f1d77b;
  font-size: 28px;
  font-weight: 800;
  line-height: 34px;
  background: #252b30;
  box-shadow: none;
}

.info-detail-card .car-number-edit-actions {
  height: 36px;
  display: flex;
  flex: 0 0 auto;
  align-items: stretch;
  align-self: stretch;
  margin: 0 !important;
  gap: 3px;
}

.admin-layout .content .info-detail-card .car-number-edit-actions button {
  width: auto;
  min-width: 48px;
  min-height: 36px !important;
  height: 36px !important;
  box-sizing: border-box;
  align-self: stretch;
  margin: 0 !important;
  padding: 0 8px;
  border: 1px solid #687178;
  color: #e6e9eb;
  font-size: 12px;
  line-height: 1;
  background: #454c52;
}

.admin-layout .content .info-detail-card .car-number-edit-actions button:hover {
  border-color: #8b949b;
  color: #ffffff;
  background: #565e65;
}

.info-detail-list {
  margin: 0;
  padding: 0;
}

.info-detail-actions {
  padding: 18px 24px;
  border-top: 1px solid var(--border-color);
  background: #f8fafb;
}

.camera-image-preview {
  position: relative;
  min-width: 0;
  height: 100%;
  min-height: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
  color: #8a93a5;
  background: #20252a;
}

.capture-image {
  display: block;
  width: 100%;
  height: 100%;
  min-height: 100%;
  object-fit: cover;
  object-position: center bottom;
  transform: scale(1.24);
  transform-origin: center bottom;
}

.image-error {
  color: #8a93a5;
}

@media (max-width: 920px) {
  .camera-detail-content {
    height: auto;
    grid-template-columns: minmax(0, 1fr);
  }

  .camera-detail-information {
    overflow-y: visible;
    border-top: 1px solid var(--border-color);
    border-left: 0;
  }

  .camera-image-preview,
  .capture-image {
    min-height: clamp(300px, 62vw, 480px);
  }
}
</style>
