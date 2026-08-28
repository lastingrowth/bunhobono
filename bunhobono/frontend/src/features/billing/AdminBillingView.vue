<template>
  <section class="management-list-page billing-admin-page">
    <header class="management-list-header billing-admin-header">
      <h2 class="management-list-title">정산 목록</h2>

      <button
        type="button"
        class="fee-rule-button"
        @click="goFeeRules"
      >
        요금 규칙 관리
      </button>
    </header>

    <p v-if="billingStore.loading" class="page-state">
      정산 목록을 불러오는 중입니다.
    </p>

    <p
      v-else-if="billingStore.errorMessage"
      class="page-state error"
    >
      {{ billingStore.errorMessage }}
    </p>

    <div
      v-else
      class="admin-table-scroll management-list-table"
    >
      <table>
        <thead>
          <tr>
            <th>번호</th>
            <th>차량번호</th>
            <th>차량 구분</th>
            <th>주차장</th>
            <th>정산금액</th>
            <th>정산상태</th>
            <th>출차 가능</th>
            <th>관리</th>
          </tr>
        </thead>

        <tbody>
          <tr
            v-for="(billing, index) in pagedBillingList"
            :key="billing.billNo"
          >
            <td>{{ (currentPage - 1) * pageSize + index + 1 }}</td>
            <td>
              <router-link
                :to="{
                  name: 'AdminBillingDetail',
                  params: { billNo: billing.billNo }
                }"
              >
                {{ billing.carNo }}
              </router-link>
            </td>
            <td>{{ carKindText(billing.carKind) }}</td>
            <td>{{ billing.parkingCode }}</td>
            <td>{{ amountText(billing.billAmount) }}</td>
            <td>{{ billStatusText(billing.billStatus) }}</td>
            <td>
              {{ exitStatusText(billing.outTime, billing.exitAllowed) }}
            </td>
            <td>
              <button
                type="button"
                class="archive-button list-delete-text"
                @click="pendingArchiveBilling = billing"
              >
                삭제
              </button>
            </td>
          </tr>

          <tr v-if="billingStore.adminBillingList.length === 0">
            <td colspan="8" class="empty-cell">
              현재 주차 중인 비입주민 차량이 없습니다.
            </td>
          </tr>
        </tbody>
      </table>
    </div>
    <Pagination
      v-if="adminBillingList.length > 0"
      :current-page="currentPage"
      :total-pages="totalPages"
      :page-numbers="pageNumbers"
      @change-page="setPage"
    />
    <ManagementConfirm
      :open="Boolean(pendingArchiveBilling)"
      title="정산 내역 이동"
      :item-name="pendingArchiveBilling?.carNo || ''"
      message="정산 내역을 지난 기록으로 이동하시겠습니까?"
      caution="이동한 정산 내역은 휴지통에서 확인할 수 있습니다."
      :processing="archiving"
      cancel-text="취소"
      confirm-text="이동"
      processing-text="이동 중..."
      @cancel="cancelArchive"
      @confirm="confirmArchive"
    />
    <ManagementFeedbackToast
      :message="feedbackMessage"
      :type="feedbackType"
    />
  </section>
</template>

<script setup>
import { onMounted, onUnmounted, ref } from 'vue'
import { storeToRefs } from 'pinia'
import { useRouter } from 'vue-router'
import Pagination from '@/shared/pagination/Pagination.vue'
import { usePagination } from '@/shared/pagination/usePagination'
import { useBillingStore } from './billingStore'
import ManagementConfirm from '@/shared/components/ManagementConfirm.vue'
import ManagementFeedbackToast from '@/shared/components/ManagementFeedbackToast.vue'

const billingStore = useBillingStore()
const router = useRouter()

// 지난 기록으로 이동할 정산 내역
const pendingArchiveBilling = ref(null)

// 지난 기록 이동 요청 진행 여부
const archiving = ref(false)

// 지난 기록 이동 결과 안내 문구와 표시 유형
const feedbackMessage = ref('')
const feedbackType = ref('success')

// 결과 안내 문구 자동 종료 타이머
let feedbackTimer

// 지난 기록 이동 성공·실패 결과를 표시한다.
const showFeedback = (message, type = 'success') => {
  feedbackMessage.value = message
  feedbackType.value = type
  window.clearTimeout(feedbackTimer)
  feedbackTimer = window.setTimeout(() => {
    feedbackMessage.value = ''
  }, 2500)
}

