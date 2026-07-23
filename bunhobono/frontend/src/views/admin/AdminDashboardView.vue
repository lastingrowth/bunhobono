<template>
    <section class="admin-dashboard"> 
        <ManagementFeedbackToast
            :message="vehicleFeedbackMessage"
            :type="vehicleFeedbackType" />
        <!-- 데이터 조회 상태 -->
        <p v-if="loading" class="dashboard-message">
            대시보드 정보를 불러오는 중입니다
        </p>

        <p v-else-if="errorMessage" class="dashboard-error">
            {{ errorMessage }}
        </p>

        <!-- 메인 영역 : 왼쪽은 영상, 오른쪽은 입출차 로그와 상세 정보 -->
        <div
            class="admin-control-layout"
            :style="monitoringHeight ? { '--monitoring-height': `${monitoringHeight}px` } : {}">
            <section class="admin-control-left">
            <!-- 주차장 모니터링 영역 -->
            <article ref="monitoringCardRef" class="dashboard-card monitoring-card">

                <!-- A/B/C/D 주차장 카드 v-for로 반복해서 표시 -->
                <div class="parking-monitor-grid">
                    <section
                        v-for="panel in parkingMonitorPanels"
                        :key="panel.parkingNo"
                        class="parking-monitor-card"
                        @click="openParkingDialog(panel)">

                        <div class="parking-video-box">
                            <img
                                v-if="hasStreamSession(panel.cameraNo) && !isCameraFinished(panel.cameraNo)"
                                class="parking-stream"
                                :src="getStreamUrl(panel.cameraNo)"
                                :alt="`${panel.parkingName} ${panel.modeText} CCTV`" />

                            <div v-else class="parking-video-placeholder">
                                <strong v-if="isCameraFinished(panel.cameraNo)">영상 재생이 끝났습니다</strong>
                                <strong v-else>{{ panel.modeText }}</strong>

                                <span
                                    v-if="panel.gate"
                                    class="video-gate-status"
                                    :class="{ open: panel.gate.gateStatus === 1, closed: panel.gate.gateStatus !== 1 }">
                                    {{ panel.gate.gateStatus === 1 ? '열림' : '닫힘' }}
                                </span>

                                <span v-else class="video-gate-status closed">
                                    게이트 없음
                                </span>

                                <small>
                                    CCTV {{ panel.cameraNo }} · {{ isCameraFinished(panel.cameraNo) ? 'END OF VIDEO' : '재생 대기' }}
                                </small>
                            </div>

                            <div class="cctv-overlay">
                                <div class="cctv-overlay-top">
                                    <span class="cctv-live" :class="{ paused: !isCameraPlaying(panel.cameraNo) }">
                                        <i></i>{{ isCameraPlaying(panel.cameraNo) ? 'REC' : (isCameraFinished(panel.cameraNo) ? 'ENDED' : 'STANDBY') }}
                                    </span>
                                    <div class="cctv-camera-mode-group">
                                        <button
                                            type="button"
                                            class="cctv-mode-control"
                                            :class="panel.modeClass"
                                            @click.stop="changeCameraMode(panel)">
                                            <span>{{ panel.mode === 'IN' ? 'IN' : 'OUT' }}</span>
                                        </button>
                                    </div>
                                </div>
                                <div class="cctv-overlay-bottom">
                                    <span>{{ panel.parkingName }} · {{ panel.modeText }}</span>
                                    <time>{{ cctvDateTime }}</time>
                                </div>
                                <i class="cctv-corner top-left"></i>
                                <i class="cctv-corner top-right"></i>
                                <i class="cctv-corner bottom-left"></i>
                                <i class="cctv-corner bottom-right"></i>
                            </div>

                            <div
                                v-if="isApprovalWaiting(panel.cameraNo)"
                                class="approval-wait-overlay"
                                :class="{ 'low-confidence': isLowConfidenceWaiting(panel) }"
                                @click.stop>
                                <div class="approval-wait-panel">
                                    <span class="approval-wait-title"><i></i>{{ approvalWaitTitle(panel) }}</span>
                                    <strong>{{ getCameraStatus(panel.cameraNo)?.lastOcrCarNo || '차량번호 확인 중' }}</strong>
                                    <p>{{ approvalWaitMessage(panel) }}</p>
                                    <button
                                        type="button"
                                        :disabled="!panel.gate"
                                        @click.stop="openGateAndResume(panel)">
                                        {{ approvalWaitButtonText(panel) }}
                                    </button>
                                </div>
                            </div>
                        </div>

                        <div class="parking-monitor-footer">
                            <span
                                v-if="panel.gate"
                                class="footer-gate-status"
                                :class="{ open: panel.gate.gateStatus === 1, closed: panel.gate.gateStatus !== 1 }">
                                <i></i>GATE {{ panel.gate.gateStatus === 1 ? 'OPEN' : 'CLOSED' }}
                            </span>

                            <span v-else class="footer-gate-status closed">
                                <i></i>NO GATE
                            </span>

                            <button
                                type="button"
                                class="camera-play-button"
                                @click.stop="toggleStream(panel)">
                                {{ isCameraPlaying(panel.cameraNo) ? '일시정지' : (isCameraFinished(panel.cameraNo) ? '다시 재생' : '재생') }}
                            </button>

                            <!-- 선택된 화면에 연결된 게이트를 연다 -->
                            <button
                                type="button"
                                class="camera-gate-button"
                                :disabled="!panel.gate"
                                @click.stop="openGateAndResume(panel)">
                                게이트 열기
                            </button>
                        </div>
                    </section>
                </div>
            </article>

            </section>

            <!-- 오른쪽 입출차 로그 + 선택 상세 영역 -->
            <section class="admin-control-right">
                <section class="dashboard-card carlog-dashboard-block">
                    <div class="section-heading">
                        <h3
                            class="dashboard-follow-title"
                            :class="{ active: detailFollowMode === 'CAR_LOG' }"
                            role="button"
                            tabindex="0"
                            @click="followLatestCarlog"
                            @keydown.enter.prevent="followLatestCarlog"
                            @keydown.space.prevent="followLatestCarlog">
                            입출차 로그
                        </h3>
                        <button
                            type="button"
                            class="dashboard-view-all-button"
                            @click="router.push('/admin/carlogs')">
                            전체보기
                        </button>
                    </div>

                    <div class="carlog-table-wrap">
                        <table class="dashboard-log-table">
                            <thead>
                                <tr>
                                    <th>차량번호</th>
                                    <th>구분</th>
                                    <th>상태</th>
                                    <th>주차장</th>
                                    <th>최근 활동</th>
                                </tr>
                            </thead>
                            <tbody>
                                <tr
                                    v-for="log in recentCarlogs"
                                    :key="log.carLogNo"
                                    :class="{ selected: selectedDetailType === 'CAR_LOG' && selectedCarlog?.carLogNo === log.carLogNo }"
                                    @click="showCarlogDetail(log)">
                                    <td>{{ log.carNo || '미인식' }}</td>
                                    <td>{{ log.carKindText }}</td>
                                    <td>
                                        <span class="state-badge" :class="parkingStateClass(log)">
                                            {{ log.parkingStateText }}
                                        </span>
                                    </td>
                                    <td>{{ log.parkingName || '-' }}</td>
                                    <td>{{ formatCameraDataTime(log.outTime || log.inTime) }}</td>
                                </tr>
                                <tr v-if="recentCarlogs.length === 0">
                                    <td colspan="5">입출차 로그가 없습니다</td>
                                </tr>
                            </tbody>
                        </table>
                    </div>
                </section>

                <section class="dashboard-card camera-dashboard-block">
                    <div class="section-heading">
                        <h3
                            class="dashboard-follow-title"
                            :class="{ active: detailFollowMode === 'CAMERA_DATA' }"
                            role="button"
                            tabindex="0"
                            @click="followLatestCameraData"
                            @keydown.enter.prevent="followLatestCameraData"
                            @keydown.space.prevent="followLatestCameraData">
                            카메라 데이터
                        </h3>
                        <button
                            type="button"
                            class="dashboard-view-all-button"
                            @click="router.push('/admin/camera-data')">
                            전체보기
                        </button>
                    </div>

                    <div class="carlog-table-wrap">
                        <table class="dashboard-log-table">
                            <thead>
                                <tr>
                                    <th>차량번호</th>
                                    <th>등록</th>
                                    <th>카메라</th>
                                    <th>촬영 시각</th>
                                </tr>
                            </thead>

                            <tbody>
                                <tr
                                    v-for="cameraData in recentCameraData"
                                    :key="cameraData.cameraDataNo"
                                    :class="{
                                        selected: selectedDetailType === 'CAMERA_DATA' && selectedCameraData?.cameraDataNo === cameraData.cameraDataNo,
                                        'recognition-review-row': needsRecognitionReview(cameraData),
                                    }"
                                    @click="showCameraDataDetail(cameraData)">
                                    <td>
                                        {{ cameraData.carNo || '미인식' }}
                                        <span v-if="needsRecognitionReview(cameraData)" class="danger-text"> · 확인 필요</span>
                                    </td>
                                    <td>{{ cameraData.vehicleCarNo ? '등록' : '미등록' }}</td>
                                    <td>{{ cameraLabel(cameraData.cameraNo) }}</td>
                                    <td>{{ formatCameraDataTime(cameraData.captureTime) }}</td>
                                </tr>

                                <tr v-if="recentCameraData.length === 0">
                                    <td colspan="4">카메라 데이터가 없습니다</td>
                                </tr>
                            </tbody>
                        </table>
                    </div>
                </section>

                <article class="dashboard-card selected-log-card">
                    <div class="combined-detail-section">
                    <div v-if="selectedDetailType === 'CAMERA_DATA' && selectedCameraData" class="selected-log-content">
                        <div class="selected-log-images">
                            <div class="capture-image-box">
                                <img
                                    v-if="selectedCameraData.cameraDataNo && !hasCaptureImageError(selectedCameraData.cameraDataNo)"
                                    class="selected-capture-image"
                                    :src="getCameraImageUrl(selectedCameraData.cameraDataNo)"
                                    title="클릭해서 크게 보기"
                                    @click="openImagePreview(selectedCameraData.cameraDataNo, selectedCameraData.carNo)"
                                    :alt="`${selectedCameraData.carNo || '미인식 차량'} OCR 캡처 이미지`"
                                    @error="markCaptureImageError(selectedCameraData.cameraDataNo)" />

                                <div v-else class="capture-image-empty">
                                    저장된 OCR 이미지가 없습니다
                                </div>

                                <div class="capture-crop-overlay">
                                    <img
                                        v-if="selectedCameraData.cameraDataNo && !hasCropImageError(selectedCameraData.cameraDataNo)"
                                        class="capture-crop-image"
                                        :src="getCameraCropImageUrl(selectedCameraData.cameraDataNo)"
                                        title="클릭해서 번호판 크게 보기"
                                        @click.stop="openImagePreview(selectedCameraData.cameraDataNo, selectedCameraData.carNo)"
                                        :alt="`${selectedCameraData.carNo || '미인식 차량'} 번호판 크롭 이미지`"
                                        @error="markCropImageError(selectedCameraData.cameraDataNo)" />
                                    <span v-else class="capture-image-label">OCR CAPTURE</span>
                                </div>
                            </div>
                        </div>

                        <dl
                            class="selected-log-info"
                            :class="{ 'recognition-review-info': needsRecognitionReview(selectedCameraData) }">
                            <div>
                                <dt>차량번호</dt>
                                <dd
                                    class="camera-car-number-field"
                                    :class="{ 'danger-text': needsRecognitionReview(selectedCameraData) }">
                                    <template v-if="!isEditingCameraCarNo">
                                        <span>{{ selectedCameraData.carNo || '미인식' }}</span>
                                        <button
                                            type="button"
                                            class="camera-car-number-edit-button"
                                            @click="startCameraCarNoEdit">
                                            수정
                                        </button>
                                    </template>

                                    <form v-else class="camera-car-number-edit-form" @submit.prevent="saveCameraCarNo">
                                        <input
                                            v-model="cameraCarNoDraft"
                                            type="text"
                                            maxlength="12"
                                            placeholder="예: 경기37바1083"
                                            aria-label="수정할 차량번호" />
                                        <div class="camera-car-number-edit-actions">
                                            <button type="submit" :disabled="cameraCarNoSaving">
                                                {{ cameraCarNoSaving ? '저장 중' : '저장' }}
                                            </button>
                                            <button type="button" class="cancel" :disabled="cameraCarNoSaving" @click="cancelCameraCarNoEdit">취소</button>
                                        </div>
                                        <small>등록 차량과 일치하면 기존 OCR 번호도 별칭으로 저장됩니다.</small>
                                    </form>
                                </dd>
                            </div>
                            <div v-if="selectedCameraData.ocrCarNo && selectedCameraData.ocrCarNo !== selectedCameraData.carNo">
                                <dt>원본 OCR 번호</dt>
                                <dd>{{ selectedCameraData.ocrCarNo }}</dd>
                            </div>
                            <div>
                                <dt>등록 상태</dt>
                                <dd>{{ selectedCameraData.vehicleCarNo ? '등록 차량' : '미등록 차량' }}</dd>
                            </div>
                            <div>
                                <dt>카메라 번호</dt>
                                <dd>{{ cameraLabel(selectedCameraData.cameraNo) }}</dd>
                            </div>
                            <div>
                                <dt>촬영 시각</dt>
                                <dd>{{ formatCameraDataTime(selectedCameraData.captureTime) }}</dd>
                            </div>
                            <div>
                                <dt>인식 상태</dt>
                                <dd :class="{ 'danger-text': needsRecognitionReview(selectedCameraData) }">{{ recognitionStateText(selectedCameraData) }}</dd>
                            </div>
                            <div>
                                <dt>인식 신뢰도</dt>
                                <dd :class="{ 'danger-text': needsRecognitionReview(selectedCameraData) }">{{ formatConfidence(selectedCameraData.confidenceScore) }}</dd>
                            </div>
                            <div>
                                <dt>비고</dt>
                                <dd class="camera-note-field">
                                    <template v-if="!isEditingCameraNote">
                                        <span>{{ selectedCameraData.camNote || '-' }}</span>
                                        <button type="button" class="camera-note-edit-button" @click="startCameraNoteEdit">
                                            메모 수정
                                        </button>
                                    </template>
                                    <form v-else class="camera-note-edit-form" @submit.prevent="saveCameraNote">
                                        <textarea
                                            v-model="cameraNoteDraft"
                                            rows="3"
                                            placeholder="메모를 입력하세요"
                                            aria-label="카메라 데이터 메모"
                                        ></textarea>
                                        <div class="camera-note-edit-actions">
                                            <button type="submit" :disabled="cameraNoteSaving">
                                                {{ cameraNoteSaving ? '저장 중' : '저장' }}
                                            </button>
                                            <button type="button" class="cancel" :disabled="cameraNoteSaving" @click="cancelCameraNoteEdit">
                                                취소
                                            </button>
                                        </div>
                                    </form>
                                </dd>
                            </div>
                        </dl>
                    </div>

                    <div v-else-if="selectedCarlog" class="selected-log-content">
                        <!-- 나중에 FastAPI에서 넘겨준 원본 이미지와 크롭 이미지를 넣을 자리 -->
                        <div class="selected-log-images">
                            <div class="capture-image-box">
                                <img
                                    v-if="selectedCarlog.cameraDataNo && !hasCaptureImageError(selectedCarlog.cameraDataNo)"
                                    class="selected-capture-image"
                                    :src="getCameraImageUrl(selectedCarlog.cameraDataNo)"
                                    title="클릭해서 크게 보기"
                                    @click="openImagePreview(selectedCarlog.cameraDataNo, selectedCarlog.carNo)"
                                    :alt="`${selectedCarlog.carNo || '미인식 차량'} OCR 캡처 이미지`"
                                    @error="markCaptureImageError(selectedCarlog.cameraDataNo)" />

                                <div v-else class="capture-image-empty">
                                    저장된 OCR 이미지가 없습니다
                                </div>

                                <div class="capture-crop-overlay">
                                    <img
                                        v-if="selectedCarlog.cameraDataNo && !hasCropImageError(selectedCarlog.cameraDataNo)"
                                        class="capture-crop-image"
                                        :src="getCameraCropImageUrl(selectedCarlog.cameraDataNo)"
                                        title="클릭해서 번호판 크게 보기"
                                        @click.stop="openImagePreview(selectedCarlog.cameraDataNo, selectedCarlog.carNo)"
                                        :alt="`${selectedCarlog.carNo || '미인식 차량'} 번호판 크롭 이미지`"
                                        @error="markCropImageError(selectedCarlog.cameraDataNo)" />
                                    <span v-else class="capture-image-label">OCR CAPTURE</span>
                                </div>
                            </div>
                        </div>

                        <dl class="selected-log-info">
                            <div>
                                <dt>차량번호</dt>
                                <dd>{{ selectedCarlog.carNo || '미인식' }}</dd>
                            </div>

                            <div>
                                <dt>차량 구분</dt>
                                <dd>{{ selectedCarlog.carKindText }}</dd>
                            </div>

                            <div>
                                <dt>주차 상태</dt>
                                <dd>{{ selectedCarlog.parkingStateText }}</dd>
                            </div>

                            <div>
                                <dt>입차 시간</dt>
                                <dd>{{ selectedCarlog.inTimeText }}</dd>
                            </div>

                            <div>
                                <dt>출차 시간</dt>
                                <dd>{{ selectedCarlog.outTime ? selectedCarlog.outTimeText : '-' }}</dd>
                            </div>

                            <div>
                                <dt>입차 게이트</dt>
                                <dd>{{ selectedCarlog.inGateText }}</dd>
                            </div>

                            <div>
                                <dt>출차 게이트</dt>
                                <dd>{{ selectedCarlog.outGateText }}</dd>
                            </div>
                        </dl>
                    </div>

                    <div v-else class="selected-log-empty">
                        입출차 로그를 선택하세요
                    </div>
                    </div>
                </article>
            </section>
        </div>

        <!-- 하단 차량 등록 및 회원 승인 -->
        <div class="dashboard-top-strip dashboard-bottom-strip">
            <form
                class="quick-register-inline"
                @submit.prevent="submitQuickVehicle">
                <input
                    v-model="quickVehicle.carNo"
                    type="text"
                    placeholder="차량번호" />

                <select
                    v-model="quickVehicle.vehicleType"
                    @change="loadQuickRegisterMembers">
                    <option value="normal">등록차량</option>
                    <option value="visit">방문차량</option>
                </select>

                <select
                    v-model="quickVehicle.role"
                    @change="loadQuickRegisterMembers">
                    <option value="RESIDENT">입주민</option>
                    <option value="ADMIN">관리자</option>
                </select>

                <select v-model.number="quickVehicle.periodValue">
                    <option
                        v-for="option in quickPeriodOptions"
                        :key="option.value"
                        :value="option.value">
                        {{ option.text }}
                    </option>
                </select>

                <select v-model.number="quickVehicle.memberNo">
                    <option :value="null">회원 선택</option>
                    <option
                        v-for="member in quickRegisterMembers"
                        :key="member.memberNo"
                        :value="member.memberNo">
                        {{ memberLabel(member) }}
                    </option>
                </select>

                <button type="submit">등록</button>
            </form>

            <div class="dashboard-alert-chips">
                <button
                    v-for="alert in dashboardAlerts"
                    :key="alert.key"
                    type="button"
                    class="alert-chip"
                    :class="{ 'has-count': Number(alert.count) > 0 }"
                    @click="router.push(alert.path)">
                    <span>{{ alert.title }}</span>
                    <strong>{{ alert.count }}</strong>
                </button>
            </div>
        </div>

        <!-- 주차장 카드를 클릭했을 때 크게 보여주는 dialog 화면 -->
        <dialog
            ref="imagePreviewDialog"
            class="image-preview-dialog"
            @click="closeImagePreviewOnBackdrop"
            @close="clearImagePreview">
            <div class="image-preview-content">
                <button
                    type="button"
                    class="image-preview-close"
                    aria-label="확대 이미지 닫기"
                    @click="closeImagePreview">×</button>
                <div class="image-preview-composite">
                    <img
                        v-if="imagePreviewUrl"
                        class="image-preview-vehicle"
                        :src="imagePreviewUrl"
                        :alt="`${imagePreviewAlt} 전체 이미지`" />
                    <div class="image-preview-crop">
                        <img
                            v-if="imagePreviewCropUrl"
                            :src="imagePreviewCropUrl"
                            :alt="`${imagePreviewAlt} 번호판 이미지`" />
                    </div>
                </div>
                <p>{{ imagePreviewAlt }}</p>
            </div>
        </dialog>

        <dialog
            ref="parkingDialog"
            class="parking-dialog"
            @close="closeParkingPanel">

            <div v-if="selectedParkingPanel" class="parking-dialog-content">
                <div class="parking-dialog-header">
                    <div>
                        <span>{{ selectedParkingPanel.modeText }} 화면</span>
                    </div>

                    <button
                        type="button"
                        class="parking-dialog-close"
                        @click="closeParkingDialog">
                        ×
                    </button>
                </div>

                <div class="parking-dialog-video">
                    <img
                        v-if="isCameraPlaying(selectedParkingPanel.cameraNo)"
                        class="parking-stream"
                        :src="getStreamUrl(selectedParkingPanel.cameraNo)"
                        :alt="`${selectedParkingPanel.parkingName} ${selectedParkingPanel.modeText} CCTV`" />

                    <div v-else class="parking-video-placeholder">
                        <strong>
                            {{ isCameraFinished(selectedParkingPanel.cameraNo)
                                ? '영상 재생이 끝났습니다'
                                : selectedParkingPanel.modeText }}
                        </strong>
                        <small>
                            CCTV {{ selectedParkingPanel.cameraNo }} ·
                            {{ isCameraFinished(selectedParkingPanel.cameraNo) ? 'END OF VIDEO' : '재생 대기' }}
                        </small>

                        <span
                            v-if="selectedParkingPanel.gate"
                            class="video-gate-status"
                            :class="{ open: selectedParkingPanel.gate.gateStatus === 1, closed: selectedParkingPanel.gate.gateStatus !== 1 }">
                            {{ selectedParkingPanel.gate.gateStatus === 1 ? '열림' : '닫힘' }}
                        </span>

                        <span v-else class="video-gate-status closed">
                            게이트 없음
                        </span>
                    </div>

                    <div class="cctv-overlay dialog-overlay" aria-hidden="true">
                        <div class="cctv-overlay-top">
                            <span class="cctv-live" :class="{ paused: !isCameraPlaying(selectedParkingPanel.cameraNo) }">
                                <i></i>{{ isCameraPlaying(selectedParkingPanel.cameraNo) ? 'REC' : (isCameraFinished(selectedParkingPanel.cameraNo) ? 'ENDED' : 'STANDBY') }}
                            </span>
                            <span>CAM {{ String(selectedParkingPanel.cameraNo).padStart(2, '0') }}</span>
                        </div>
                        <div class="cctv-overlay-bottom">
                            <span>{{ selectedParkingPanel.parkingName }} · {{ selectedParkingPanel.modeText }}</span>
                            <time>{{ cctvDateTime }}</time>
                        </div>
                        <i class="cctv-corner top-left"></i>
                        <i class="cctv-corner top-right"></i>
                        <i class="cctv-corner bottom-left"></i>
                        <i class="cctv-corner bottom-right"></i>
                    </div>

                    <button
                        type="button"
                        class="cctv-mode-control dialog-mode-control"
                        :class="selectedParkingPanel.modeClass"
                        @click="changeCameraMode(selectedParkingPanel)">
                        <i></i>
                        <span>{{ selectedParkingPanel.mode === 'IN' ? 'IN' : 'OUT' }}</span>
                        <small>{{ selectedParkingPanel.modeText }} 전환</small>
                    </button>

                    <div
                        v-if="isApprovalWaiting(selectedParkingPanel.cameraNo)"
                        class="approval-wait-overlay dialog-approval-wait"
                        @click.stop>
                        <div class="approval-wait-panel">
                            <span class="approval-wait-title"><i></i>{{ approvalWaitTitle(selectedParkingPanel) }}</span>
                            <strong>{{ getCameraStatus(selectedParkingPanel.cameraNo)?.lastOcrCarNo || '차량번호 확인 중' }}</strong>
                            <p>{{ approvalWaitMessage(selectedParkingPanel) }}</p>
                            <button
                                type="button"
                                :disabled="!selectedParkingPanel.gate"
                                @click="openGateAndResume(selectedParkingPanel)">
                                {{ approvalWaitButtonText(selectedParkingPanel) }}
                            </button>
                        </div>
                    </div>
                </div>

                <div class="parking-dialog-actions">
                    <span
                        v-if="selectedParkingPanel.gate"
                        class="footer-gate-status"
                        :class="{ open: selectedParkingPanel.gate.gateStatus === 1, closed: selectedParkingPanel.gate.gateStatus !== 1 }">
                        <i></i>GATE {{ selectedParkingPanel.gate.gateStatus === 1 ? 'OPEN' : 'CLOSED' }}
                    </span>

                    <button
                        type="button"
                        class="camera-play-button"
                        @click="toggleStream(selectedParkingPanel)">
                        {{ isCameraPlaying(selectedParkingPanel.cameraNo)
                            ? '일시정지'
                            : (isCameraFinished(selectedParkingPanel.cameraNo) ? '다시 재생' : '재생') }}
                    </button>

                    <!-- 백엔드 open API 호출. 자동 닫힘은 백엔드 scheduleClose에서 처리 -->
                    <button
                        type="button"
                        class="camera-gate-button"
                        :disabled="!selectedParkingPanel.gate"
                        @click="openGateAndResume(selectedParkingPanel)">
                        게이트 열기
                    </button>
                </div>
            </div>
        </dialog>

    </section>
