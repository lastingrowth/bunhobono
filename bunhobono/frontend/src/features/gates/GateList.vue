<template>
  <section class="gate-page">
    <div class="page-heading">
      <h2>게이트 목록</h2>
      <p>주차장별 입·출차 게이트를 관리합니다.</p>
    </div>

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

    <div class="register-area">
      <button class="register-toggle" type="button" @click="toggleForm">
        {{ isFormOpen ? '등록 닫기' : '+ 게이트 등록' }}
      </button>

      <form v-if="isFormOpen" class="register-form" @submit.prevent="signupGo">
        <div class="form-heading">
          <h3>새 게이트 등록</h3>
          <p>게이트가 속한 주차장과 분류를 입력해 주세요.</p>
        </div>

        <div class="form-grid">
          <label>
            <span>주차장</span>
            <select v-model="gate.parkingNo" required>
              <option disabled value="">주차장 선택</option>
              <option v-for="p in pStore.list" :key="p.parkingNo" :value="p.parkingNo">
                {{ p.parkingName }}
              </option>
            </select>
          </label>

          <label>
            <span>게이트 이름</span>
            <input v-model.trim="gate.gateName" type="text" placeholder="예: A동 입구" required />
          </label>

          <label>
            <span>게이트 분류</span>
            <select v-model="gate.gateType" required>
              <option value="In">입차 (In)</option>
              <option value="Out">출차 (Out)</option>
              <option value="Both">입·출차 (Both)</option>
            </select>
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
import { onMounted, ref } from 'vue';
import { useGateStore } from './gateStore';
import { useParkingsStore } from '@/features/parking/parkingsStore';

const gStore = useGateStore();
const pStore = useParkingsStore();
const isFormOpen = ref(false);

const createEmptyGate = () => ({
  parkingNo: '',
  gateName: '',
  gateType: 'In',
});

const gate = ref(createEmptyGate());

const toggleForm = () => {
  isFormOpen.value = !isFormOpen.value;
};

const closeForm = () => {
  gate.value = createEmptyGate();
  isFormOpen.value = false;
};

const signupGo = async () => {
  const success = await gStore.signup(gate.value);
  if (success) closeForm();
};

onMounted(async () => {
  await Promise.all([
    gStore.loadList(),
    pStore.loadList(),
  ]);
});
</script>

<style scoped>
.gate-page { padding: 8px 0 32px; color: #253047; }
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
.form-grid { display: grid; grid-template-columns: repeat(3, minmax(0, 1fr)); gap: 18px; }
.form-grid label { display: flex; flex-direction: column; gap: 8px; color: #4f5b70; font-size: 13px; font-weight: 700; }
input, select { width: 100%; box-sizing: border-box; padding: 11px 12px; border: 1px solid #d9dfeb; border-radius: 9px; background: #fff; color: #273148; outline: none; }
input:focus, select:focus { border-color: #5f86d4; box-shadow: 0 0 0 3px rgba(95, 134, 212, .13); }
.form-actions { display: flex; justify-content: flex-end; gap: 10px; margin-top: 24px; }
.cancel-button { padding: 10px 18px; background: #edf0f5; color: #596477; }
.submit-button { padding: 10px 22px; background: #315fae; color: #fff; }
@media (max-width: 760px) { .form-grid { grid-template-columns: 1fr; } .register-form { padding: 18px; } }
</style>
