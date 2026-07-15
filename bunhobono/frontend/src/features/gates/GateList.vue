<template>
  <section class="gate-page">
    <!-- 게이트 목록 제목 -->
    <div class="page-heading">
      <div>
        <h2>게이트 목록</h2>
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
            <th>관리</th>
          </tr>
        </thead>

        <tbody>
          <tr v-for="g in gStore.list" :key="g.gateNo">
            <td>{{ g.gateNo }}</td>
            <td>{{ g.gateName }}</td>
            <td>{{ g.parkingName ?? '-' }}</td>
            <td><span class="type-badge">{{ g.gateType }}</span></td>
            <td>
              <button class="delete-button" type="button" @click="gStore.remove(g.gateNo)">
                삭제
              </button>
            </td>
          </tr>

          <tr v-if="gStore.list.length === 0">
            <td class="empty-row" colspan="5">등록된 게이트가 없습니다.</td>
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
  </section>
</template>

<script setup>
import { onMounted, ref } from 'vue';
import { useGateStore } from './gateStore';
import { useParkingsStore } from '@/features/parking/parkingsStore';

const gStore = useGateStore();
const pStore = useParkingsStore();

const registerDialog = ref(null);

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
  border: 1px solid #e5e9f2;
  border-radius: 14px;
  background: #fff;
  box-shadow: 0 8px 24px rgba(28, 39, 60, 0.06);
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

.type-badge {
  padding: 5px 10px;
  display: inline-flex;
  border-radius: 999px;
  color: #2864c7;
  background: #edf4ff;
  font-size: 12px;
  font-weight: 700;
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