</template>

<script setup>
import { useAdminDashboardStore } from '@/stores/adminDashboard';
import { editCameraDataCarNo, editCameraDataNote, getCameraDataDetail } from '@/features/camera-data/cameraDataApi';
import { storeToRefs } from 'pinia';
import { nextTick, onBeforeUnmount, onMounted, ref } from 'vue';
import { useRouter } from 'vue-router';
import ManagementFeedbackToast from '@/shared/components/ManagementFeedbackToast.vue';


const router = useRouter()
const dashboardStore = useAdminDashboardStore()
const parkingDialog = ref(null)
const imagePreviewDialog = ref(null)
const imagePreviewUrl = ref('')
const imagePreviewCropUrl = ref('')
const imagePreviewAlt = ref('')
const FASTAPI_URL = 'http://127.0.0.1:8000'
const playingCameraNos = ref(new Set())
const finishedCameraNos = ref(new Set())
const selectedDetailType = ref('CAR_LOG')
const selectedCameraData = ref(null)
const streamSessions = ref({})
const cctvDateTime = ref('')
const captureImageErrors = ref(new Set())
const cropImageErrors = ref(new Set())
const lastOcrEventIds = new Map()
const pendingCameraDataNos = ref({})
const cameraStatuses = ref({})
const isEditingCameraCarNo = ref(false)
const cameraCarNoDraft = ref('')
const cameraCarNoSaving = ref(false)
const isEditingCameraNote = ref(false)
const cameraNoteDraft = ref('')
const cameraNoteSaving = ref(false)
const detailFollowMode = ref('CAR_LOG')
const monitoringCardRef = ref(null)
const monitoringHeight = ref(0)
let ocrStatusTimer = null
let refreshingCarlogs = false
let cctvClockTimer = null
let monitoringResizeObserver = null

