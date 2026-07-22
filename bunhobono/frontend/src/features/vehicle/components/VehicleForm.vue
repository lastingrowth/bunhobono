<template>
  <div class="vehicle-management-section">
    <ManagementFeedbackToast
      :message="feedbackMessage"
      :type="feedbackType"
    />
    <div class="form-header">
      <h3>차량 등록</h3>
    </div>

    <form class="vehicle-register-form" novalidate @submit.prevent="add">
      <table class="vehicle-register-table">
        <tbody>
          <tr>
            <th>차량번호</th>
            <td>
              <div class="car-number-input-wrap">
                <input
                  ref="carNoInput"
                  v-model="carNo"
                  placeholder="차량번호 예: 12가3456"
                  required
                >
                <button
                  v-if="carNo"
                  type="button"
                  class="car-number-clear"
                  aria-label="차량번호 지우기"
                  title="차량번호 지우기"
                  @click="clearCarNo"
                >×</button>
              </div>
            </td>
          </tr>

          <tr>
            <th>차량종류</th>
            <td>
              <select v-model="vehicleType" required>
                <option value="normal">등록차량</option>
                <option value="visit">방문차량</option>
              </select>
            </td>
          </tr>

          <tr>
            <th>회원구분</th>
            <td>
              <select v-model="role" required>
                <option value="RESIDENT">입주민</option>
                <option value="ADMIN">관리자</option>
              </select>
            </td>
          </tr>

          <tr>
            <th>{{ periodLabel }}</th>
            <td>
              <select v-model.number="periodValue" required>
                <option
                  v-for="option in periodOptions"
                  :key="option.value"
                  :value="option.value"
                >
                  {{ option.text }}
                </option>
              </select>
            </td>
          </tr>

          <tr>
            <th>회원 선택</th>
            <td>
              <select v-model.number="memberNo" required>
                <option :value="null">회원 선택</option>
                <option
                  v-for="member in vehicleStore.registerMembers"
                  :key="member.memberNo"
                  :value="member.memberNo"
                >
                  {{ memberLabel(member) }}
                </option>
              </select>
            </td>
          </tr>

          <tr>
            <td colspan="2" class="vehicle-form-actions">
              <button type="submit">등록</button>
              <button type="button" @click="reset">초기화</button>
            </td>
          </tr>
        </tbody>
      </table>
    </form>
  </div>
</template>

