<template>

    <div class="approve-header">
      <h3>승인 대기 차량</h3>
    </div>

    <div class="admin-table-scroll">
    <table border="">
      <thead>
        <tr>
          <th>차량번호</th>
          <th>차량종류</th>
          <th>승인상태</th>
          <th>예상 방문시간</th>
          <th>등록시간</th>
          <th>관리</th>
        </tr>
      </thead>

      <tbody>
        <tr v-for="vehicle in paginatedItems" :key="vehicle.vehicleCarNo">
          <td>{{ vehicle.carNo }}</td>
          <td>{{ vehicle.vehicleTypeText || vehicle.vehicleType }}</td>
          <td>{{ vehicle.vehicleStatusText || vehicle.vehicleStatus }}</td>
          <td>{{ vehicle.startDateText || '-' }}</td>
          <td>{{ vehicle.periodText || '-' }}</td>
          <td class="action-cell">
            <button
              type="button"
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
          </td>

        </tr>

        <tr v-if="paginatedItems.length === 0">
          <td colspan="6" align="center">
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
        />

        <div class="dialog-actions">
          <button type="button" @click="closeRejectDialog">취소</button>
          <button type="submit" class="reject-btn">반려 전송</button>
        </div>
      </form>
    </dialog>
  </div>
</template>

<script setup>
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import { usePagination } from '@/shared/pagination/usePagination'
import Pagination from '@/shared/pagination/Pagination.vue'
import { useVehicleStore } from '../vehicleStore'

const props = defineProps({
  vehicles: {
    type: Array,
    default: () => []
  }
})

const emit = defineEmits(['back'])
const vehicleStore = useVehicleStore()

const processingNo = ref(null)
const rejectDialog = ref(null)
const rejectTarget = ref(null)
const rejectReason = ref('')

const approveVehicles = computed(() => props.vehicles)
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

async function approve(vehicle) {
  if (!confirm(`${vehicle.carNo} 차량을 승인할까요?`)) {
    return
  }

  processingNo.value = vehicle.vehicleCarNo

  try {
    await vehicleStore.changeVehicleApproveStatus(vehicle.vehicleCarNo, {
      vehicleStatus: 'APPROVED'
    })
  } finally {
    processingNo.value = null
  }
}

function openRejectDialog(vehicle) {
  rejectTarget.value = vehicle
  rejectReason.value = ''
  rejectDialog.value.showModal()
}

function closeRejectDialog() {
  rejectDialog.value.close()
  rejectTarget.value = null
  rejectReason.value = ''
}

async function submitReject() {
  const reason = rejectReason.value.trim()

  if (!reason || !rejectTarget.value) {
    alert('반려 사유를 입력하세요.')
    return
  }

  processingNo.value = rejectTarget.value.vehicleCarNo

  try {
    await vehicleStore.changeVehicleApproveStatus(
      rejectTarget.value.vehicleCarNo,
      {
        vehicleStatus: 'REJECTED',
        rejectReason: reason
      }
    )

    closeRejectDialog()
  } finally {
    processingNo.value = null
  }
}
</script>

<style scoped>
.approve-header,
.action-cell,
.dialog-actions {
  display: flex;
  align-items: center;
  gap: 8px;
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

.dialog-actions {
  justify-content: flex-end;
  margin-top: 16px;
}
</style>