// storeToRefs 를 사용하면 store 안의 ref/computed 반응성이 유지된다
const {
    loading,
    errorMessage,
    vehicleFeedbackMessage,
    vehicleFeedbackType,
    quickVehicle,
    quickPeriodOptions,
    quickRegisterMembers,
    dashboardAlerts,
    parkingMonitorPanels,
    selectedParkingPanel,
    recentCarlogs,
    recentCameraData,
    selectedCarlog,
} = storeToRefs(dashboardStore)

const {
    memberLabel,
    showVehicleFeedback,
    submitQuickVehicle,
    loadQuickRegisterMembers,
    toggleParkingCamera,
    selectParkingPanel,
    closeParkingPanel,
    selectCarlog,
    refreshCarlogs,
    parkingStateClass,
    openManualGate,
    refreshGateStatuses,
    loadDashboard,
} = dashboardStore

const openParkingDialog = (panel) => {
    selectParkingPanel(panel)
    parkingDialog.value?.showModal()
}

const closeParkingDialog = () => {
    closeParkingPanel()
    parkingDialog.value?.close()
}

const showCarlogDetail = (log) => {
    selectCarlog(log)
    selectedDetailType.value = 'CAR_LOG'
    detailFollowMode.value = 'MANUAL'
}

const selectCameraDataDetail = async (cameraData) => {
    isEditingCameraCarNo.value = false
    cameraCarNoDraft.value = ''
    isEditingCameraNote.value = false
    cameraNoteDraft.value = ''
    selectedCameraData.value = cameraData
    selectedDetailType.value = 'CAMERA_DATA'

    try {
        const response = await getCameraDataDetail(cameraData.cameraDataNo)
        selectedCameraData.value = {
            ...cameraData,
            ...(response.data ?? {}),
        }
    } catch (error) {
        console.error('카메라 데이터 상세 조회 실패', error)
    }
}

