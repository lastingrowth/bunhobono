<template>
  <section class="camera-page">
    <div class="page-heading">
      <div>
        <h2>카메라 목록</h2>
        <p>주차장 출입구에 설치된 카메라를 관리합니다.</p>
      </div>
    </div>

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
            <td><span class="type-badge">{{ c.cameraType }}</span></td>
            <td>{{ c.installDate }}</td>
            <td>
              <button class="delete-button" type="button" @click="cStore.remove(c.cameraNo)">삭제</button>
            </td>
          </tr>
          <tr v-if="cStore.list.length === 0">
            <td class="empty-row" colspan="7">등록된 카메라가 없습니다.</td>
          </tr>
        </tbody>
      </table>
    </div>

    <div class="register-area">
      <button class="register-toggle" type="button" @click="toggleForm">
        {{ isFormOpen ? '등록 닫기' : '+ 카메라 등록' }}
      </button>

      <form v-if="isFormOpen" class="register-form" @submit.prevent="signupGo">
        <div class="form-heading">
          <h3>새 카메라 등록</h3>
          <p>설치할 주차장과 게이트를 선택해 주세요.</p>
        </div>

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

        <div class="form-actions">
          <button class="cancel-button" type="button" @click="closeForm">취소</button>
          <button class="submit-button" type="submit">등록하기</button>
        </div>
      </form>
    </div>
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
const isFormOpen = ref(false);

const createEmptyCamera = () => ({
  parkingNo: '', gateNo: '', cameraName: '', cameraType: 'In', installDate: '',
});

const camera = ref(createEmptyCamera());
const filteredGates = computed(() =>
  gStore.list.filter((gate) => Number(gate.parkingNo) === Number(camera.value.parkingNo)),
);

const toggleForm = () => { isFormOpen.value = !isFormOpen.value; };
const closeForm = () => {
  camera.value = createEmptyCamera();
  isFormOpen.value = false;
};
const signupGo = async () => {
  const success = await cStore.signup(camera.value);
  if (success) closeForm();
};

onMounted(async () => {
  await Promise.all([cStore.loadList(), pStore.loadList(), gStore.loadList()]);
});
</script>

<style scoped>
.camera-page { padding: 8px 0 32px; color: #253047; }
.page-heading { margin-bottom: 20px; }
.page-heading h2 { margin: 0 0 6px; font-size: 26px; }
.page-heading p, .form-heading p { margin: 0; color: #778197; font-size: 14px; }
.table-wrap { overflow-x: auto; border: 1px solid #e5e9f2; border-radius: 14px; background: #fff; box-shadow: 0 8px 24px rgba(28, 39, 60, .06); }
table { width: 100%; border-collapse: collapse; }
th, td { padding: 14px 16px; border-bottom: 1px solid #edf0f5; text-align: center; white-space: nowrap; }
th { background: #f7f9fc; color: #58647a; font-size: 13px; }
tbody tr:last-child td { border-bottom: 0; }
tbody tr:hover { background: #fbfcff; }
.type-badge { display: inline-flex; padding: 5px 10px; border-radius: 999px; background: #edf4ff; color: #2864c7; font-size: 12px; font-weight: 700; }
.empty-row { padding: 36px; color: #929bad; }
button { border: 0; border-radius: 9px; cursor: pointer; font-weight: 700; transition: .18s ease; }
button:hover { transform: translateY(-1px); }
.delete-button { padding: 7px 12px; background: #fff0f0; color: #d33f49; }
.register-area { margin-top: 22px; }
.register-toggle { width: 100%; padding: 14px 18px; border: 1px dashed #8da9dc; background: #f5f8ff; color: #315fae; font-size: 15px; }
.register-form { margin-top: 14px; padding: 24px; border: 1px solid #dfe6f2; border-radius: 14px; background: #fff; box-shadow: 0 10px 30px rgba(28, 39, 60, .07); }
.form-heading { margin-bottom: 20px; }
.form-heading h3 { margin: 0 0 6px; font-size: 20px; }
.form-grid { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 18px; }
.form-grid label { display: flex; flex-direction: column; gap: 8px; color: #4f5b70; font-size: 13px; font-weight: 700; }
input, select { width: 100%; box-sizing: border-box; padding: 11px 12px; border: 1px solid #d9dfeb; border-radius: 9px; background: #fff; color: #273148; outline: none; }
input:focus, select:focus { border-color: #5f86d4; box-shadow: 0 0 0 3px rgba(95, 134, 212, .13); }
select:disabled { background: #f1f3f7; color: #a0a7b5; }
.form-actions { display: flex; justify-content: flex-end; gap: 10px; margin-top: 24px; }
.cancel-button { padding: 10px 18px; background: #edf0f5; color: #596477; }
.submit-button { padding: 10px 22px; background: #315fae; color: #fff; }
@media (max-width: 760px) { .form-grid { grid-template-columns: 1fr; } .register-form { padding: 18px; } }
</style>
