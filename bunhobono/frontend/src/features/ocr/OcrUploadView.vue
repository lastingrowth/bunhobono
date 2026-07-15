<template>
    <main class="ocr-upload-page">
        <section class="ocr-upload-card">
            
            <!-- 화면 제목 -->
            <header class="upload-header">
                <span class="upload-category">SMART PARKING OCR</span>
                <h1>차량 번호 인식</h1>
                <p>차량 사진을 선택하면 번호판을 분석하고 주차관리 시스템으로 전송합니다.</p>
            </header>

            <div class="upload-content">
                <!-- 사진 선택 및 미리보기 -->
                <section class="preview-section">
                    <input  ref="fileInput"
                            class="file-input"
                            type="file"
                            accept="image/*"
                            @change="handleFileChange" />
                    
                    <div class="upload-drop-zone"
                         :class="{ dragging : isDragging}"
                         role="button"
                         tabindex="0"
                         @click="openFilePicker"
                         @keydown.enter="openFilePicker"
                         @dragover.prevent="isDragging = true"
                         @dragleave.prevent="isDragging = false"
                         @drop.prevent="handleDrop">

                        <img v-if="previewUrl"
                            :src="previewUrl"
                            alt="차량 사진"
                            class="preview-image"/>

                        <div v-else class="upload-placeholder">
                            <span class="upload-icon">📷</span>
                            <strong>차량 사진을 선택하세요</strong>
                            <span>클릭하거나 사진을 이 영역에 끌어놓을 수 있습니다.</span>
                        </div>
                    </div>

                    <p v-if="selectedFile" class="selected-file-name">
                        선택한 파일 : {{ selectedFile.name }}
                    </p>
                </section>

                <!-- 카메라 선택 및 분석 -->
                <section class="control-section">
                    <div class="control-heading">
                        <span>OCR 분석</span>
                        <h2>촬영 정보</h2>
                    </div>

                    <!-- 현재 선택된 카메라 번호 -->
                    <div class="camera-device"
                        :class="{missing : !cameraNo}">
                        
                        <span>현재 카메라</span>

                        <strong v-if="cameraNo">
                            CAMERA #{{ cameraNo }}
                        </strong>

                        <strong v-else>
                            카메라 번호 없음
                        </strong>

                        <small v-if="cameraNo">
                            이 카메라에 연결된 게이트를 기준으로 입·출차를 처리합니다
                        </small>

                        <small v-else>
                            아래에서 사용할 카메라를 선택해주세요
                        </small>
                    </div>

                    <!-- 1번부터 8번까지 시연 카메라 선택 -->
                    <div class="camera-selector">
                      <span class="camera-selector-label">
                        카메라 선택
                      </span>

                      <div class="camera-button-grid">
                        <button
                          v-for="number in cameraNumbers"
                          :key="number"
                          type="button"
                          class="camera-select-button"
                          :class="{ active : cameraNo === number }"
                          :aria-pressed="cameraNo === number"
                          @click="selectCamera(number)">
                        CAM {{ number }}
                        </button>
                      </div>
                    </div>

                    <button 
                      type="button"
                      class="analyze-button"
                      :disabled="loading || !selectedFile || !cameraNo"
                      @click="analyzeImage">

                      <span v-if="loading">OCR 분석 중...</span>
                      <span v-else>OCR 분석 시작</span>
                    </button>

                    <p v-if="errorMessage" class="error-message">
                        {{ errorMessage }}
                    </p>

                    <!-- OCR 처리 결과 -->
                    <div v-if="result"
                        class="result-card"
                        :class="{ failed : !result.success}">
                         
                        <span class="result-label">분석 결과</span>

                        <template v-if="result.success">
                            <strong class="result-car-number">
                                {{ result.carNo || '미인식' }}
                            </strong>

                            <div class="result-information">
                                <span>인식 신뢰도</span>
                                <strong>{{ confidenceText }}</strong>
                            </div>

                            <p>대시보드로 차량 이미지와 인식 결과를 전송했습니다.</p>
                        </template>

                        <template v-else>
                            <strong class="result-failed-title">번호판 검출 실패</strong>
                        
                            <p>
                                번호판이 선명하게 보이는 사진으로 다시 시도해주세요.
                            </p>
                        </template>

                    </div>
                </section>
            </div>
        </section>
    </main>
</template>


<script setup>
import { storeToRefs } from 'pinia';
import { useOcrStore } from './ocrStore';
import { computed, onUnmounted, ref } from 'vue';
import { useRoute, useRouter } from 'vue-router';