const showCameraDataDetail = async (cameraData) => {
    detailFollowMode.value = 'MANUAL'
    await selectCameraDataDetail(cameraData)
}

const followLatestCarlog = () => {
    detailFollowMode.value = 'CAR_LOG'
    const latestLog = recentCarlogs.value[0]
    if (latestLog) {
        selectCarlog(latestLog)
        selectedDetailType.value = 'CAR_LOG'
    }
}

const followLatestCameraData = async () => {
    detailFollowMode.value = 'CAMERA_DATA'
    const latestCameraData = recentCameraData.value[0]
    if (latestCameraData) {
        await selectCameraDataDetail(latestCameraData)
    }
}

const startCameraCarNoEdit = () => {
    detailFollowMode.value = 'MANUAL'
    cameraCarNoDraft.value = selectedCameraData.value?.carNo ?? ''
    isEditingCameraCarNo.value = true
}

const cancelCameraCarNoEdit = () => {
    isEditingCameraCarNo.value = false
    cameraCarNoDraft.value = ''
}

const saveCameraCarNo = async () => {
    const carNo = cameraCarNoDraft.value.trim().replace(/\s/g, '')
    const carNoPattern = /^([가-힣]{2})?\d{2,3}[가-힣]\d{4}$/

    if (!carNoPattern.test(carNo)) {
        showVehicleFeedback('차량번호 형식을 확인하세요. 예: 123가4567, 경기37바1083', 'error')
        return
    }

    cameraCarNoSaving.value = true

    try {
        const response = await editCameraDataCarNo(
            selectedCameraData.value.cameraDataNo,
            carNo,
            true,
        )

        selectedCameraData.value = {
            ...selectedCameraData.value,
            ...(response.data ?? {}),
        }
        await refreshCarlogs()
        isEditingCameraCarNo.value = false
        cameraCarNoDraft.value = ''
        showVehicleFeedback(`${carNo} 차량번호로 수정했습니다.`)
    } catch (error) {
        console.error('차량번호 수정 실패', error)
        showVehicleFeedback(
            error.response?.data?.message || '차량번호 수정에 실패했습니다.',
            'error',
        )
    } finally {
        cameraCarNoSaving.value = false
    }
}

