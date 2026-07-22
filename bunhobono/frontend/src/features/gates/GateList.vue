<template>
  <section class="gate-page">
    <ManagementFeedbackToast :message="feedbackMessage" :type="feedbackType" />
    <!-- 게이트 목록 제목 -->
    <div class="page-heading">
      <div>
        <h2>게이트</h2>
        <p>주차장별 입·출차 게이트를 관리합니다.</p>
      </div>

      <button class="register-button"
              type="button"
              @click="openDialog">
        + 게이트 등록
      </button>
    </div>

    <!-- 게이트 목록 -->
    <div class="table-wrap">
      <table>
        <thead>
          <tr>
            <th>게이트 번호</th>
            <th>게이트 이름</th>
            <th>주차장 이름</th>
            <th>게이트 분류</th>
            <th>상태</th>
            <th>관리</th>
          </tr>
        </thead>

        <tbody>
          <tr v-for="g in gStore.list" :key="g.gateNo">
            <td>{{ g.gateNo }}</td>
            <td>{{ g.gateName }}</td>
            <td>{{ g.parkingName ?? '-' }}</td>
            <td>{{ g.gateType }}</td>
            <td>{{ g.gateStatus === 1 ? '열림' : '닫힘' }}</td>
            <td>
              <button class="delete-button" type="button" @click="requestDelete(g)">삭제</button>
            </td>
          </tr>

          <tr v-if="gStore.list.length === 0">
            <td class="empty-row" colspan="6">등록된 게이트가 없습니다.</td>
          </tr>
        </tbody>
      </table>
    </div>

    <!-- 게이트 등록 다이얼로그 -->
    <dialog ref="registerDialog"
            class="gate-dialog"
            @close="resetForm"
            @click="closeOnBackdrop">
      
      <form class="dialog-form" @submit.prevent="signupGo">
        <!-- 다이얼로그 제목 -->
        <div class="dialog-heading">
          <div>
            <h3>새 게이트 등록</h3>
            <p>게이트가 속한 주차장과 분류를 입력해주세요</p>
          </div>

          <button class="dialog-close"
                  type="button"
                  aria-label="등록 창 닫기"
                  @click="closeDialog">
            ✕ 
          </button>
        </div>

        <!-- 게이트 등록 입력 영역 -->
        <div class="form-grid">
          <label>
            <span>주차장</span>
            <select v-model="gate.parkingNo" required>
              <option disabled value="">
                주차장 선택
              </option>

              <option v-for="p in pStore.list"
                      :key="p.parkingNo"
                      :value="p.parkingNo">
                {{ p.parkingName }}
              </option>
            </select>
          </label>

          <label>
            <span>게이트 이름</span>

            <input v-model.trim="gate.gateName"
                   type="text"
                   placeholder="예 : A동 입구" required/>
          </label>
          
          <label>
            <span>게이트 분류</span>

            <select v-model="gate.gateType" required>
              <option value="IN">입차 (IN)</option>
              <option value="OUT">출차 (OUT)</option>
            </select>
          </label>
        </div>

        <!-- 다이얼로그 하단 버튼 -->
        <div class="form-actions">
          <button class="cancel-button"
                  type="button"
                  @click="closeDialog">
            취소
          </button>

          <button class="submit-button"
                  type="submit">
            등록
          </button>
        </div>
      </form>
    </dialog>
    <ManagementDeleteConfirm
      :open="Boolean(pendingDeleteItem)"
      title="게이트 삭제"
      :item-name="pendingDeleteItem?.gateName || ''"
      message="게이트를 삭제하시겠습니까?"
      caution="연결된 카메라나 입출차 기록이 있으면 삭제할 수 없습니다."
      :deleting="deleting"
      @cancel="cancelDelete"
      @confirm="confirmDelete"
    />
  </section>
</template>

<script setup>
import { onMounted, ref } from 'vue';
import { useGateStore } from './gateStore';
import { useParkingsStore } from '@/features/parking/parkingsStore';
import ManagementDeleteConfirm from '@/shared/components/ManagementDeleteConfirm.vue';
import ManagementFeedbackToast from '@/shared/components/ManagementFeedbackToast.vue';

const gStore = useGateStore();
const pStore = useParkingsStore();

const registerDialog = ref(null);
const pendingDeleteItem = ref(null);
const deleting = ref(false);
const feedbackMessage = ref('');
const feedbackType = ref('success');
let feedbackTimer;

const showFeedback = (message, type = 'success') => {
  feedbackMessage.value = message;
  feedbackType.value = type;
  window.clearTimeout(feedbackTimer);
  feedbackTimer = window.setTimeout(() => { feedbackMessage.value = ''; }, 2500);
};

