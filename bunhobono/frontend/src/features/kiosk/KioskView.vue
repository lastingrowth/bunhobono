<template>
    <section class="kiosk-page management-list-page facility-list-page">
        <ManagementFeedbackToast :message="feedbackMessage" :type="feedbackType" />

        <div class="page-heading facility-list-heading">
            <div>
                <h2 class="management-list-title">키오스크</h2>
                <p>주차장에 설치된 키오스크의 위치와 장비 정보를 확인합니다.</p>
            </div>

            <button class="register-button" type="button" @click="openDialog">
                + 키오스크 등록
            </button>
        </div>

        <div class="table-wrap management-list-table facility-list-table kiosk-table-wrap">
            <table>
                <colgroup>
                    <col class="number-column" />
                    <col />
                    <col />
                    <col />
                    <col />
                    <col class="management-column" />
                </colgroup>
                <thead>
                    <tr>
                        <th>키오스크 번호</th>
                        <th>주차장 이름</th>
                        <th>모델명</th>
                        <th>설치 위치</th>
                        <th>설치일</th>
                        <th>관리</th>
                    </tr>
                </thead>

                <tbody>
                    <tr v-for="kiosk in kioskStore.list" :key="kiosk.kioskNo">
                        <td>{{ kiosk.displayNo }}</td>
                        <td>{{ kiosk.parkingName || '-' }}</td>
                        <td>{{ kiosk.modelName || '-' }}</td>
                        <td>{{ kiosk.kioskLocation || '-' }}</td>
                        <td>{{ kiosk.installDate || '-' }}</td>
                        <td>
                            <button
                                class="delete-button list-delete-text"
                                type="button"
                                @click="requestDelete(kiosk)"
                            >
                                삭제
                            </button>
                        </td>
                    </tr>

                    <tr v-if="kioskStore.loading">
                        <td class="empty-row" colspan="6">키오스크 목록을 불러오는 중입니다.</td>
                    </tr>

                    <tr v-else-if="kioskStore.errorMessage">
                        <td class="empty-row error-message" colspan="6">
                            {{ kioskStore.errorMessage }}
                        </td>
                    </tr>

                    <tr v-else-if="kioskStore.list.length === 0">
                        <td class="empty-row" colspan="6">등록된 키오스크가 없습니다.</td>
                    </tr>
                </tbody>
            </table>
        </div>

        <dialog
            ref="registerDialog"
            class="kiosk-dialog"
            @close="resetForm"
            @click="closeOnBackdrop"
        >
            <form class="dialog-form" @submit.prevent="signupGo">
                <div class="dialog-heading">
                    <div>
                        <h3>새 키오스크 등록</h3>
                        <p>설치할 주차장과 키오스크 정보를 입력해주세요.</p>
                    </div>

                    <button
                        class="dialog-close"
                        type="button"
                        aria-label="등록 창 닫기"
                        @click="closeDialog"
                    >
                        ✕
                    </button>
                </div>

                <div class="form-grid">
                    <label>
                        <span>주차장</span>
                        <select v-model="kioskForm.parkingNo" required>
                            <option disabled value="">주차장 선택</option>
                            <option
                                v-for="parking in parkingsStore.list"
                                :key="parking.parkingNo"
                                :value="parking.parkingNo"
                            >
                                {{ parking.parkingName }}
                            </option>
                        </select>
                    </label>

                    <label>
                        <span>모델명</span>
                        <input
                            v-model.trim="kioskForm.modelName"
                            type="text"
                            placeholder="예: BONO-KIOSK-V1"
                            required
                        />
                    </label>

                    <label>
                        <span>설치 위치</span>
                        <input
                            v-model.trim="kioskForm.kioskLocation"
                            type="text"
                            placeholder="예: 지하 1층 A구역"
                            required
                        />
                    </label>

                    <label>
                        <span>설치일</span>
                        <input v-model="kioskForm.installDate" type="date" required />
                    </label>
                </div>

                <p v-if="registerErrorMessage" class="register-error" role="alert">
                    {{ registerErrorMessage }}
                </p>

                <div class="form-actions">
                    <button class="cancel-button" type="button" :disabled="submitting" @click="closeDialog">
                        취소
                    </button>
                    <button class="submit-button" type="submit" :disabled="submitting">
                        {{ submitting ? '등록 중' : '등록' }}
                    </button>
                </div>
            </form>
        </dialog>

        <ManagementDeleteConfirm
            :open="Boolean(pendingDeleteItem)"
            title="키오스크 삭제"
            :item-name="pendingDeleteItem ? `${pendingDeleteItem.displayNo}번 키오스크` : ''"
            message="키오스크를 삭제하시겠습니까?"
            caution="삭제된 키오스크 정보는 복원할 수 없습니다."
            :deleting="deleting"
            @cancel="cancelDelete"
            @confirm="confirmDelete"
        />
    </section>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { useKioskStore } from './kioskStore'