const startCameraNoteEdit = () => {
    detailFollowMode.value = 'MANUAL'
    cameraNoteDraft.value = selectedCameraData.value?.camNote ?? ''
    isEditingCameraNote.value = true
}

const cancelCameraNoteEdit = () => {
    isEditingCameraNote.value = false
    cameraNoteDraft.value = ''
}

const saveCameraNote = async () => {
    cameraNoteSaving.value = true

    try {
        const response = await editCameraDataNote(
            selectedCameraData.value.cameraDataNo,
            cameraNoteDraft.value,
        )
        selectedCameraData.value = {
            ...selectedCameraData.value,
            ...(response.data ?? {}),
        }
        await refreshCarlogs()
        cancelCameraNoteEdit()
        showVehicleFeedback('메모가 저장되었습니다.')
    } catch (error) {
        console.error('카메라 데이터 메모 수정 실패', error)
        showVehicleFeedback(
            error.response?.data?.message || '메모 저장에 실패했습니다.',
            'error',
        )
    } finally {
        cameraNoteSaving.value = false
    }
}

const formatCameraDataTime = (value) => {
    if (!value) {
        return '-'
    }

    const date = new Date(value)
    return Number.isNaN(date.getTime()) ? value : date.toLocaleString('ko-KR')
}

const cameraLabel = (cameraNo) => {
    const labels = {
        1: 'A 입차',
        2: 'A 출차',
        3: 'B 입차',
        4: 'B 출차',
        5: 'C 입차',
        6: 'C 출차',
        7: 'D 입차',
        8: 'D 출차',
    }

    return labels[Number(cameraNo)] ?? `CAM ${cameraNo}`
}