<script setup>
import { computed, nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { useVehicleStore } from '../vehicleStore'
import ManagementFeedbackToast from '@/shared/components/ManagementFeedbackToast.vue'

const vehicleStore = useVehicleStore()
const emit = defineEmits(['back', 'registered'])

const carNo = ref('')
const carNoInput = ref(null)
const vehicleType = ref('normal')
const role = ref('RESIDENT')
const periodValue = ref(1)
const memberNo = ref(null)
const feedbackMessage = ref('')
const feedbackType = ref('success')
let feedbackTimer = null

function showFeedback(message, type = 'success') {
  feedbackMessage.value = message
  feedbackType.value = type
  window.clearTimeout(feedbackTimer)
  feedbackTimer = window.setTimeout(() => {
    feedbackMessage.value = ''
  }, 3000)
}

const carNoPattern = /^([가-힣]{2})?\d{2,3}[가-힣]\d{4}$/

const periodLabel = computed(() => {
  if (vehicleType.value === 'normal') {
    return '등록기간'
  }

  return '방문시간'
})

const periodOptions = computed(() => {
  if (vehicleType.value === 'normal') {
    return [
      { value: 1, text: '1개월' },
      { value: 3, text: '3개월' },
      { value: 6, text: '6개월' },
      { value: 12, text: '12개월' }
    ]
  }

  return [
    { value: 2, text: '2시간' },
    { value: 4, text: '4시간' },
    { value: 6, text: '6시간' },
    { value: 8, text: '8시간' },
    { value: 12, text: '12시간' }
  ]
})

const startDate = computed(() => {
  return new Date()
})

const endDate = computed(() => {
  const date = new Date(startDate.value)

  if (vehicleType.value === 'normal') {
    date.setMonth(date.getMonth() + Number(periodValue.value))
  } else {
    date.setHours(date.getHours() + Number(periodValue.value))
  }

  return date
})

watch(vehicleType, () => {
  periodValue.value = vehicleType.value === 'normal' ? 1 : 2
  memberNo.value = null
  loadRegisterMembers()
})

watch(role, () => {
  memberNo.value = null
  loadRegisterMembers()
})

onMounted(() => {
  loadRegisterMembers()
})

onBeforeUnmount(() => {
  window.clearTimeout(feedbackTimer)
})

async function loadRegisterMembers() {
  await vehicleStore.loadRegisterMembers({
    vehicleType: vehicleType.value,
    role: role.value
  })
}

async function add() {
  const normalizedCarNo = carNo.value.trim().replace(/\s/g, '')

  if (normalizedCarNo === '') {
    showFeedback('차량번호를 입력하세요.', 'error')
    return
  }

  if (!carNoPattern.test(normalizedCarNo)) {
    showFeedback('차량번호 형식이 올바르지 않습니다. 예: 12가3456, 서울12가3456', 'error')
    return
  }

  if (!memberNo.value) {
    showFeedback('회원을 선택하세요.', 'error')
    return
  }

  try {
    await vehicleStore.addVehicle({
      carNo: normalizedCarNo,
      vehicleType: vehicleType.value,
      vehicleStatus: 'APPROVED',
      memberNo: memberNo.value,
      startDate: formatDateTimeValue(startDate.value),
      endDate: formatDateTimeValue(endDate.value)
    })
  } catch (error) {
    const data = error.response?.data

    if (typeof data === 'string' && data.trim() !== '') {
      showFeedback(data, 'error')
      return
    }

    if (data?.message) {
      showFeedback(data.message, 'error')
      return
    }

    if (error.response?.status === 409) {
      showFeedback('차량 등록 조건에 맞지 않습니다.', 'error')
      return
    }

    if (error.response?.status === 403) {
      showFeedback('권한 또는 보안 설정 때문에 차량 등록 요청이 차단되었습니다.', 'error')
      return
    }

    showFeedback('차량 등록 중 오류가 발생했습니다.', 'error')
    return
  }

  reset()

  try {
    await loadRegisterMembers()
  } catch (error) {
    console.error(error)
  }

  emit('registered', '차량이 승인 등록되었습니다.')
}

function reset() {
  carNo.value = ''
  vehicleType.value = 'normal'
  role.value = 'RESIDENT'
  periodValue.value = 1
  memberNo.value = null
}

async function clearCarNo() {
  carNo.value = ''
  await nextTick()
  carNoInput.value?.focus()
}

function memberLabel(member) {
  const dong = dongText(member.memDong)
  const ho = hoText(member.memHo)

  return `${member.memName} / ${dong} ${ho}`
}

function dongText(dong) {
  if (String(dong) === '0') {
    return '관리동'
  }

  return `${dong}동`
}

function hoText(ho) {
  if (String(ho) === '0') {
    return '관리실'
  }

  return `${ho}호`
}

function formatDateTimeValue(date) {
  const yyyy = date.getFullYear()
  const mm = String(date.getMonth() + 1).padStart(2, '0')
  const dd = String(date.getDate()).padStart(2, '0')
  const hh = String(date.getHours()).padStart(2, '0')
  const mi = String(date.getMinutes()).padStart(2, '0')
  const ss = String(date.getSeconds()).padStart(2, '0')

  return `${yyyy}-${mm}-${dd}T${hh}:${mi}:${ss}`
}
</script>

<style scoped>
.vehicle-management-section {
  padding: 22px 24px;
  border: 0;
  background: transparent;
  box-shadow: none;
}

.admin-layout .content .vehicle-register-form {
  width: 100%;
  max-width: 100%;
  box-sizing: border-box;
  overflow: hidden;
  padding: 0 !important;
  border: 0 !important;
  background: transparent !important;
  box-shadow: none !important;
}

.admin-layout .content .vehicle-register-table {
  width: 100% !important;
  max-width: 100% !important;
  box-sizing: border-box;
  table-layout: fixed;
  border-collapse: collapse;
  border-spacing: 0;
  border: 0 !important;
  background: transparent !important;
  box-shadow: none !important;
}

.admin-layout .content .vehicle-register-table th,
.admin-layout .content .vehicle-register-table td {
  box-sizing: border-box;
  overflow: hidden;
  border-top: 0 !important;
  border-right: 0 !important;
  border-left: 0 !important;
  border-bottom: 0 !important;
}

.admin-layout .content .vehicle-register-table tr {
  border-bottom: 1px solid var(--admin-line, var(--border-color));
}

.admin-layout .content .vehicle-register-table tr:last-child {
  border-bottom: 0 !important;
}

.admin-layout .content .vehicle-register-table input,
.admin-layout .content .vehicle-register-table select {
  width: 100% !important;
  max-width: 100% !important;
  box-sizing: border-box;
}

.admin-layout .content .vehicle-register-table .car-number-input-wrap {
  width: 100%;
  max-width: 100%;
  box-sizing: border-box;
  overflow: hidden;
}

.form-header {
  display: flex;
  gap: 12px;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 16px;
}

.form-header h3 {
  margin: 0;
}

.back-btn {
  min-width: 88px;
  height: 36px;
  white-space: nowrap;
}

.car-number-input-wrap {
  position: relative;
  width: 100%;
}

.car-number-input-wrap input {
  padding-right: 38px !important;
}

.car-number-clear {
  position: absolute;
  top: 50%;
  right: 8px;
  width: 26px;
  min-width: 26px;
  height: 26px;
  min-height: 26px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  padding: 0;
  border: 0;
  color: #aeb6bd;
  font-size: 20px;
  font-weight: 400;
  line-height: 1;
  background: transparent;
  transform: translateY(-50%);
  cursor: pointer;
}

.car-number-clear:hover {
  color: #ffffff;
  background: #454c52;
}

.vehicle-form-actions {
  text-align: right !important;
}

.vehicle-form-actions button + button {
  margin-left: 6px;
}
</style>
