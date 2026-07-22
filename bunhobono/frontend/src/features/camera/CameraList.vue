<template>
  <section class="camera-page">
    <!-- 카메라 목록 제목 -->
    <div class="page-heading">
      <div>
        <h2>카메라</h2>
        <p>주차장 출입구에 설치된 카메라를 관리합니다.</p>
      </div>

      <button class="register-button" 
            type="button" 
            @click="openDialog">
        + 카메라 등록
      </button>
    </div>

    

    <!-- 카메라 목록 -->
    <div class="table-wrap">
      <table>
        <thead>
          <tr>
            <th>카메라 번호</th>
            <th>게이트 이름</th>
            <th>주차장 이름</th>
            <th>카메라 이름</th>
            <th>카메라 종류</th>
            <th>설치 날짜</th>
            <th>관리</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="c in cStore.list" :key="c.cameraNo">
            <td>{{ c.cameraNo }}</td>
            <td>{{ c.gateName ?? '-' }}</td>
            <td>{{ c.parkingName ?? '-' }}</td>
            <td>{{ c.cameraName }}</td>
            <td>{{ c.cameraType }}</td>
            <td>{{ c.installDate }}</td>
            <td><button class="delete-button" type="button" @click="cStore.remove(c.cameraNo)">삭제</button></td>
          </tr>
          <tr v-if="cStore.list.length === 0">
            <td class="empty-row" colspan="7">등록된 카메라가 없습니다.</td>
          </tr>
        </tbody>
      </table>
    </div>

    <!-- 카메라 등록 다이얼로그 -->
    <dialog ref="registerDialog"
            class="camera-dialog"
            @close="resetForm"
            @click="closeOnBackdrop">

      <form class="dialog-form"
            @submit.prevent="signupGo">
      
        <!-- 다이얼로그 제목 -->
        <div class="dialog-heading">
          <div>
            <h3>새 카메라 등록</h3>
            <p>설치할 주차장과 게이트를 선택해주세요</p>
          </div>

          <button class="dialog-close"
                  type="button"
                  aria-label="등록 창 닫기"
                  @click="closeDialog">
            ✕
          </button>
        </div>

        <!-- 카메라 등록 영역 입력 -->
        <div class="form-grid">
          <label>
            <span>주차장</span>

            <select v-model="camera.parkingNo" required @change="camera.gateNo = ''">
              <option disabled value="">주차장 선택</option>
              <option v-for="p in pStore.list" :key="p.parkingNo" :value="p.parkingNo">
                {{ p.parkingName }}
              </option>
            </select>
          </label>
      
          <label>
            <span>게이트</span>
            <select v-model="camera.gateNo" :disabled="!camera.parkingNo" required>
              <option disabled value="">게이트 선택</option>
              <option v-for="g in filteredGates" :key="g.gateNo" :value="g.gateNo">
                {{ g.gateName }}
              </option>
            </select>
          </label>

          <label>
            <span>카메라 이름</span>
            <input v-model.trim="camera.cameraName" type="text" placeholder="예: 정문 입구 카메라" required />
          </label>

          <label>
            <span>카메라 종류</span>
            <select v-model="camera.cameraType" required>
              <option value="In">입차 (In)</option>
              <option value="Out">출차 (Out)</option>
            </select>
          </label>

          <label>
            <span>설치 날짜</span>
            <input v-model="camera.installDate" type="date" required />
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
import { computed, onMounted, ref } from 'vue';
import { useCameraStore } from './cameraStore';
import { useGateStore } from '@/features/gates/gateStore';
import { useParkingsStore } from '@/features/parking/parkingsStore';

const cStore = useCameraStore();
const gStore = useGateStore();
const pStore = useParkingsStore();

const registerDialog = ref(null);

// 빈 카메라 등록 정보 생성
const createEmptyCamera = () => ({
  parkingNo: '',
  gateNo: '',
  cameraName: '',
  cameraType: 'In',
  installDate: '',
});

const camera = ref(createEmptyCamera());

// 선택한 주차장에 포함된 게이트만 표시
const filteredGates = computed(() =>
  gStore.list.filter((gate) => 
    Number(gate.parkingNo) === Number(camera.value.parkingNo)),
);

// 카메라 등록 다이얼로그 열기
const openDialog = () => {
  camera.value = createEmptyCamera();
  registerDialog.value.showModal();
};

// 카메라 등록 다이얼로그 닫기
const closeDialog = () => {
  registerDialog.value.close();
}

// 다이얼로그가 닫히면 입력값 초기화
const resetForm = () => {
  camera.value = createEmptyCamera();
}

// 검은 배경 영역을 클릭하면 다이얼로그 닫기
const closeOnBackdrop = (event) => {
  if (event.target === registerDialog.value) {
    closeDialog();
  }
};

// 카메라 등록
const signupGo = async () => {
  const success = await cStore.signup(camera.value);
  if (success) closeDialog();
};

onMounted(async () => {
  await Promise.all([cStore.loadList(), pStore.loadList(), gStore.loadList()]);
});
</script>

<style scoped>
.camera-page {
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

/* 카메라 목록 */
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

/* 카메라 등록 다이얼로그 */
.camera-dialog {
  width: min(680px, calc(100% - 32px));
  max-height: calc(100vh - 40px);
  padding: 0;
  overflow-y: auto;
  border: 0;
  border-radius: 16px;
  background: #fff;
  box-shadow: 0 24px 60px rgba(15, 23, 42, 0.25);
}

.camera-dialog::backdrop {
  background: rgba(15, 23, 42, 0.55);
  backdrop-filter: blur(2px);
}

.camera-dialog .dialog-form {
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

.camera-dialog button.dialog-close {
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

.camera-dialog button.dialog-close:hover {
  color: #253047;
  background: #e5e9f0;
  transform: none;
}

/* 등록 입력 영역 */
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

select:disabled {
  color: #a0a7b5;
  background: #f1f3f7;
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

  .camera-dialog .dialog-form {
    padding: 18px;
  }
}
</style>
