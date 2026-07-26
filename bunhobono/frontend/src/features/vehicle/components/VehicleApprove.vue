<template>

    <div class="approve-header">
      <h3>승인 대기 차량</h3>
    </div>

    <div class="admin-table-scroll management-list-table">
    <table border="">
      <thead>
        <tr>
          <th>번호</th>
          <th>차량번호</th>
          <th>차량종류</th>
          <th>승인상태</th>
          <th>예상 방문시간</th>
          <th>등록시간</th>
          <th>관리</th>
        </tr>
      </thead>

      <tbody>
        <tr v-for="(vehicle, index) in paginatedItems" :key="vehicle.vehicleCarNo">
          <td>{{ (currentPage - 1) * pageSize + index + 1 }}</td>
          <td>{{ vehicle.carNo }}</td>
          <td>{{ vehicle.vehicleTypeText || vehicle.vehicleType }}</td>
          <td>{{ vehicle.vehicleStatusText || vehicle.vehicleStatus }}</td>
          <td>{{ vehicle.startDateText || '-' }}</td>
          <td>{{ vehicle.periodText || '-' }}</td>
          <td class="action-cell">
            <div class="action-buttons">
              <button
                type="button"
                class="approve-btn"
                :disabled="processingNo === vehicle.vehicleCarNo"
                @click="approve(vehicle)"
              >
                승인
              </button>

              <button
                type="button"
                class="reject-btn"
                :disabled="processingNo === vehicle.vehicleCarNo"
                @click="openRejectDialog(vehicle)"
              >
                반려
              </button>
            </div>
          </td>

        </tr>

        <tr v-if="paginatedItems.length === 0">
          <td colspan="7" align="center">
            승인 대기 차량이 없습니다.
          </td>
        </tr>
      </tbody>
    </table>

    </div>
    <div class="admin-pagination-area">


    <Pagination
      :current-page="currentPage"
      :total-pages="totalPages"
      :page-numbers="pageNumbers"
      @change-page="setPage"
    />

    <dialog ref="rejectDialog" class="reject-dialog">
      <form @submit.prevent="submitReject">
        <h3>방문차량 신청 반려</h3>

        <p>
          {{ rejectTarget?.carNo }} 차량의 반려 사유를 입력하세요.
        </p>

        <textarea
          v-model="rejectReason"
          maxlength="300"
          placeholder="입주민에게 전달할 반려 사유"
          required
          @input="rejectError = ''"
        />
        <p v-if="rejectError" class="reject-error">{{ rejectError }}</p>

        <div class="dialog-actions">
          <button type="button" @click="closeRejectDialog">취소</button>
          <button type="submit" class="reject-btn">반려 전송</button>
        </div>
      </form>
    </dialog>
    <ManagementConfirm
      :open="approveConfirmOpen"
      title="방문차량 승인"
      :item-name="approveTarget?.carNo || ''"
      message="차량의 방문 신청을 승인하시겠습니까?"
      :processing="processingNo !== null"
      confirm-text="승인"
      processing-text="승인 중"
      @cancel="cancelApprove"
      @confirm="confirmApprove"
    />  </div>
</template>

<script setup>
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import { usePagination } from '@/shared/pagination/usePagination'
import Pagination from '@/shared/pagination/Pagination.vue'
import ManagementConfirm from '@/shared/components/ManagementConfirm.vue'
import { useVehicleStore } from '../vehicleStore'

const props = defineProps({
  vehicles: {
    type: Array,
    default: () => []
  }
})

const emit = defineEmits(['back', 'feedback'])
const vehicleStore = useVehicleStore()

const processingNo = ref(null)
const approveTarget = ref(null)
const approveConfirmOpen = ref(false)
const rejectDialog = ref(null)
const rejectTarget = ref(null)
const rejectReason = ref('')
const rejectError = ref('')

const approveVehicles = computed(() => {
  const list = [...props.vehicles]

  return list.sort((a, b) => {
    const left = Number(
      a.displayNo ?? a.vehicleCarNo
    )

    const right = Number(
      b.displayNo ?? b.vehicleCarNo
    )

    return right - left
  })
})