const route = useRoute()
const router = useRouter()
const ocrStore = useOcrStore()

const cameraNumbers = [
  1, 2, 3, 4,
  5, 6, 7, 8
]

const {
    loading,
    result,
    errorMessage
} = storeToRefs(ocrStore)

const fileInput = ref(null)
const selectedFile = ref(null)
const previewUrl = ref('')
const isDragging = ref(false)

// 주소의 cameraNo 값을 현재 시연 카메라 번호로 사용
const cameraNo = computed(() => {
    const value = Number(route.query.cameraNo)

    if (!Number.isInteger(value) || value <= 0) {
        return null
    }

    return value
})

// 선택한 카메라 번호로 URL을 변경
const selectCamera = (cameraNumber) => {
    router.replace({
        path: '/ocr-upload',
        query: {
            cameraNo: cameraNumber
        }
    })
}
 
// FastAPI의 OCR 신뢰도를 백분율로 표시
const confidenceText = computed(() => {

    const score = Number(result.value?.ocr_score)
    
    if (Number.isNaN(score)) {
        return '-'
    }
    
    return `${(score * 100).toFixed(1)}%`
})

// 파일 선택창 열기
const openFilePicker = () => {
    fileInput.value?.click()
}

// 기존 미리보기 이미지 주소 제거
const clearPreview = () => {
    if (previewUrl.value) {
        URL.revokeObjectURL(previewUrl.value)
    }

    previewUrl.value = ''
}

// 선택한 이미지 저장 및 미리보기 생성
const selectImage = (file) => {
    if (!file) {
        return
    }

    if (!file.type.startsWith('image/')) {
        alert('이미지 파일만 선택할 수 있습니다.')
        return
    }

    clearPreview()

    selectedFile.value = file
    previewUrl.value = URL.createObjectURL(file)
}

// 파일 선택창으로 사진 선택
const handleFileChange = (event) => {
    selectImage(event.target.files[0])
}

// 사진을 끌어놓아 선택
const handleDrop = (event) => {
    isDragging.value = false
    selectImage(event.dataTransfer.files[0])
}

// 선택한 사진을 FastAPI로 전달
const analyzeImage = async () => {
    
    if (!selectedFile.value) {
        alert('차량 사진을 선택해주세요')
        return
    }

    if (!cameraNo.value) {
        alert('촬영 카메라를 선택해주세요')
        return
    }

    await ocrStore.uploadImage( selectedFile.value, cameraNo.value)
}

// 화면을 나갈 때 임시 미리보기 주소 제거
onUnmounted(() => {
    clearPreview()
})

</script>

