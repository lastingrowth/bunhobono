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
                <select
                  v-if="isEditing"
                  v-model.number="feeRuleNoDraft"
                  class="fee-rule-select"
                  :disabled="
                    billingStore.loading
                      || activeFeeRules.length === 0
                  "
                >
                  <option :value="null" disabled>
                    요금 규칙을 선택하세요
                  </option>

                  <option
                    v-for="rule in activeFeeRules"
                    :key="rule.feeRuleNo"
                    :value="rule.feeRuleNo"
                  >
                    {{ rule.ruleName }}{{ rule.isDefault ? ' (기본)' : '' }}
                  </option>
                </select>

                <RouterLink
                  v-else-if="detail.ruleName"
                  :to="{ name: 'AdminFeeRuleList' }"
                  class="fee-rule-link"
                >
                  {{ detail.ruleName }}
                </RouterLink>

                <span v-else>
                  -
                </span>
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
import { computed, onMounted, onUnmounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useDialog } from '@/shared/alert/useDialog'
import { useBillingStore } from './billingStore'
import { useFeeRuleStore } from '../fee-rule/feeRuleStore'

const route = useRoute()
const router = useRouter()
const billingStore = useBillingStore()
const feeRuleStore = useFeeRuleStore()
const { alertDialog } = useDialog()

// 정산 수정 화면 전환 여부
const isEditing = ref(false)

// 관리자가 입력한 무료시간
const freeTimeDraft = ref(0)

// 관리자가 선택한 요금 규칙 번호
const feeRuleNoDraft = ref(null)

// 활성 요금 규칙을 판별하는 기준시각
const currentTime = ref(Date.now())

// 정산 상세 자동조회 타이머
let billingDetailTimer = null

const detail = computed(() => {
  return billingStore.adminBillingDetail
})

// 현재 활성화된 요금 규칙을 기본 적용 규칙부터 표시한다.
const activeFeeRules = computed(() => {
  const now = currentTime.value

  return feeRuleStore.feeRuleList
    .filter((rule) => {
      const effectiveFrom = new Date(rule.effectiveFrom).getTime()
      const effectiveTo = rule.effectiveTo
        ? new Date(rule.effectiveTo).getTime()
        : null

      return effectiveFrom <= now
        && (effectiveTo === null || effectiveTo > now)
    })
    .sort((a, b) => {
      if (a.isDefault === b.isDefault) {
        return 0
      }

      return a.isDefault ? -1 : 1
    })
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

// 현재 무료시간과 요금 규칙을 입력값에 복사하고 수정 상태로 전환한다.
const startEdit = () => {
  currentTime.value = Date.now()
  freeTimeDraft.value = Number(detail.value?.freeTime ?? 0)

  const isCurrentRuleActive = activeFeeRules.value.some(
    (rule) => rule.feeRuleNo === detail.value?.feeRuleNo
  )

  feeRuleNoDraft.value = isCurrentRuleActive
    ? detail.value.feeRuleNo
    : null

  isEditing.value = true
}

// 입력값을 버리고 상세 조회 상태로 돌아간다.
const cancelEdit = () => {
  freeTimeDraft.value = Number(detail.value?.freeTime ?? 0)
  feeRuleNoDraft.value = null
  isEditing.value = false
}

// 수정한 무료시간과 요금 규칙을 저장하고 재계산된 상세정보를 표시한다.
const saveEdit = async () => {
  const freeTime = Number(freeTimeDraft.value)
  const feeRuleNo = Number(feeRuleNoDraft.value)

  if (!Number.isInteger(freeTime) || freeTime < 0) {
    await alertDialog({
      theme: 'admin',
      type: 'warning',
      title: '정산 수정값 확인',
      message: '무료시간은 0분 이상의 정수로 입력해 주세요.'
    })
    return
  }

  if (!Number.isInteger(feeRuleNo) || feeRuleNo <= 0) {
    await alertDialog({
      theme: 'admin',
      type: 'warning',
      title: '정산 수정값 확인',
      message: '적용할 요금 규칙을 선택해 주세요.'
    })
    return
  }

  const result = await billingStore.saveAdminBilling(
    detail.value.billNo,
    {
      freeTime,
      feeRuleNo
    }
  )

  if (!result.success) {
    await alertDialog({
      theme: 'admin',
      type: 'error',
      title: '정산 수정 실패',
      message: result.message
    })
    return
  }

  feeRuleNoDraft.value = null
  isEditing.value = false

  await alertDialog({
    theme: 'admin',
    type: 'success',
    title: '정산 수정 완료',
    message: '정산정보를 수정했습니다.'
  })
}

// 정산 목록 화면으로 돌아간다.
const goList = () => {
  router.push({
    name: 'AdminBillingList'
  })
}

// 정산 상세정보와 선택 가능한 요금 규칙 목록을 조회한다.
onMounted(async () => {
  const billNo = Number(route.params.billNo)

  await Promise.all([
    billingStore.loadAdminBillingDetail(billNo),
    feeRuleStore.loadFeeRuleList()
  ])

  // 수정 중이 아닌 미결제 정산서를 1분마다 다시 조회한다.
  billingDetailTimer = window.setInterval(() => {
    if (!isEditing.value && detail.value?.billStatus === 'UNPAID') {
      billingStore.loadAdminBillingDetail(billNo, false)
    }
  }, 60000)
})

// 상세 화면을 벗어나면 자동조회 타이머를 중지한다.
onUnmounted(() => {
  if (billingDetailTimer !== null) {
    window.clearInterval(billingDetailTimer)
    billingDetailTimer = null
  }
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

.free-time-input,
.fee-rule-select {
  box-sizing: border-box;
  width: 100%;
  padding: 9px 12px;
  border: 1px solid var(--admin-line);
  border-radius: 4px;
  color: var(--admin-ink);
  background: var(--admin-surface);
  font: inherit;
}

.free-time-input {
  max-width: 180px;
}

.fee-rule-select {
  max-width: 300px;
}

.free-time-input:focus,
.fee-rule-select:focus {
  border-color: var(--admin-muted);
  outline: none;
  box-shadow: 0 0 0 3px rgba(81, 91, 99, 0.14);
}

.fee-rule-link {
  color: #d4b83f;
  font-weight: 700;
  text-decoration: underline;
  text-underline-offset: 3px;
}

.fee-rule-link:hover {
  color: #ead46d;
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