const pageSize = 10

const {
  currentPage,
  totalPages,
  pageNumbers,
  paginatedItems,
  setPage
} = usePagination(approveVehicles, pageSize)

let refreshTimer = null

onMounted(() => {
  refreshTimer = window.setInterval(() => {
    if (!rejectDialog.value?.open && processingNo.value === null) {
      vehicleStore.loadVehicleApproveList()
    }
  }, 5000)
})

onBeforeUnmount(() => {
  window.clearInterval(refreshTimer)
})

function approve(vehicle) {
  approveTarget.value = vehicle
  approveConfirmOpen.value = true
}

function cancelApprove() {
  if (processingNo.value === null) {
    approveConfirmOpen.value = false
    approveTarget.value = null
  }
}

async function confirmApprove() {
  if (!approveTarget.value || processingNo.value !== null) {
    return
  }

  const vehicle = approveTarget.value
  processingNo.value = vehicle.vehicleCarNo
  approveConfirmOpen.value = false
  approveTarget.value = null

  try {
    await vehicleStore.changeVehicleApproveStatus(vehicle.vehicleCarNo, {
      vehicleStatus: 'APPROVED'
    })
    emit('feedback', `${vehicle.carNo} 차량이 승인되었습니다.`)
  } catch (error) {
    console.error('차량 승인 실패', error)
    emit('feedback', error.response?.data?.message || '차량 승인에 실패했습니다.', 'error')
  } finally {
    processingNo.value = null
  }
}

function openRejectDialog(vehicle) {
  rejectTarget.value = vehicle
  rejectReason.value = ''
  rejectError.value = ''
  rejectDialog.value.showModal()
}

function closeRejectDialog() {
  rejectDialog.value.close()
  rejectTarget.value = null
  rejectReason.value = ''
  rejectError.value = ''
}

async function submitReject() {
  const reason = rejectReason.value.trim()

  if (!reason || !rejectTarget.value) {
    rejectError.value = '반려 사유를 입력하세요.'
    emit('feedback', rejectError.value, 'error')
    return
  }

  const vehicle = rejectTarget.value
  processingNo.value = vehicle.vehicleCarNo
  closeRejectDialog()

  try {
    await vehicleStore.changeVehicleApproveStatus(
      vehicle.vehicleCarNo,
      {
        vehicleStatus: 'REJECTED',
        rejectReason: reason
      }
    )

    emit('feedback', `${vehicle.carNo} 차량이 반려되었습니다.`)
  } catch (error) {
    console.error('차량 반려 실패', error)
    emit('feedback', error.response?.data?.message || '차량 반려에 실패했습니다.', 'error')
  } finally {
    processingNo.value = null
  }
}
</script>

<style scoped>

table th,
table td {
  text-align: center;
  vertical-align: middle;
}

.action-cell {
  justify-content: center;
}

.approve-header,
.dialog-actions {
  display: flex;
  align-items: center;
  gap: 8px;
}

.action-buttons {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
}

.action-buttons button {
  min-width: 46px;
  height: 24px;
  padding: 2px 8px;
  line-height: 18px;
  white-space: nowrap;
}

.approve-header {
  justify-content: space-between;
  margin-bottom: 16px;
}

.approve-header h3 {
  margin: 0;
}

.back-btn {
  min-width: 88px;
  height: 36px;
}

.reject-btn {
  color: #fff;
  background: #b42318;
}

.reject-dialog {
  width: min(440px, calc(100vw - 32px));
  padding: 24px;
  border: 1px solid #d0d5dd;
  border-radius: 8px;
}

.reject-dialog::backdrop {
  background: rgba(15, 23, 42, 0.5);
}

.reject-dialog textarea {
  width: 100%;
  min-height: 120px;
  box-sizing: border-box;
  resize: vertical;
}

.reject-error {
  margin: 8px 0 0;
  color: #b42318;
  font-size: 12px;
  font-weight: 700;
}
.dialog-actions {
  justify-content: flex-end;
  margin-top: 16px;
}
</style>