// 스토어의 정산 목록을 공용 페이지네이션에서 사용할 ref로 가져온다.
const { adminBillingList } = storeToRefs(billingStore)

// 한 페이지에 표시할 정산 건수
const pageSize = 10

// 공용 페이지네이션에서 현재 페이지의 정산 목록과 페이지 정보를 가져온다.
const {
  currentPage,
  totalPages,
  pageNumbers,
  paginatedItems: pagedBillingList,
  setPage
} = usePagination(adminBillingList, pageSize)

// 관리자 정산 목록을 10초마다 갱신하는 타이머
let billingListTimer = null

// 차량 등록상태를 관리자 화면용 한글로 표시한다.
const carKindText = (carKind) => {
  return {
    VISIT: '방문차량',
    UNKNOWN: '미등록차량'
  }[carKind] || '-'
}

// 정산서가 아직 없으면 미정산으로 표시한다.
const billStatusText = (billStatus) => {
  return {
    UNPAID: '미결제',
    PAID: '정산완료'
  }[billStatus] || '미정산'
}

// 출차 완료 여부와 현재 출차 가능 여부를 구분해 표시한다.
const exitStatusText = (outTime, exitAllowed) => {
  if (outTime) {
    return '출차 완료'
  }

  return exitAllowed ? '가능' : '불가능'
}

// 정산서가 없는 차량은 정산금액을 표시하지 않는다.
const amountText = (amount) => {
  if (amount === null || amount === undefined) {
    return '-'
  }

  return `${Number(amount).toLocaleString('ko-KR')}원`
}

// 요금 규칙 관리 화면으로 이동한다.
const goFeeRules = () => {
  router.push({
    name: 'AdminFeeRuleList'
  })
}

// 이동 요청 중이 아닐 때 확인 Dialog를 닫는다.
const cancelArchive = () => {
  if (!archiving.value) {
    pendingArchiveBilling.value = null
  }
}

// 선택한 정산 내역을 지난 기록으로 이동한다.
const confirmArchive = async () => {
  if (!pendingArchiveBilling.value || archiving.value) {
    return
  }

  // 정산서가 생성되지 않은 미정산 차량은 삭제 요청을 보내지 않는다.
  if (!pendingArchiveBilling.value.billNo) {
    showFeedback(
      '미정산 차량은 삭제할 수 없습니다.',
      'error'
    )
    pendingArchiveBilling.value = null
    return
  }

  archiving.value = true

  const result =
    await billingStore.moveAdminBillingToTrash(
      pendingArchiveBilling.value.billNo
    )

  archiving.value = false

  if (!result.success) {
    showFeedback(result.message, 'error')
    pendingArchiveBilling.value = null
    return
  }

  pendingArchiveBilling.value = null
  showFeedback(
    '정산 내역을 지난 기록으로 이동했습니다.'
  )
}

// 화면 진입 시 목록을 바로 조회하고 이후 1분마다 조용히 갱신한다.
onMounted(async () => {
  await billingStore.loadAdminBillingList()

  billingListTimer = window.setInterval(() => {
    billingStore.loadAdminBillingList(false)
  }, 60000)
})

// 관리자 정산 화면을 벗어나면 자동 갱신과 결과 안내 타이머를 중지한다.
onUnmounted(() => {
  if (billingListTimer !== null) {
    window.clearInterval(billingListTimer)
    billingListTimer = null
  }

  window.clearTimeout(feedbackTimer)
})
</script>

<style scoped>
.billing-admin-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.fee-rule-button {
  min-height: 38px;
  padding: 8px 16px;
  border: 1px solid #d4b83f;
  border-radius: 0;
  color: #1f2428;
  background: #d4b83f;
  font: inherit;
  font-weight: 700;
  cursor: pointer;
}

.billing-admin-page table {
  width: 100%;
  border-collapse: collapse;
}

.billing-admin-page th,
.billing-admin-page td {
  padding: 14px 12px;
  text-align: center;
  white-space: nowrap;
}

.billing-admin-page .page-state,
.billing-admin-page .empty-cell {
  padding: 36px;
  text-align: center;
}

.billing-admin-page .page-state.error {
  color: #c73d3d;
}

.archive-button {
  margin-left: 8px;
  padding: 8px 16px;
  border: 0;
  border-radius: 7px;
  color: #fff;
  background: #6b7280;
  font-weight: 700;
  cursor: pointer;
}
</style>