const formatConfidence = (value) => {
    if (value === null || value === undefined) {
        return '-'
    }

    return `${Number(value).toFixed(1)}%`
}

const needsRecognitionReview = (cameraData) => {
    if (!cameraData) {
        return false
    }

    const currentCarNo = String(cameraData.carNo || '').trim()
    const originalOcrCarNo = String(cameraData.ocrCarNo || '').trim()
    const wasManuallyCorrected = currentCarNo
        && currentCarNo !== '미인식'
        && originalOcrCarNo
        && currentCarNo !== originalOcrCarNo

    if (wasManuallyCorrected) {
        return false
    }

    const score = Number(cameraData.confidenceScore)
    return !cameraData.carNo
        || cameraData.carNo === '미인식'
        || cameraData.recognitionState === false
        || (cameraData.recognitionState == null
            && !Number.isNaN(score)
            && score <= 95)
}

const recognitionStateText = (cameraData) => {
    if (!cameraData?.carNo || cameraData.carNo === '미인식') {
        return '미인식'
    }

    return needsRecognitionReview(cameraData) ? '확인 필요' : '인식 성공'
}

const isCameraPlaying = (cameraNo) => {
    return playingCameraNos.value.has(Number(cameraNo))
}

const hasStreamSession = (cameraNo) => {
    return Boolean(streamSessions.value[cameraNo])
}