import { useParkingsStore } from '@/features/parking/parkingsStore'
import ManagementDeleteConfirm from '@/shared/components/ManagementDeleteConfirm.vue'
import ManagementFeedbackToast from '@/shared/components/ManagementFeedbackToast.vue'

const kioskStore = useKioskStore()
const parkingsStore = useParkingsStore()
const registerDialog = ref(null)
const submitting = ref(false)
const registerErrorMessage = ref('')
const pendingDeleteItem = ref(null)
const deleting = ref(false)
const feedbackMessage = ref('')
const feedbackType = ref('success')
let feedbackTimer

const createEmptyKiosk = () => ({
    parkingNo: '',
    modelName: '',
    kioskLocation: '',
    installDate: '',
})

const kioskForm = ref(createEmptyKiosk())

const showFeedback = (message, type = 'success') => {
    feedbackMessage.value = message
    feedbackType.value = type
    window.clearTimeout(feedbackTimer)
    feedbackTimer = window.setTimeout(() => {
        feedbackMessage.value = ''
    }, 2500)
}

const requestDelete = (kiosk) => {
    pendingDeleteItem.value = kiosk
}

const cancelDelete = () => {
    if (!deleting.value) pendingDeleteItem.value = null
}

const openDialog = () => {
    kioskForm.value = createEmptyKiosk()
    registerErrorMessage.value = ''
    registerDialog.value.showModal()
}

const closeDialog = () => {
    if (!submitting.value) registerDialog.value.close()
}

const resetForm = () => {
    kioskForm.value = createEmptyKiosk()
    registerErrorMessage.value = ''
}

const closeOnBackdrop = (event) => {
    if (event.target === registerDialog.value) closeDialog()
}

const signupGo = async () => {
    if (submitting.value) return

    submitting.value = true
    registerErrorMessage.value = ''

    try {
        const result = await kioskStore.signup({ ...kioskForm.value })

        if (!result?.success) {
            registerErrorMessage.value = result?.message || '키오스크 등록에 실패했습니다.'
            return
        }

        showFeedback('키오스크를 등록했습니다.')

        const dialog = registerDialog.value
        if (dialog?.open && typeof dialog.close === 'function') {
            dialog.close()
        }
    } catch (error) {
        console.error('키오스크 등록 처리 실패', error)
        registerErrorMessage.value = '키오스크 등록에 실패했습니다. 잠시 후 다시 시도해주세요.'
    } finally {
        submitting.value = false
    }
}

const confirmDelete = async () => {
    if (!pendingDeleteItem.value || deleting.value) return

    deleting.value = true
    try {
        const result = await kioskStore.remove(pendingDeleteItem.value.kioskNo)

        if (result.success) {
            showFeedback('키오스크를 삭제했습니다.')
        } else {
            showFeedback(result.message, 'error')
        }
    } finally {
        deleting.value = false
        pendingDeleteItem.value = null
    }
}

onMounted(async () => {
    await Promise.all([
        kioskStore.loadList(),
        parkingsStore.loadList(),
    ])
})
</script>