const requestDelete = (item) => { pendingDeleteItem.value = item; };
const cancelDelete = () => { if (!deleting.value) pendingDeleteItem.value = null; };
const confirmDelete = async () => {
  if (!pendingDeleteItem.value || deleting.value) return;
  deleting.value = true;
  const result = await gStore.remove(pendingDeleteItem.value.gateNo);
  deleting.value = false;
  pendingDeleteItem.value = null;
  if (result?.success) {
    showFeedback('게이트를 삭제했습니다.');
  } else {
    showFeedback(result?.message || '게이트 삭제에 실패했습니다.', 'error');
  }
};

// 빈 게이트 등록 정보 생성
const createEmptyGate = () => ({
  parkingNo: '',
  gateName: '',
  gateType: 'IN',
});

const gate = ref(createEmptyGate());

// 게이트 등록 다이얼로그 열기
const openDialog = () => {
  gate.value = createEmptyGate();
  registerDialog.value.showModal();
};

// 게이트 등록 다이얼로그 닫기
const closeDialog = () => {
  registerDialog.value.close();
};

// 다이얼로그가 닫히면 입력값 초기화
const resetForm = () => {
  gate.value = createEmptyGate();
};

// 검은 배경 영역을 클릭하면 다이얼로그 닫기
const closeOnBackdrop = (event) => {
  if (event.target === registerDialog.value) {
    closeDialog();
  }
}

// 게이트 등록
const signupGo = async () => {
  const success = await gStore.signup(gate.value);
  if (success) {
    closeDialog();
  };
};

onMounted(async () => {
  await Promise.all([
    gStore.loadList(),
    pStore.loadList(),
  ]);
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

.gate-page {
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

/* 게이트 목록 */
.table-wrap {
  overflow-x: auto;
  border: 1px solid transparent;
  border-radius: 6px;
  background: #fff;
  box-shadow: 0 10px 26px rgba(35, 52, 66, 0.11);
}

table {
  width: 100%;
  border-collapse: collapse;
}

th,
td {
  padding: 14px 16px;
  border-bottom: 1px solid #edf0f5;
  text-align: center;
  white-space: nowrap;
}

th {
  background: #f7f9fc;
  color: #58647a;
  font-size: 13px;
}

tbody tr:last-child td {
  border-bottom: 0;
}

tbody tr:hover {
  background: #fbfcff;
}

/* 게이트 열기/닫기 버튼 */
.status-button {
  margin-right: 8px;
  padding: 7px 12px;
  color: #1f6fd1;
  background: #eaf4ff;
}

.empty-row {
  padding: 36px;
  color: #929bad;
}

/* 버튼 */
button {
  border: 0;
  border-radius: 9px;
  cursor: pointer;
  font-weight: 700;
  transition: 0.18s ease;
}

button:hover {
  transform: translateY(-1px);
}

.register-button {
  padding: 11px 18px;
  color: #fff;
  background: #315fae;
}

.delete-button {
  padding: 7px 12px;
  color: #d33f49;
  background: #fff0f0;
}

/* 게이트 등록 다이얼로그 */
.gate-dialog {
  width: min(680px, calc(100% - 32px));
  max-height: calc(100vh - 40px);
  padding: 0;
  overflow-y: auto;
  border: 0;
  border-radius: 16px;
  background: #fff;
  box-shadow: 0 24px 60px rgba(15, 23, 42, 0.25);
}

.gate-dialog::backdrop {
  background: rgba(15, 23, 42, 0.55);
  backdrop-filter: blur(2px);
}

.gate-dialog .dialog-form {
  width: auto;
  max-width: none;
  padding: 24px;
  border: 0;
  border-radius: 0;
  box-shadow: none;
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

.gate-dialog button.dialog-close {
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
}

.gate-dialog button.dialog-close:hover {
  color: #253047;
  background: #e5e9f0;
  transform: none;
}

/* 게이트 등록 입력 영역 */
.form-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
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

input,
select {
  width: 100%;
  box-sizing: border-box;
  padding: 11px 12px;
  border: 1px solid #d9dfeb;
  border-radius: 9px;
  outline: none;
  color: #273148;
  background: #fff;
}

input:focus,
select:focus {
  border-color: #5f86d4;
  box-shadow: 0 0 0 3px rgba(95, 134, 212, 0.13);
}

/* 다이얼로그 하단 버튼 */
.form-actions {
  margin-top: 24px;
  display: flex;
  justify-content: flex-end;
  gap: 10px;
}

.cancel-button {
  padding: 10px 18px;
  color: #596477;
  background: #edf0f5;
}

.submit-button {
  padding: 10px 22px;
  color: #fff;
  background: #315fae;
}

@media (max-width: 760px) {
  .page-heading {
    align-items: flex-start;
    flex-direction: column;
  }

  .form-grid {
    grid-template-columns: 1fr;
  }

  .gate-dialog .dialog-form {
    padding: 18px;
  }
}
</style>
