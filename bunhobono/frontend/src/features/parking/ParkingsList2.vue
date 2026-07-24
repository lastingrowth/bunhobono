<template>
  <section class="parking-page management-list-page facility-list-page">
    <ManagementFeedbackToast :message="feedbackMessage" :type="feedbackType" />
    <!-- 주차장 목록 제목 + 등록 버튼 -->
    <div class="page-heading facility-list-heading">
      <div>
        <h2 class="management-list-title">주차장 관리</h2>
        <p>주차장 구역과 전체 주차 면수를 관리합니다</p>
      </div>

      <button
        class="register-button"
        type="button"
        @click="openDialog">
        + 주차장 등록
      </button>
    </div>
    
    <!-- 주차장 목록 -->
    <div class="table-wrap management-list-table facility-list-table">
      <table>
        <thead>
          <tr>
            <th>주차장번호</th>
            <th>주차장이름</th>
            <th>주차가용 수</th>
            <th>주차장위치</th>
            <th>주차중</th>
            <th>관리</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="p in pStore.list" :key="p.parkingNo">
            <td>{{ p.displayNo }}</td>

            <!-- 수정 중인 행이면 input, 아니면 기존 글자 표시 -->
            <td>
              <input
                v-if="editingParkingNo === p.parkingNo"
                v-model.trim="editParking.parkingName"
                class="inline-edit-input"
                type="text">
              <span v-else>{{ p.parkingName }}</span>
            </td>

            <td>
              <input
                v-if="editingParkingNo === p.parkingNo"
                v-model.number="editParking.parkingSpaces"
                class="inline-edit-input"
                type="number"
                min="0">
              <span v-else>{{ p.parkingSpaces }}</span>
            </td>

            <td>
              <input
                v-if="editingParkingNo === p.parkingNo"
                v-model.trim="editParking.parkingLocation"
                class="inline-edit-input"
                type="text">
              <span v-else>{{ p.parkingLocation }}</span>
            </td>

            <td>
              {{ p.parkingSpaces - p.availableSpaces }}/{{ p.parkingSpaces }}
            </td>
            <td>
              <template v-if="editingParkingNo === p.parkingNo">
                <button class="edit-button" type="button" @click="completeEdit">완료</button>
                <button class="delete-button" type="button" @click="cancelEdit">취소</button>
              </template>
              <template v-else>
                <button class="edit-button" type="button" @click="startEdit(p)">수정</button>
                <button class="delete-button" type="button" @click="requestDelete(p)">삭제</button>
              </template>
            </td>

          </tr>

          <tr v-if="pStore.list.length === 0">
            <td class="empty-row" colspan="6">
              등록된 주차장이 없습니다.
            </td>
          </tr>
        </tbody>
      </table>
    </div>

    <!-- 주차장 등록 다이얼로그 -->
    <dialog
      ref="registerDialog"
      class="parking-dialog"
      @close="resetForm"
      @click="closeOnBackdrop">
      <form
        class="dialog-form"
        @submit.prevent="signupGo">
        
        <!-- 다이얼로그 제목 -->
        <div class="dialog-heading">
          <div>
            <h3>새 주차장 등록</h3>
            <p>주차장 이름, 면수, 위치를 입력해 주세요</p>
          </div>

          <button
            class="dialog-close"
            type="button"
            aria-label="등록 창 닫기"
            @click="closeDialog">
            ✕
          </button>
        </div>

        <!-- 주차장 등록 입력 영역 -->
        <div class="form-grid parking-form-grid">
          <label>
            <span>주차장 이름</span>
            <input
              v-model.trim="parking.parkingName" 
              type="text"
              placeholder="예 : A 주차장"
              required />
          </label>

          <label>
            <span>전체 주차 면수</span>
            <input
              v-model.number="parking.parkingSpaces" 
              type="number"
              min="0"
              placeholder="예 : 100"
              required />
          </label>

          <label>
            <span>주차장 위치</span>
            <input
              v-model.trim="parking.parkingLocation" 
              type="text"
              placeholder="예 : A동 지상"
              required />
          </label>
        </div>

        <!-- 다이얼로그 하단 버튼 -->
        <div class="form-actions">
          <button
            class="cancel-button"
            type="button"
            @click="closeDialog">    
            취소
          </button>

          <button
            class="submit-button"
            type="submit">    
            등록
          </button>
        </div>
      </form>
    </dialog>
    <ManagementDeleteConfirm
      :open="Boolean(pendingDeleteItem)"
      title="주차장 삭제"
      :item-name="pendingDeleteItem?.parkingName || ''"
      message="주차장을 삭제하시겠습니까?"
      caution="연결된 게이트나 기록이 있으면 삭제할 수 없습니다."
      :deleting="deleting"
      @cancel="cancelDelete"
      @confirm="confirmDelete"
    />
  </section>
</template>

