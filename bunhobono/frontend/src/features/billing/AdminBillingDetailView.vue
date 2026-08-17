<template>
  <section class="management-detail-page billing-detail-page">
    <header class="billing-detail-header">
      <div>
        <span class="detail-category">BILLING</span>
        <h2>정산 상세</h2>
      </div>

      <button
        type="button"
        class="list-button"
        @click="goList"
      >
        목록으로
      </button>
    </header>

    <p
      v-if="billingStore.loading"
      class="page-state"
    >
      정산 상세정보를 불러오는 중입니다.
    </p>

    <p
      v-else-if="billingStore.errorMessage"
      class="page-state error"
    >
      {{ billingStore.errorMessage }}
    </p>

        <template v-else-if="billingStore.adminBillingDetail">
      <section class="detail-card">
        <dl class="detail-list">
            <div>
                <dt>차량번호</dt>
                <dd>{{ valueText(detail.carNo) }}</dd>
            </div>

            <div>
                <dt>차량 구분</dt>
                <dd>{{ carKindText(detail.carKind) }}</dd>
            </div>

            <div>
                <dt>주차장</dt>
                <dd>{{ valueText(detail.parkingCode) }}</dd>
            </div>

            <div>
                <dt>입차시각</dt>
                <dd>{{ dateTimeText(detail.inTime) }}</dd>
            </div>

            <div>
                <dt>출차시각</dt>
                <dd>{{ dateTimeText(detail.outTime) }}</dd>
            </div>

            <div>
                <dt>무료시간</dt>
                <dd>
                <input
                    v-if="isEditing"
                    v-model.number="freeTimeDraft"
                    type="number"
                    min="0"
                    step="1"
                    class="free-time-input"
                    aria-label="무료시간"
                />

                <span v-else>
                    {{ minuteText(detail.freeTime) }}
                </span>
                </dd>
            </div>

            <div>
                <dt>과금시간</dt>
                <dd>{{ minuteText(detail.chargeMinutes) }}</dd>
            </div>

            <div>
                <dt>정산금액</dt>
                <dd>{{ amountText(detail.billAmount) }}</dd>
            </div>

            <div>
                <dt>정산상태</dt>
                <dd>{{ billStatusText(detail.billStatus) }}</dd>
            </div>

            <div>
                <dt>결제수단</dt>
                <dd>{{ valueText(detail.paymentMethod) }}</dd>
            </div>

            <div>
                <dt>결제시각</dt>
                <dd>{{ dateTimeText(detail.paidAt) }}</dd>
            </div>

            <div>
                <dt>출차 가능 여부</dt>
                <dd>{{ exitStatusText(detail.outTime, detail.exitAllowed) }}</dd>
            </div>

            <div>
                <dt>출차 가능 종료시각</dt>
                <dd>{{ dateTimeText(detail.exitAllowedUntil) }}</dd>
            </div>

            <div>
              <dt>요금 규칙명</dt>
              <dd>
                <button
                  type="button"
                  class="fee-rule-link"
                  :disabled="!detail.ruleName"
                  @click="goFeeRules"
                >
                  {{ valueText(detail.ruleName) }}
                </button>
              </dd>
            </div>
        </dl>
      </section>

            <div class="detail-actions">
        <button
          type="button"
          class="list-button"
          :disabled="billingStore.loading"
          @click="goList"
        >
          목록으로
        </button>

        <button
          v-if="
            !isEditing
            && detail.billStatus !== 'PAID'
          "
          type="button"
          class="edit-button"
          @click="startEdit"
        >
          정산 수정
        </button>

        <template v-if="isEditing">
          <button
            type="button"
            class="cancel-button"
            :disabled="billingStore.loading"
            @click="cancelEdit"
          >
            취소
          </button>

          <button
            type="button"
            class="save-button"
            :disabled="billingStore.loading"
            @click="saveEdit"
          >
            {{ billingStore.loading ? '저장 중' : '저장' }}
          </button>
        </template>
      </div>
    </template>
  </section>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useBillingStore } from './billingStore'

const route = useRoute()
const router = useRouter()
const billingStore = useBillingStore()

// 정산 수정 화면 전환 여부
const isEditing = ref(false)

// 관리자가 입력한 무료시간
const freeTimeDraft = ref(0)

const detail = computed(() => {
  return billingStore.adminBillingDetail
})

// 값이 없는 상세항목은 하이픈으로 표시한다.
const valueText = (value) => {
  if (value === null
        || value === undefined
        || value === ''
  ) {
    return '-'
  }

  return value
}

// 차량 등록상태를 관리자 화면용 한글로 표시한다.
const carKindText = (carKind) => {
  return {
    VISIT: '방문차량',
    UNKNOWN: '미등록차량'
  }[carKind] || '-'
}

// 정산서가 아직 생성되지 않은 차량은 미정산으로 표시한다.
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

// 분 단위 값이 없는 경우 하이픈으로 표시한다.
const minuteText = (minutes) => {
  if (minutes === null || minutes === undefined) {
    return '-'
  }

  return `${Number(minutes).toLocaleString('ko-KR')}분`
}

// 금액이 없는 경우 하이픈으로 표시한다.
const amountText = (amount) => {
  if (amount === null || amount === undefined) {
    return '-'
  }

  return `${Number(amount).toLocaleString('ko-KR')}원`
}