const isCameraFinished = (cameraNo) => {
    return finishedCameraNos.value.has(Number(cameraNo))
}

const setCameraFinished = (cameraNo, finished) => {
    const next = new Set(finishedCameraNos.value)

    if (finished) {
        next.add(Number(cameraNo))
    } else {
        next.delete(Number(cameraNo))
    }

    finishedCameraNos.value = next
}

const getStreamUrl = (cameraNo) => {
    const session = streamSessions.value[cameraNo] ?? 0
    return `${FASTAPI_URL}/cctv/${cameraNo}/stream?session=${session}`
}

const getCameraImageUrl = (cameraDataNo) => {
    return `${import.meta.env.VITE_API_URL}/camera-data/${cameraDataNo}/image`
}

const getCameraCropImageUrl = (cameraDataNo) => {
    return `${import.meta.env.VITE_API_URL}/camera-data/${cameraDataNo}/crop-image`
}

const hasCaptureImageError = (cameraDataNo) => {
    return captureImageErrors.value.has(Number(cameraDataNo))
}

const markCaptureImageError = (cameraDataNo) => {
    const next = new Set(captureImageErrors.value)
    next.add(Number(cameraDataNo))
    captureImageErrors.value = next
}

const hasCropImageError = (cameraDataNo) => {
    return cropImageErrors.value.has(Number(cameraDataNo))
}

const markCropImageError = (cameraDataNo) => {
    const next = new Set(cropImageErrors.value)
    next.add(Number(cameraDataNo))
    cropImageErrors.value = next
}

const openImagePreview = async (cameraDataNo, carNo) => {
    imagePreviewUrl.value = getCameraImageUrl(cameraDataNo)
    imagePreviewCropUrl.value = getCameraCropImageUrl(cameraDataNo)
    imagePreviewAlt.value = carNo || '미인식 차량'
    await nextTick()

    if (!imagePreviewDialog.value?.open) {
        imagePreviewDialog.value?.showModal()
    }
}

const closeImagePreview = () => {
    imagePreviewDialog.value?.close()
}

const closeImagePreviewOnBackdrop = (event) => {
    if (event.target === imagePreviewDialog.value) {
        closeImagePreview()
    }
}

const clearImagePreview = () => {
    imagePreviewUrl.value = ''
    imagePreviewCropUrl.value = ''
    imagePreviewAlt.value = ''
}

const getPendingCameraDataNo = (cameraNo) => {
    return pendingCameraDataNos.value[cameraNo] ?? null
}

const getCameraStatus = (cameraNo) => {
    return cameraStatuses.value[cameraNo] ?? null
}

const isApprovalWaiting = (cameraNo) => {
    const status = getCameraStatus(cameraNo)
    return status?.pauseReason === 'WAITING_FOR_BACKEND'
        && Boolean(status?.pendingCameraDataNo)
}

const getPendingCameraData = (cameraNo) => {
    const cameraDataNo = getPendingCameraDataNo(cameraNo)
    return recentCameraData.value.find((item) => {
        return Number(item.cameraDataNo) === Number(cameraDataNo)
    }) ?? null
}

const isLowConfidenceWaiting = (panel) => {
    const cameraData = getPendingCameraData(panel.cameraNo)
    return cameraData?.recognitionState === false
        && cameraData?.carNo
        && cameraData.carNo !== '미인식'
}

const approvalWaitTitle = (panel) => {
    return isLowConfidenceWaiting(panel)
        ? `${panel.modeText} OCR 확인 필요`
        : `${panel.modeText} 승인 대기`
}

const approvalWaitMessage = (panel) => {
    if (isLowConfidenceWaiting(panel)) {
        const score = formatConfidence(
            getPendingCameraData(panel.cameraNo)?.confidenceScore,
        )
        return `OCR 인식 신뢰도가 ${score}입니다. 차량번호를 확인한 뒤 통과를 승인해 주세요.`
    }

    return '미등록 차량입니다. 관리자 게이트 개방을 기다리고 있습니다.'
}

const approvalWaitButtonText = (panel) => {
    return isLowConfidenceWaiting(panel)
        ? '확인 후 통과 승인'
        : '게이트 열기'
}

const setCameraPlaying = (cameraNo, playing) => {
    const next = new Set(playingCameraNos.value)

    if (playing) {
        next.add(Number(cameraNo))
    } else {
        next.delete(Number(cameraNo))
    }

    playingCameraNos.value = next
}