<script setup>
import { onMounted, ref } from 'vue';
import { useParkingsStore } from './parkingsStore';
import ManagementDeleteConfirm from '@/shared/components/ManagementDeleteConfirm.vue';
import ManagementFeedbackToast from '@/shared/components/ManagementFeedbackToast.vue';

const pStore = useParkingsStore();

const registerDialog = ref(null)
const pendingDeleteItem = ref(null)
const deleting = ref(false)
const feedbackMessage = ref('')
const feedbackType = ref('success')
let feedbackTimer

const showFeedback = (message, type = 'success') => {
  feedbackMessage.value = message
  feedbackType.value = type
  window.clearTimeout(feedbackTimer)
  feedbackTimer = window.setTimeout(() => { feedbackMessage.value = '' }, 2500)
}

const requestDelete = (item) => { pendingDeleteItem.value = item }
const cancelDelete = () => { if (!deleting.value) pendingDeleteItem.value = null }
const confirmDelete = async () => {
  if (!pendingDeleteItem.value || deleting.value) return
  deleting.value = true
  const result = await pStore.remove(pendingDeleteItem.value.parkingNo)
  deleting.value = false
  pendingDeleteItem.value = null
  if (result?.success) {
    showFeedback('주차장을 삭제했습니다.')
  } else {
    showFeedback(result?.message || '주차장 삭제에 실패했습니다.', 'error')
  }
}

// 빈 주차장 등록 정보 생성
const createEmptyParking = () => ({
  parkingName: '',
  parkingSpaces: '',
  parkingLocation: '',
})

const parking = ref(createEmptyParking())

// 현재 수정 중인 주차장 번호
// null이면 수정 중인 행이 없다는 뜻이다.
const editingParkingNo = ref(null)

// 목록에서 바로 수정할 값을 임시로 담아두는 객체
const editParking = ref(createEmptyParking())

// 주차장 등록 다이얼로그 열기
const openDialog = () => {
  parking.value = createEmptyParking()
  registerDialog.value.showModal()
}

// 주차장 등록 다이얼로그 닫기
const closeDialog = () => {
  registerDialog.value.close()
}

// 다이얼로그가 닫히면 입력값 초기화
const resetForm = () => {
  parking.value = createEmptyParking()
}

// 검은 배경 영역을 클릭하면 다이얼로그 닫기
const closeOnBackdrop = (event) => {
  if (event.target === registerDialog.value) {
    closeDialog()
  }
}

// 주차장 등록
const signupGo = async () => {
  try {
    await pStore.signup(parking.value)
    await pStore.loadList()
    closeDialog()
    alert('주차장 등록 완료')
  } catch (error) {
    console.error(error)
    alert('주차장 등록 실패')
  }
}

// 수정 버튼을 누르면 해당 행의 글자들이 input으로 바뀐다.
const startEdit = (parkingItem) => {
  editingParkingNo.value = parkingItem.parkingNo

  editParking.value = {
    parkingName: parkingItem.parkingName,
    parkingSpaces: parkingItem.parkingSpaces,
    parkingLocation: parkingItem.parkingLocation,
  }
}

// 취소를 누르면 input을 닫고 기존 목록 형태로 되돌린다.
const cancelEdit = () => {
  editingParkingNo.value = null
  editParking.value = createEmptyParking()
}

// 완료 버튼을 누르면 수정 API를 호출하고 목록을 다시 불러온다.
const completeEdit = async () => {
  if (!editParking.value.parkingName || !editParking.value.parkingLocation) {
    alert('주차장 이름과 위치를 입력해 주세요')
    return
  }

  if (editParking.value.parkingSpaces === '' || Number(editParking.value.parkingSpaces) < 0) {
    alert('주차 가능 대수를 확인해 주세요')
    return
  }

  try {
    await pStore.update(editingParkingNo.value, editParking.value)

    editingParkingNo.value = null
    editParking.value = createEmptyParking()
  } catch (error) {
    console.error(error)
    alert('주차장 수정 실패')
  }
}

onMounted(() => {
  pStore.loadList();
});

</script>

<style scoped>
.table-wrap th,
.table-wrap td {
  box-sizing: border-box;
  height: 30px !important;
  padding: 4px 7px !important;
  font-size: 13px;
  line-height: 1.3;
  text-align: center !important;
  vertical-align: middle;
}

.table-wrap tbody tr {
  height: 30px !important;
}

.table-wrap td button {
  box-sizing: border-box;
  width: auto;
  min-width: 52px;
  height: 22px !important;
  min-height: 0 !important;
  padding: 2px 8px !important;
  font-size: 12px;
  line-height: 16px;
  white-space: nowrap;
}

.table-wrap .inline-edit-input {
  display: block;
  width: 100%;
  min-width: 100%;
  height: 22px;
  min-height: 0;
  padding: 2px 6px;
  font-size: 12px;
  box-sizing: border-box;
}

.table-wrap td:has(.inline-edit-input) {
  padding: 0 !important;
}