<style scoped>
.ocr-upload-page {
  min-height: 100vh;
  padding: 40px;
  display: flex;
  align-items: center;
  justify-content: center;
  box-sizing: border-box;
  color: #18233d;
  background:
    radial-gradient(circle at top left, #dff4ff 0, transparent 34%),
    linear-gradient(135deg, #f5f8fc 0%, #edf3f9 100%);
}

.ocr-upload-card {
  width: min(1080px, 100%);
  padding: 38px;
  box-sizing: border-box;
  background: rgba(255, 255, 255, 0.96);
  border: 1px solid #dbe3ec;
  border-radius: 28px;
  box-shadow: 0 24px 60px rgba(37, 57, 88, 0.13);
}

.upload-header {
  margin-bottom: 30px;
}

.upload-category {
  color: #1598df;
  font-size: 13px;
  font-weight: 800;
  letter-spacing: 0.16em;
}

.upload-header h1 {
  margin: 8px 0 6px;
  font-size: 34px;
}

.upload-header p {
  margin: 0;
  color: #7d899d;
  line-height: 1.6;
}

.upload-content {
  display: grid;
  grid-template-columns: minmax(0, 1.4fr) minmax(360px, 0.85fr);
  align-items : stretch;
  gap: 34px;
}

.preview-section {
  min-width: 0;
}

.file-input {
  display: none;
}

.upload-drop-zone {
  height: 470px;
  overflow: hidden;
  display: flex;
  align-items: center;
  justify-content: center;
  box-sizing: border-box;
  cursor: pointer;
  background: #f5f8fb;
  border: 2px dashed #bdcbd9;
  border-radius: 22px;
  transition: 0.2s ease;
}

.upload-drop-zone:hover,
.upload-drop-zone.dragging {
  background: #edf8ff;
  border-color: #1598df;
  transform: translateY(-2px);
}

.preview-image {
  width: 100%;
  height: 100%;
  object-fit: contain;
  background: #111827;
}

.upload-placeholder {
  padding: 30px;
  display: flex;
  flex-direction: column;
  align-items: center;
  color: #8491a5;
  text-align: center;
}

.upload-placeholder strong {
  margin: 18px 0 8px;
  color: #25324a;
  font-size: 20px;
}

.upload-icon {
  font-size: 56px;
}

.selected-file-name {
  margin: 12px 4px 0;
  overflow: hidden;
  color: #748096;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.control-section {
  padding: 28px;
  box-sizing: border-box;
  background: #f7f9fc;
  border: 1px solid #dbe3ec;
  border-radius: 22px;
}

.control-heading span {
  color: #1598df;
  font-size: 12px;
  font-weight: 800;
  letter-spacing: 0.14em;
}

.control-heading h2 {
  margin: 6px 0 26px;
  font-size: 24px;
}

.camera-device {
  padding: 18px;
  display: flex;
  flex-direction: column;
  background: #fff;
  border: 1px solid #c9deee;
  border-radius: 14px;
}

.camera-device > span {
  color: #8491a5;
  font-size: 13px;
  font-weight: 700;
}

.camera-device strong {
  margin: 8px 0;
  color: #168bd1;
  font-size: 24px;
}

.camera-device small {
  color: #748096;
  line-height: 1.5;
}

.camera-device.missing {
  border-color: #efbcbc;
}

.camera-device.missing strong {
  color: #dc2626;
}

/* 현재 카메라 번호를 발표 화면에서도 잘 보이게 강조 */
.camera-device strong {
  font-size: 34px;
  letter-spacing: 0.04em;
}

/* 카메라 선택 버튼 영역 */
.camera-selector {
  margin-top: 20px;
}

.camera-selector-label {
  display: block;
  margin-bottom: 10px;
  color: #475569;
  font-size: 14px;
  font-weight: 800;
}

.camera-button-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 8px 12px;
}

.camera-select-button {
  min-width: 0;
  min-height: 44px;
  padding: 8px 4px;
  color: #168bd1;
  background: #ffffff;
  border: 1px solid #38a9e8;
  border-radius: 11px;
  font-size: 14px;
  font-weight: 800;
  cursor: pointer;
  transition: 0.2s ease;
}

.camera-select-button:hover {
  background: #edf8ff;
  transform: translateY(-1px);
}

.camera-select-button.active {
  color: #ffffff;
  background: linear-gradient(135deg, #169ee5, #2376e5);
  border-color: #168bd1;
  box-shadow: 0 7px 16px rgba(31, 130, 224, 0.2);
}

.analyze-button {
  width: 100%;
  min-height: 52px;
  margin-top: 22px;
  color: #fff;
  font-size: 16px;
  font-weight: 800;
  cursor: pointer;
  background: linear-gradient(135deg, #169ee5, #2376e5);
  border: 0;
  border-radius: 14px;
  box-shadow: 0 12px 24px rgba(31, 130, 224, 0.22);
}

.analyze-button:hover:not(:disabled) {
  transform: translateY(-1px);
}

.analyze-button:disabled {
  cursor: not-allowed;
  opacity: 0.55;
}

.error-message {
  margin: 14px 0 0;
  color: #dc2626;
  font-size: 14px;
}

.result-card {
  margin-top: 22px;
  padding: 22px;
  background: #fff;
  border: 1px solid #bfe3d0;
  border-radius: 16px;
}

.result-card.failed {
  border-color: #f1c4c4;
}

.result-label {
  display: block;
  margin-bottom: 10px;
  color: #8491a5;
  font-size: 13px;
  font-weight: 700;
}

.result-car-number {
  display: block;
  color: #17223b;
  font-size: 30px;
  letter-spacing: 0.08em;
}

.result-information {
  margin-top: 16px;
  padding-top: 14px;
  display: flex;
  justify-content: space-between;
  border-top: 1px solid #e4e9ef;
}

.result-information strong {
  color: #1598df;
}

.result-card p {
  margin: 14px 0 0;
  color: #748096;
  font-size: 13px;
  line-height: 1.5;
}

.result-failed-title {
  color: #dc2626;
  font-size: 20px;
}

@media (max-width: 800px) {
  .ocr-upload-page {
    padding: 20px;
    align-items: flex-start;
  }

  .ocr-upload-card {
    padding: 24px;
  }

  .upload-content {
    grid-template-columns: 1fr;
  }

  .upload-drop-zone {
    height: 450px;
  }
}
</style>