const toggleStream = async (panel) => {
    const cameraNo = Number(panel.cameraNo)

    if (isCameraPlaying(cameraNo)) {
        setCameraPlaying(cameraNo, false)
        await fetch(`${FASTAPI_URL}/cctv/${cameraNo}/pause`, { method: 'POST' }).catch(() => {})
        return
    }

    try {
        const endpoint = isCameraFinished(cameraNo) ? 'restart' : 'resume'
        const response = await fetch(
            `${FASTAPI_URL}/cctv/${cameraNo}/${endpoint}`,
            { method: 'POST' }
        )

        if (!response.ok) {
            throw new Error(`CCTV ${endpoint} 요청 실패: ${response.status}`)
        }

        setCameraFinished(cameraNo, false)
        streamSessions.value = {
            ...streamSessions.value,
            [cameraNo]: Date.now(),
        }
        setCameraPlaying(cameraNo, true)
    } catch (error) {
        console.error(error)
        alert('FastAPI 영상 서버에 연결할 수 없습니다')
    }
}

const changeCameraMode = async (panel) => {
    if (isCameraPlaying(panel.cameraNo)) {
        await toggleStream(panel)
    }

    toggleParkingCamera(panel.parkingName)
}

const openGateAndResume = async (panel) => {
    const pendingCameraDataNo = getPendingCameraDataNo(panel.cameraNo)
    const opened = await openManualGate(panel.gate, pendingCameraDataNo)

    if (!opened) {
        return
    }

    if (detailFollowMode.value === 'CAR_LOG') {
        followLatestCarlog()
    }

    try {
        const endpoint = pendingCameraDataNo ? 'complete' : 'resume'
        await fetch(`${FASTAPI_URL}/cctv/${panel.cameraNo}/${endpoint}`, {
            method: 'POST',
        })

        if (pendingCameraDataNo) {
            pendingCameraDataNos.value = {
                ...pendingCameraDataNos.value,
                [panel.cameraNo]: null,
            }
            cameraStatuses.value = {
                ...cameraStatuses.value,
                [panel.cameraNo]: {
                    ...cameraStatuses.value[panel.cameraNo],
                    paused: false,
                    autoPaused: false,
                    pauseReason: null,
                    pendingCameraDataNo: null,
                },
            }
        }
    } catch (error) {
        console.error('게이트 개방 후 CCTV 재생 실패', error)
    }
}

const updateCctvClock = () => {
    const now = new Date()
    const time = now.toLocaleTimeString('ko-KR', { hour12: false })
    cctvDateTime.value = time
}

const checkOcrEvents = async () => {
    const cameraNos = [...playingCameraNos.value]

    if (cameraNos.length === 0 || refreshingCarlogs) {
        return
    }

    try {
        const statuses = await Promise.all(cameraNos.map(async (cameraNo) => {
            const response = await fetch(`${FASTAPI_URL}/cctv/${cameraNo}/status`)
            return response.ok ? response.json() : null
        }))

        let hasNewOcr = false

        statuses.forEach((status) => {
            if (!status) {
                return
            }

            const cameraNo = Number(status.cameraNo)
            const eventId = Number(status.ocrEventId ?? 0)
            const previousId = Number(lastOcrEventIds.get(cameraNo) ?? 0)

            cameraStatuses.value = {
                ...cameraStatuses.value,
                [cameraNo]: status,
            }

            if (status.videoFinished) {
                setCameraPlaying(cameraNo, false)
                setCameraFinished(cameraNo, true)
            }

            pendingCameraDataNos.value = {
                ...pendingCameraDataNos.value,
                [cameraNo]: status.pendingCameraDataNo ?? null,
            }

            if (eventId > previousId) {
                hasNewOcr = true
            }

            lastOcrEventIds.set(cameraNo, eventId)
        })

        if (hasNewOcr) {
            refreshingCarlogs = true
            await Promise.all([
                refreshCarlogs(),
                refreshGateStatuses(),
            ])

            if (detailFollowMode.value === 'CAR_LOG') {
                const latestLog = recentCarlogs.value[0]
                if (latestLog) {
                    selectCarlog(latestLog)
                    selectedDetailType.value = 'CAR_LOG'
                }
            } else if (detailFollowMode.value === 'CAMERA_DATA') {
                const latestCameraData = recentCameraData.value[0]
                if (latestCameraData) {
                    await selectCameraDataDetail(latestCameraData)
                }
            }

            // The backend closes an automatically opened gate after five seconds.
            // Reload once more so the closed state is also reflected in the UI.
            window.setTimeout(() => {
                refreshGateStatuses().catch((error) => {
                    console.debug('게이트 닫힘 상태 갱신 실패', error)
                })
            }, 5500)
        }
    } catch (error) {
        console.debug('OCR 상태 확인 실패', error)
    } finally {
        refreshingCarlogs = false
    }
}
// 화면에 처음 들어왔을 때 상단 카드에 필요한 데이터를 조회
onMounted(async () => {
    updateCctvClock()
    cctvClockTimer = window.setInterval(updateCctvClock, 1000)
    ocrStatusTimer = window.setInterval(checkOcrEvents, 1000)
    await loadDashboard()
    await nextTick()
    monitoringResizeObserver = new ResizeObserver(([entry]) => {
        monitoringHeight.value = Math.round(entry.target.getBoundingClientRect().height)
    })
    if (monitoringCardRef.value) {
        monitoringResizeObserver.observe(monitoringCardRef.value)
    }
})

onBeforeUnmount(() => {
    monitoringResizeObserver?.disconnect()
    window.clearInterval(cctvClockTimer)
    window.clearInterval(ocrStatusTimer)
    playingCameraNos.value.forEach((cameraNo) => {
        fetch(`${FASTAPI_URL}/cctv/${cameraNo}/pause`, { method: 'POST' }).catch(() => {})
    })
    playingCameraNos.value = new Set()
    finishedCameraNos.value = new Set()
})
</script>