<style scoped>
.kiosk-page {
    padding: 8px 0 32px;
    color: #253047;
}

.page-heading {
    margin-bottom: 20px;
    display: flex;
    justify-content: space-between;
    align-items: center;
    gap: 16px;
}

.page-heading h2 {
    margin: 0 0 6px;
    font-size: 26px;
}

.page-heading p {
    margin: 0;
    color: #778197;
    font-size: 14px;
}

.kiosk-table-wrap th,
.kiosk-table-wrap td {
    box-sizing: border-box;
    height: 38px;
    padding: 7px 12px;
    text-align: center;
    vertical-align: middle;
}

.kiosk-table-wrap table {
    table-layout: fixed;
}

.kiosk-table-wrap .number-column {
    width: 90px;
}

.kiosk-table-wrap th:first-child,
.kiosk-table-wrap td:first-child {
    padding-right: 4px;
    padding-left: 4px;
}

.kiosk-table-wrap .management-column {
    width: 90px;
}

.delete-button {
    box-sizing: border-box;
    min-width: 52px;
    height: 22px;
    padding: 2px 8px;
    border: 0;
    border-radius: 9px;
    cursor: pointer;
    color: #d33f49;
    background: #fff0f0;
    font-size: 12px;
    font-weight: 700;
    line-height: 16px;
}

.delete-button:hover {
    transform: translateY(-1px);
}

.kiosk-dialog {
    width: min(680px, calc(100% - 32px));
    max-height: calc(100vh - 40px);
    padding: 0;
    overflow-y: auto;
    border: 0;
    border-radius: 16px;
    background: #fff;
    box-shadow: 0 24px 60px rgba(15, 23, 42, 0.25);
}

.kiosk-dialog::backdrop {
    background: rgba(15, 23, 42, 0.55);
    backdrop-filter: blur(2px);
}

.dialog-form {
    padding: 24px;
}

.dialog-heading {
    margin-bottom: 22px;
    display: flex;
    justify-content: space-between;
    align-items: flex-start;
    gap: 16px;
}

.dialog-heading h3 {
    margin: 0 0 6px;
    font-size: 21px;
}

.dialog-heading p {
    margin: 0;
    color: #778197;
    font-size: 14px;
}

.dialog-close {
    width: 36px;
    height: 36px;
    padding: 0;
    flex-shrink: 0;
    border: 0;
    border-radius: 50%;
    cursor: pointer;
    color: #778197;
    background: #f1f3f7;
    font-size: 20px;
}

.form-grid {
    display: grid;
    grid-template-columns: repeat(2, minmax(0, 1fr));
    gap: 18px;
}

.form-grid label {
    display: flex;
    flex-direction: column;
    gap: 8px;
    color: #4f5b70;
    font-size: 13px;
    font-weight: 700;
}

.form-grid input,
.form-grid select {
    width: 100%;
    padding: 11px 12px;
    border: 1px solid #d9dfeb;
    border-radius: 9px;
    outline: none;
    color: #273148;
    background: #fff;
}

.form-grid input:focus,
.form-grid select:focus {
    border-color: #5f86d4;
    box-shadow: 0 0 0 3px rgba(95, 134, 212, 0.13);
}

.form-actions {
    margin-top: 24px;
    display: flex;
    justify-content: flex-end;
    gap: 10px;
}

.register-error {
    margin: 16px 0 0;
    color: #b42318;
    font-size: 13px;
    font-weight: 700;
}

.form-actions button {
    padding: 10px 18px;
    border: 0;
    border-radius: 9px;
    cursor: pointer;
    font-weight: 700;
}

.cancel-button {
    color: #596477;
    background: #edf0f5;
}

.submit-button {
    color: #fff;
    background: #315fae;
}

.form-actions button:disabled {
    cursor: wait;
    opacity: 0.6;
}

@media (max-width: 760px) {
    .form-grid {
        grid-template-columns: 1fr;
    }
}

.error-message {
    color: #b42318;
}
</style>