@media (max-width: 1000px) {
  .table-wrap th,
  .table-wrap td,
  .table-wrap td button { font-size: 12px; }
}

@media (max-width: 700px) {
  .table-wrap th,
  .table-wrap td,
  .table-wrap td button { font-size: 11px; }
}

.parking-page {
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

.page-heading p,
.dialog-heading p {
  margin: 0;
  color: #778197;
  font-size: 14px;
}

.register-button {
  min-height: 42px;
  padding: 0 18px;
  border: 1px solid #2d8cff;
  border-radius: 10px;
  background: #2d8cff;
  color: #fff;
  font-weight: 700;
  cursor: pointer;
}

.register-button:hover {
  background: #1677e8;
}

.table-wrap {
  overflow-x: auto;
  border: 1px solid #d8e1ec;
  border-radius: 14px;
  background: #fff;
}

table {
  width: 100%;
  border-collapse: collapse;
}

th,
td {
  padding: 16px 18px;
  border-bottom: 1px solid #e1e8f0;
  text-align: left;
  vertical-align: middle;
}

th {
  background: #f8fafc;
  color: #1f2f46;
  font-weight: 800;
}

td {
  color: #26384f;
}

tbody tr:hover {
  background: #f7fbff;
}

.empty-row {
  padding: 40px 16px;
  color: #8a96a8;
  text-align: center;
}

.inline-edit-input {
  width: 100%;
  min-width: 90px;
  height: 34px;
  padding: 6px 10px;
  border: 1px solid #cfd8e3;
  border-radius: 8px;
  font-size: 14px;
  color: #253047;
  background: #fff;
  box-sizing: border-box;
}

.inline-edit-input:focus {
  outline: none;
  border-color: #2f80ed;
  box-shadow: 0 0 0 3px rgba(47, 128, 237, 0.12);
}

.delete-button {
  padding: 8px 14px;
  border: 1px solid #d6e0eb;
  border-radius: 9px;
  background: #fff;
  color: #1f2f46;
  font-weight: 700;
  cursor: pointer;
}

.delete-button:hover {
  border-color: #ff6b6b;
  color: #e03131;
}

/* 주차장 등록 다이얼로그 */
.parking-dialog {
  width: min(560px, calc(100% - 32px));
  padding: 0;
  border: 0;
  border-radius: 18px;
  background: #fff;
  box-shadow: 0 24px 70px rgba(15, 23, 42, 0.28);
}

.parking-dialog::backdrop {
  background: rgba(15, 23, 42, 0.42);
}

.dialog-form {
  padding: 22px 18px 18px;
}

.dialog-heading {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 16px;
  margin-bottom: 18px;
}

.dialog-heading h3 {
  margin: 0 0 6px;
  color: #1f2f46;
  font-size: 22px;
}

/* 카메라/게이트 등록창과 같은 닫기 버튼 스타일 */
.parking-dialog button.dialog-close {
  width: 36px;
  height: 36px;
  min-width: 36px;
  padding: 0;
  display: flex;
  justify-content: center;
  align-items: center;
  flex-shrink: 0;
  border: 0;
  border-radius: 50%;
  color: #778197;
  background: #f1f3f7;
  font-size: 20px;
  line-height: 1;
  cursor: pointer;
}

.parking-dialog button.dialog-close:hover {
  color: #253047;
  background: #e5e9f0;
  transform: none;
}

.form-grid {
  display: grid;
  gap: 16px;
}

/* 주차장 등록 입력칸을 게이트 등록처럼 가로 한 줄로 배치 */
.parking-form-grid {
  grid-template-columns: 1fr 0.8fr 1fr;
}

.form-grid label {
  display: grid;
  gap: 8px;
}

.form-grid span {
  color: #34445c;
  font-size: 14px;
  font-weight: 800;
}

.form-grid input {
  min-height: 42px;
  padding: 0 12px;
  border: 1px solid #d3deeb;
  border-radius: 10px;
  color: #1f2f46;
  font-size: 15px;
}

.form-grid input:focus {
  outline: none;
  border-color: #2d8cff;
  box-shadow: 0 0 0 3px rgba(45, 140, 255, 0.14);
}

.form-actions {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  margin-top: 18px;
}

.cancel-button,
.submit-button {
  min-height: 40px;
  padding: 0 16px;
  border-radius: 10px;
  font-weight: 800;
  cursor: pointer;
}

.cancel-button {
  border: 1px solid #d6e0eb;
  background: #fff;
  color: #4d5b70;
}

.submit-button {
  border: 1px solid #2d8cff;
  background: #2d8cff;
  color: #fff;
}

.cancel-button:hover {
  background: #f5f7fa;
}

.submit-button:hover {
  background: #1677e8;
}

@media (max-width: 720px) {
  .page-heading {
    align-items: flex-start;
    flex-direction: column;
  }

  .parking-form-grid {
    grid-template-columns: 1fr;
  }
}
</style>