// 값이 없는 시각은 하이픈으로 표시한다.
const dateTimeText = (dateTime) => {
  if (!dateTime) {
    return '-'
  }

  return new Intl.DateTimeFormat('ko-KR', {
    dateStyle: 'medium',
    timeStyle: 'short'
  }).format(new Date(dateTime))
}

// 현재 무료시간을 입력값에 복사하고 수정 상태로 전환한다.
const startEdit = () => {
  freeTimeDraft.value = Number(detail.value?.freeTime ?? 0)
  isEditing.value = true
}

// 입력값을 버리고 상세 조회 상태로 돌아간다.
const cancelEdit = () => {
  freeTimeDraft.value = Number(detail.value?.freeTime ?? 0)
  isEditing.value = false
}

// 수정한 무료시간을 저장하고 재계산된 상세정보를 표시한다.
const saveEdit = async () => {
  const freeTime = Number(freeTimeDraft.value)

  if (!Number.isInteger(freeTime) || freeTime < 0) {
    window.alert('무료시간은 0분 이상의 정수로 입력해주세요.')
    return
  }

  const result = await billingStore.saveAdminBilling(
    detail.value.carLogNo,
    freeTime
  )

  if (!result.success) {
    window.alert(result.message)
    return
  }

  isEditing.value = false
}

// 요금 규칙 관리 화면으로 이동한다.
const goFeeRules = () => {
  router.push({
    name: 'AdminFeeRuleList'
  })
}

// 정산 목록 화면으로 돌아간다.
const goList = () => {
  router.push({
    name: 'AdminBillingList'
  })
}

// 주소의 입출차 기록 번호로 정산 상세정보를 조회한다.
onMounted(() => {
  billingStore.loadAdminBillingDetail(
    Number(route.params.carLogNo)
  )
})
</script>

<style scoped>
.billing-detail-page {
  display: grid;
  gap: 20px;
}

.billing-detail-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.billing-detail-header h2 {
  margin: 4px 0 0;
}

.detail-category {
  color: var(--admin-muted);
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 0.08em;
}

.detail-card {
  overflow: hidden;
  border: 1px solid var(--admin-line);
  border-radius: 12px;
  background: var(--admin-surface);
}

.detail-list {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  margin: 0;
}

.detail-list > div {
  display: grid;
  grid-template-columns: 160px minmax(0, 1fr);
  min-width: 0;
  border-bottom: 1px solid var(--admin-line);
}

.detail-list > div:nth-child(odd) {
  border-right: 1px solid var(--admin-line);
}

.detail-list > div:nth-last-child(-n + 2) {
  border-bottom: 0;
}

.detail-list dt,
.detail-list dd {
  margin: 0;
  padding: 16px 18px;
}

.detail-list dt {
  color: var(--admin-muted);
  font-weight: 700;
  background: var(--admin-surface-muted);
}

.detail-list dd {
  min-width: 0;
  color: var(--admin-ink);
  background: var(--admin-surface);
  overflow-wrap: anywhere;
}

.free-time-input {
  width: 100%;
  max-width: 180px;
  padding: 9px 12px;
  border: 1px solid var(--admin-line);
  border-radius: 4px;
  color: var(--admin-ink);
  background: var(--admin-surface);
  font: inherit;
}

.free-time-input:focus {
  border-color: var(--admin-muted);
  outline: none;
  box-shadow: 0 0 0 3px rgba(81, 91, 99, 0.14);
}

.detail-actions {
  display: flex;
  justify-content: center;
  gap: 12px;
}

.list-button,
.edit-button,
.cancel-button,
.save-button {
  min-width: 120px;
  padding: 11px 18px;
  border: 1px solid var(--admin-ink);
  border-radius: 4px;
  font-weight: 700;
  cursor: pointer;
}

.list-button,
.cancel-button {
  color: var(--admin-ink);
  background: var(--admin-surface);
}

.edit-button,
.save-button {
  color: var(--admin-surface);
  background: var(--admin-ink);
}

.list-button:hover,
.cancel-button:hover {
  background: var(--admin-surface-muted);
}

.edit-button:hover,
.save-button:hover {
  border-color: var(--admin-muted);
  background: var(--admin-muted);
}

.fee-rule-link {
  padding: 0;
  border: 0;
  border-bottom: 1px solid currentColor;
  color: #d4b83f;
  background: transparent;
  font: inherit;
  font-weight: 700;
  cursor: pointer;
}

.fee-rule-link:hover:not(:disabled) {
  color: #ead46d;
}

.fee-rule-link:disabled {
  border-bottom-color: transparent;
  color: var(--admin-muted);
  cursor: default;
}

.list-button:disabled,
.cancel-button:disabled,
.save-button:disabled {
  cursor: not-allowed;
  opacity: 0.55;
}

.page-state {
  padding: 40px;
  text-align: center;
}

.page-state.error {
  color: #c73d3d;
}

@media (max-width: 900px) {
  .detail-list {
    grid-template-columns: 1fr;
  }

  .detail-list > div:nth-child(odd) {
    border-right: 0;
  }

  .detail-list > div:nth-last-child(-n + 2) {
    border-bottom: 1px solid var(--admin-line);
  }

  .detail-list > div:last-child {
    border-bottom: 0;
  }
}
</style>
