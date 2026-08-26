<template>
  <section class="management-list-page fee-rule-page">
    <header class="management-list-header fee-rule-header">
      <h2 class="management-list-title">요금 규칙 관리</h2>

      <div class="header-actions">
        <button
          type="button"
          class="secondary-button"
          @click="goBillingList"
        >
          목록으로
        </button>

        <button
          type="button"
          class="primary-button"
          @click="openCreateDialog"
        >
          새 요금 규칙 등록
        </button>
      </div>
    </header>

    <p v-if="feeRuleStore.loading" class="page-state">
      요금 규칙 목록을 불러오는 중입니다.
    </p>

    <p
      v-else-if="feeRuleStore.errorMessage"
      class="page-state error"
    >
      {{ feeRuleStore.errorMessage }}
    </p>

    <template v-else>
      <section class="rule-section">
        <h3>현재 사용 중인 요금 규칙</h3>

        <div class="management-list-table table-wrap">
          <table>
            <thead>
              <tr>
                <th>규칙명</th>
                <th>과금 단위</th>
                <th>단위요금</th>
                <th>일일 최대요금</th>
                <th>출차 유예시간</th>
                <th>기본 적용</th>
                <th>적용 시작일시</th>
                <th>적용 종료일시</th>
              </tr>
            </thead>

            <tbody>
              <tr
                v-for="rule in currentFeeRules"
                :key="rule.feeRuleNo"
              >
                <td>{{ rule.ruleName }}</td>
                <td>{{ minuteText(rule.unitMinutes) }}</td>
                <td>{{ amountText(rule.unitFee) }}</td>
                <td>{{ amountText(rule.dailyMaxFee) }}</td>
                <td>{{ minuteText(rule.exitGraceMinutes) }}</td>
                <td>{{ rule.isDefault ? '예' : '아니오' }}</td>
                <td>{{ dateTimeText(rule.effectiveFrom) }}</td>
                <td>{{ dateTimeText(rule.effectiveTo) }}</td>
              </tr>

              <tr v-if="currentFeeRules.length === 0">
                <td colspan="8" class="empty-cell">
                  현재 사용 중인 요금 규칙이 없습니다.
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </section>

      <section class="rule-section">
        <h3>전체 요금 규칙</h3>

        <div class="management-list-table table-wrap">
          <table>
            <thead>
              <tr>
                <th>번호</th>
                <th>상태</th>
                <th>규칙명</th>
                <th>과금 단위</th>
                <th>단위요금</th>
                <th>일일 최대요금</th>
                <th>출차 유예시간</th>
                <th>기본 적용</th>
                <th>등록일시</th>
                <th>적용 시작일시</th>
                <th>적용 종료일시</th>
                <th>관리</th>
              </tr>
            </thead>

            <tbody>
              <tr
                v-for="(rule, index) in feeRuleStore.feeRuleList"
                :key="rule.feeRuleNo"
              >
                <td>{{ index + 1 }}</td>
                <td>
                  <span
                    class="status-badge"
                    :class="statusClass(rule)"
                  >
                    {{ statusText(rule) }}
                  </span>
                </td>
                <td>{{ rule.ruleName }}</td>
                <td>{{ minuteText(rule.unitMinutes) }}</td>
                <td>{{ amountText(rule.unitFee) }}</td>
                <td>{{ amountText(rule.dailyMaxFee) }}</td>
                <td>{{ minuteText(rule.exitGraceMinutes) }}</td>
                <td>{{ rule.isDefault ? '예' : '아니오' }}</td>
                <td>{{ dateTimeText(rule.createdAt) }}</td>
                <td>{{ dateTimeText(rule.effectiveFrom) }}</td>
                <td>{{ dateTimeText(rule.effectiveTo) }}</td>
                <td>
                  <button
                    type="button"
                    class="secondary-button rule-edit-button"
                    :disabled="
                      statusText(rule) === '종료'
                        || feeRuleStore.saving
                    "
                    @click="openEditDialog(rule)"
                  >
                    {{ editButtonText(rule) }}
                  </button>
                </td>
              </tr>

              <tr v-if="feeRuleStore.feeRuleList.length === 0">
                <td colspan="12" class="empty-cell">
                  등록된 요금 규칙이 없습니다.
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </section>
    </template>

    <dialog
      ref="createDialog"
      class="fee-rule-dialog"
      aria-labelledby="fee-rule-dialog-title"
      @close="resetForm"
      @click="closeCreateOnBackdrop"
    >
      <button
        type="button"
        class="dialog-close"
        aria-label="요금 규칙 등록창 닫기"
        :disabled="feeRuleStore.saving"
        @click="closeCreateDialog"
      >
        ×
      </button>

      <h3 id="fee-rule-dialog-title">
        새 요금 규칙 등록
      </h3>

      <form @submit.prevent="submitFeeRule">
        <label>
          규칙명
          <input
            v-model.trim="form.ruleName"
            type="text"
            maxlength="100"
            required
          />
        </label>

        <label>
          과금 단위 (분)
          <input
            v-model.number="form.unitMinutes"
            type="number"
            min="1"
            step="1"
            required
          />
        </label>

        <label>
          단위요금 (원)
          <input
            v-model.number="form.unitFee"
            type="number"
            min="0"
            step="1"
            required
          />
        </label>

        <label>
          일일 최대요금
          <input
            v-model.number="form.dailyMaxFee"
            type="number"
            min="0"
            step="1"
            placeholder="제한이 없으면 비워두세요"
          />
        </label>

        <label>
          출차 유예시간 (분)
          <input
            v-model.number="form.exitGraceMinutes"
            type="number"
            min="0"
            step="1"
            required
          />
        </label>

        <label class="checkbox-label">
          <input
            v-model="form.isDefault"
            type="checkbox"
          />
          활성 규칙 중 기본으로 적용
        </label>

        <label>
          적용 시작일시
          <input
            v-model="form.effectiveFrom"
            type="datetime-local"
            :min="effectiveFromMin"
            required
          />
        </label>

        <label>
          적용 종료일시
          <input
            v-model="form.effectiveTo"
            type="datetime-local"
          />
        </label>

        <div class="dialog-actions">
          <button
            type="button"
            class="secondary-button"
            :disabled="feeRuleStore.saving"
            @click="closeCreateDialog"
          >
            취소
          </button>

          <button
            type="submit"
            class="primary-button"
            :disabled="feeRuleStore.saving"
          >
            {{ feeRuleStore.saving ? '등록 중' : '등록' }}
          </button>
        </div>
      </form>
    </dialog>

    <dialog
      ref="editDialog"
      class="fee-rule-dialog"
      aria-labelledby="fee-rule-edit-dialog-title"
      @cancel.prevent="closeEditDialog"
      @click="closeEditOnBackdrop"
    >
      <button
        type="button"
        class="dialog-close"
        aria-label="요금 규칙 수정창 닫기"
        :disabled="feeRuleStore.saving"
        @click="closeEditDialog"
      >
        ×
      </button>

      <h3 id="fee-rule-edit-dialog-title">
        요금 규칙 수정
      </h3>

      <form @submit.prevent="submitEditFeeRule">
        <label>
          규칙명
          <input
            v-model.trim="editForm.ruleName"
            type="text"
            maxlength="100"
            required
            :disabled="feeRuleStore.saving"
          />
        </label>

        <label>
          과금 단위 (분)
          <input
            v-model.number="editForm.unitMinutes"
            type="number"
            min="1"
            step="1"
            required
            :disabled="feeRuleStore.saving"
          />
        </label>

        <label>
          단위요금 (원)
          <input
            v-model.number="editForm.unitFee"
            type="number"
            min="0"
            step="1"
            required
            :disabled="feeRuleStore.saving"
          />
        </label>

        <label>
          일일 최대요금
          <input
            v-model.number="editForm.dailyMaxFee"
            type="number"
            min="0"
            step="1"
            placeholder="제한이 없으면 비워두세요"
            :disabled="feeRuleStore.saving"
          />
        </label>

        <label>
          출차 유예시간 (분)
          <input
            v-model.number="editForm.exitGraceMinutes"
            type="number"
            min="0"
            step="1"
            required
            :disabled="feeRuleStore.saving"
          />
        </label>

        <label class="checkbox-label">
          <input
            v-model="editForm.isDefault"
            type="checkbox"
            :disabled="!scheduledEdit || feeRuleStore.saving"
          />
          활성 규칙 중 기본으로 적용
        </label>

        <label>
          적용 시작일시
          <input
            v-model="editForm.effectiveFrom"
            type="datetime-local"
            :min="effectiveFromMin"
            required
            :disabled="feeRuleStore.saving"
          />
        </label>

        <label>
          적용 종료일시
          <input
            v-model="editForm.effectiveTo"
            type="datetime-local"
            :disabled="feeRuleStore.saving"
          />
        </label>

        <div class="dialog-actions">
          <button
            type="button"
            class="secondary-button"
            :disabled="feeRuleStore.saving"
            @click="closeEditDialog"
          >
            취소
          </button>

          <button
            type="submit"
            class="primary-button"
            :disabled="feeRuleStore.saving"
          >
            {{ feeRuleStore.saving ? '수정 중' : '수정' }}
          </button>
        </div>
      </form>
    </dialog>
  </section>
</template>

<script setup>
import { computed, onMounted, onUnmounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { useDialog } from '@/shared/alert/useDialog'
import { useFeeRuleStore } from './feeRuleStore'

const router = useRouter()
const feeRuleStore = useFeeRuleStore()
const { alertDialog } = useDialog()

// 신규 등록 Dialog 요소
const createDialog = ref(null)

// 요금 규칙 수정 Dialog 요소
const editDialog = ref(null)

// 현재 열린 Dialog에서 선택할 수 있는 가장 이른 적용 시작일시
const effectiveFromMin = ref('')

// 수정할 요금 규칙
const selectedFeeRule = ref(null)

// 요금 규칙 수정 입력값
const editForm = reactive({
  ruleName: '',
  unitMinutes: 30,
  unitFee: 0,
  dailyMaxFee: '',
  exitGraceMinutes: 30,
  isDefault: false,
  effectiveFrom: '',
  effectiveTo: ''
})

// 요금 규칙 상태 계산에 사용하는 현재 시각
const currentTime = ref(Date.now())

// 현재 시각을 주기적으로 갱신하는 타이머
let statusTimer = null

// 현재 시각에서 최소 5분 뒤의 기본 시작일시
const defaultEffectiveFrom = computed(() => {
  const date = new Date(
    Math.ceil((currentTime.value + 5 * 60000) / 60000) * 60000
  )

  date.setMinutes(
    date.getMinutes() - date.getTimezoneOffset()
  )

  return date.toISOString().slice(0, 16)
})

// 서버의 날짜·시간을 datetime-local 입력값으로 변환한다.
const dateTimeInput = (dateTime) => {
  if (!dateTime) {
    return ''
  }

  const date = new Date(dateTime)

  date.setMinutes(
    date.getMinutes() - date.getTimezoneOffset()
  )

  return date.toISOString().slice(0, 16)
}

// 새 요금 규칙 입력값
const form = reactive({
  ruleName: '',
  unitMinutes: 30,
  unitFee: 0,
  dailyMaxFee: '',
  exitGraceMinutes: 30,
  isDefault: false,
  effectiveFrom: defaultEffectiveFrom.value,
  effectiveTo: ''
})

// 입력값을 최초 상태로 되돌린다.
const resetForm = () => {
  form.ruleName = ''
  form.unitMinutes = 30
  form.unitFee = 0
  form.dailyMaxFee = ''
  form.exitGraceMinutes = 30
  form.isDefault = false
  form.effectiveFrom = effectiveFromMin.value
  form.effectiveTo = ''
}

// 규칙의 적용 시작·종료일시로 현재 상태를 구분한다.
const statusText = (rule) => {
  const now = currentTime.value
  const effectiveFrom = new Date(rule.effectiveFrom).getTime()
  const effectiveTo = rule.effectiveTo
    ? new Date(rule.effectiveTo).getTime()
    : null

  if (now < effectiveFrom) {
    return '예약'
  }

  if (effectiveTo !== null && now >= effectiveTo) {
    return '종료'
  }

  return '사용 중'
}

// 상태별 표시 색상 클래스를 반환한다.
const statusClass = (rule) => {
  return {
    예약: 'reserved',
    '사용 중': 'using',
    종료: 'ended'
  }[statusText(rule)]
}

// 종료 여부에 따라 요금 규칙 수정 버튼 문구를 반환한다.
const editButtonText = (rule) => {
  return statusText(rule) === '종료'
    ? '수정 불가'
    : '수정'
}

// 현재 사용 중인 규칙을 기본 적용 규칙부터 표시한다.
const currentFeeRules = computed(() => {
  return feeRuleStore.feeRuleList
    .filter((rule) => statusText(rule) === '사용 중')
    .sort((a, b) => {
      if (a.isDefault === b.isDefault) {
        return 0
      }

      return a.isDefault ? -1 : 1
    })
})

// 분 단위 값을 화면용 문구로 표시한다.
const minuteText = (minutes) => {
  if (minutes === null || minutes === undefined) {
    return '-'
  }

  return `${Number(minutes).toLocaleString('ko-KR')}분`
}

// 금액을 원 단위 문구로 표시한다.
const amountText = (amount) => {
  if (amount === null
        || amount === undefined
        || amount === ''
  ) {
    return '-'
  }

  return `${Number(amount).toLocaleString('ko-KR')}원`
}

// 날짜와 시각이 없으면 하이픈으로 표시한다.
const dateTimeText = (dateTime) => {
  if (!dateTime) {
    return '-'
  }

  return new Intl.DateTimeFormat('ko-KR', {
    dateStyle: 'medium',
    timeStyle: 'short'
  }).format(new Date(dateTime))
}

// 신규 요금 규칙 등록 Dialog를 연다.
const openCreateDialog = () => {
  effectiveFromMin.value = defaultEffectiveFrom.value
  resetForm()
  createDialog.value.showModal()
}

// 저장 중이 아닐 때 등록 Dialog를 닫는다.
const closeCreateDialog = () => {
  if (feeRuleStore.saving) {
    return
  }

  createDialog.value.close()
}

// 등록 Dialog의 바깥 영역을 클릭하면 닫는다.
const closeCreateOnBackdrop = (event) => {
  if (event.target === createDialog.value) {
    closeCreateDialog()
  }
}

// 선택한 요금 규칙이 예약 상태인지 확인한다.
const scheduledEdit = computed(() => {
  return selectedFeeRule.value
    && statusText(selectedFeeRule.value) === '예약'
})

// 선택한 요금 규칙의 기존 값으로 수정 Dialog를 연다.
const openEditDialog = (rule) => {
  if (statusText(rule) === '종료') {
    return
  }

  selectedFeeRule.value = rule
  effectiveFromMin.value = defaultEffectiveFrom.value

  const effectiveFrom = dateTimeInput(rule.effectiveFrom)

  editForm.ruleName = rule.ruleName
  editForm.unitMinutes = rule.unitMinutes
  editForm.unitFee = rule.unitFee
  editForm.dailyMaxFee = rule.dailyMaxFee ?? ''
  editForm.exitGraceMinutes = rule.exitGraceMinutes
  editForm.isDefault = rule.isDefault
  editForm.effectiveFrom = scheduledEdit.value && effectiveFrom >= effectiveFromMin.value ? effectiveFrom : effectiveFromMin.value
  editForm.effectiveTo = dateTimeInput(rule.effectiveTo)
  editDialog.value.showModal()
}

// 저장 중이 아닐 때 수정 Dialog를 닫고 입력값을 초기화한다.
const closeEditDialog = () => {
  if (feeRuleStore.saving) {
    return
  }

  editDialog.value.close()
  resetEditForm()
}

// 수정 Dialog가 닫히면 선택값과 입력값을 초기화한다.
const resetEditForm = () => {
  selectedFeeRule.value = null
  editForm.ruleName = ''
  editForm.unitMinutes = 30
  editForm.unitFee = 0
  editForm.dailyMaxFee = ''
  editForm.exitGraceMinutes = 30
  editForm.isDefault = false
  editForm.effectiveFrom = ''
  editForm.effectiveTo = ''
}

// 수정 Dialog의 바깥 영역을 클릭하면 닫는다.
const closeEditOnBackdrop = (event) => {
  if (event.target === editDialog.value) {
    closeEditDialog()
  }
}

// 수정값을 유지한 채 수정 Dialog 대신 오류 Dialog를 표시한다.
const showEditError = async (
  title,
  message
) => {
  editDialog.value.close()

  await alertDialog({
    theme: 'admin',
    type: 'error',
    title,
    message
  })

  if (editDialog.value && selectedFeeRule.value) {
    editDialog.value.showModal()
  }
}

// 등록값을 유지한 채 등록 Dialog 대신 오류 Dialog를 표시한다.
const showCreateError = async (
  title,
  message
) => {
  const dto = { ...form }

  createDialog.value.close()

  await alertDialog({
    theme: 'admin',
    type: 'error',
    title,
    message
  })

  Object.assign(form, dto)

  if (createDialog.value) {
    createDialog.value.showModal()
  }
}

// 입력한 요금 규칙을 등록한다.
const submitFeeRule = async () => {
  const unitMinutes = Number(form.unitMinutes)
  const unitFee = Number(form.unitFee)
  const exitGraceMinutes = Number(form.exitGraceMinutes)

  const dailyMaxFee =
    form.dailyMaxFee === ''
      ? null
      : Number(form.dailyMaxFee)

  if (!Number.isInteger(unitMinutes) || unitMinutes <= 0) {
    await showCreateError(
      '요금 규칙 입력 오류',
      '과금 단위는 1분 이상의 정수로 입력해 주세요.'
    )
    return
  }

  if (!Number.isFinite(unitFee) || unitFee < 0) {
    await showCreateError(
      '요금 규칙 입력 오류',
      '단위요금은 0원 이상으로 입력해 주세요.'
    )
    return
  }

  if (dailyMaxFee !== null
        && (!Number.isFinite(dailyMaxFee) || dailyMaxFee < 0)
  ) {
    await showCreateError(
      '요금 규칙 입력 오류',
      '일일 최대요금은 0원 이상으로 입력해 주세요.'
    )
    return
  }

  if (!Number.isInteger(exitGraceMinutes) || exitGraceMinutes < 0) {
    await showCreateError(
      '요금 규칙 입력 오류',
      '출차 유예시간은 0분 이상의 정수로 입력해 주세요.'
    )
    return
  }

  if (form.effectiveTo
        && new Date(form.effectiveTo) <= new Date(form.effectiveFrom)
  ) {
    await showCreateError(
      '요금 규칙 입력 오류',
      '적용 종료일시는 시작일시보다 뒤여야 합니다.'
    )
    return
  }

  const result = await feeRuleStore.addFeeRule({
    ruleName: form.ruleName.trim(),
    unitMinutes,
    unitFee,
    dailyMaxFee,
    exitGraceMinutes,
    isDefault: form.isDefault,
    effectiveFrom: form.effectiveFrom,
    effectiveTo: form.effectiveTo || null
  })

  if (!result.success) {
    await showCreateError(
      '요금 규칙 등록 실패',
      result.message
    )
    return
  }

  closeCreateDialog()

  await alertDialog({
    theme: 'admin',
    type: 'success',
    title: '요금 규칙 저장 완료',
    message: '요금 규칙을 저장했습니다.'
  })
}

// 예약 규칙을 수정하거나 활성 규칙의 새 버전을 등록한다.
const submitEditFeeRule = async () => {
  if (!selectedFeeRule.value) {
    return
  }

  const scheduled = scheduledEdit.value
  const unitMinutes = Number(editForm.unitMinutes)
  const unitFee = Number(editForm.unitFee)
  const exitGraceMinutes = Number(editForm.exitGraceMinutes)

  const dailyMaxFee =
    editForm.dailyMaxFee === ''
      ? null
      : Number(editForm.dailyMaxFee)

  if (!Number.isInteger(unitMinutes) || unitMinutes <= 0) {
    await showEditError(
      '요금 규칙 수정 오류',
      '과금 단위는 1분 이상의 정수로 입력해 주세요.'
    )
    return
  }

  if (!Number.isFinite(unitFee) || unitFee < 0) {
    await showEditError(
      '요금 규칙 수정 오류',
      '단위요금은 0원 이상으로 입력해 주세요.'
    )
    return
  }

  if (dailyMaxFee !== null
        && (!Number.isFinite(dailyMaxFee) || dailyMaxFee < 0)
  ) {
    await showEditError(
      '요금 규칙 수정 오류',
      '일일 최대요금은 0원 이상으로 입력해 주세요.'
    )
    return
  }

  if (!Number.isInteger(exitGraceMinutes) || exitGraceMinutes < 0) {
    await showEditError(
      '요금 규칙 수정 오류',
      '출차 유예시간은 0분 이상의 정수로 입력해 주세요.'
    )
    return
  }

  if (!editForm.effectiveFrom
        || new Date(editForm.effectiveFrom) <= new Date()
  ) {
    await showEditError(
      '요금 규칙 수정 오류',
      '적용 시작일시는 현재 시각보다 뒤여야 합니다.'
    )
    return
  }

  if (editForm.effectiveTo
        && new Date(editForm.effectiveTo)
        <= new Date(editForm.effectiveFrom)
  ) {
    await showEditError(
      '요금 규칙 수정 오류',
      '적용 종료일시는 시작일시보다 뒤여야 합니다.'
    )
    return
  }

  const result = await feeRuleStore.saveFeeRule(
    selectedFeeRule.value.feeRuleNo,
    {
      ruleName: editForm.ruleName.trim(),
      unitMinutes,
      unitFee,
      dailyMaxFee,
      exitGraceMinutes,
      isDefault: editForm.isDefault,
      effectiveFrom: editForm.effectiveFrom,
      effectiveTo: editForm.effectiveTo || null
    }
  )

  if (!result.success) {
    await showEditError(
      '요금 규칙 수정 실패',
      result.message
    )
    return
  }

  closeEditDialog()

  await alertDialog({
    theme: 'admin',
    type: 'success',
    title: '요금 규칙 수정 완료',
    message: scheduled
      ? '예약 요금 규칙을 수정했습니다.'
      : '새 버전의 요금 규칙을 등록했습니다.'
  })
}

// 정산 목록 화면으로 돌아간다.
const goBillingList = () => {
  router.push({
    name: 'AdminBillingList'
  })
}

// 화면 진입 시 목록을 조회하고 상태 표시 갱신을 시작한다.
onMounted(async () => {
  await feeRuleStore.loadFeeRuleList()

  statusTimer = window.setInterval(() => {
    currentTime.value = Date.now()
  }, 10000)
})

// 화면을 벗어나면 상태 표시 갱신을 중지한다.
onUnmounted(() => {
  if (statusTimer !== null) {
    window.clearInterval(statusTimer)
    statusTimer = null
  }
})
</script>

<style scoped>
.fee-rule-page {
  width: 100%;
}

.fee-rule-header,
.header-actions,
.dialog-actions {
  display: flex;
  align-items: center;
}

.fee-rule-header {
  justify-content: space-between;
  gap: 16px;
}

.header-actions,
.dialog-actions {
  gap: 8px;
}

.primary-button,
.secondary-button {
  min-height: 38px;
  padding: 8px 16px;
  border: 1px solid #69737b;
  border-radius: 0;
  color: #f1f3f5;
  background: #343a40;
  font: inherit;
  font-weight: 700;
  cursor: pointer;
}

.primary-button {
  border-color: #d4b83f;
  color: #1f2428;
  background: #d4b83f;
}

.primary-button:disabled,
.secondary-button:disabled,
.dialog-close:disabled {
  cursor: not-allowed;
  opacity: 0.55;
}

.rule-edit-button {
  min-width: 88px;
  min-height: 32px;
  padding: 5px 10px;
  font-size: 12px;
  white-space: nowrap;
}

.rule-edit-button:not(:disabled):hover {
  border-color: #d4b83f;
  color: #d4b83f;
}

.rule-section {
  margin-top: 22px;
}

.rule-section h3 {
  margin: 0 0 10px;
  color: #f1f3f5;
  font-size: 17px;
}

.table-wrap {
  width: 100%;
  overflow-x: auto;
}

.table-wrap table {
  width: 100% !important;
  min-width: 980px;
  border-collapse: collapse;
}

.table-wrap th,
.table-wrap td {
  padding: 12px 10px;
  border: 1px solid #505960;
  text-align: center;
  white-space: nowrap;
}

.table-wrap th {
  color: #e8ecef;
  background: #41484e;
}

.table-wrap td {
  color: #eef1f3;
  background: #2b3035;
}

.page-state,
.empty-cell {
  padding: 36px;
  text-align: center;
}

.page-state.error {
  color: #ef7777;
}

.status-badge {
  display: inline-flex;
  min-width: 58px;
  justify-content: center;
  padding: 4px 9px;
  border: 1px solid transparent;
  border-radius: 999px;
  font-size: 12px;
  font-weight: 800;
}

.status-badge.reserved {
  border-color: #6c88a0;
  color: #c9e6ff;
  background: #344b5e;
}

.status-badge.using {
  border-color: #708b65;
  color: #d9f2cf;
  background: #3d5236;
}

.status-badge.ended {
  border-color: #666d73;
  color: #c9ced2;
  background: #41464a;
}

.fee-rule-dialog {
  box-sizing: border-box;
  width: min(560px, calc(100vw - 32px));
  max-height: calc(100vh - 32px) !important;
  margin: auto;
  padding: 28px;
  border: 1px solid #69737b;
  color: #f1f3f5;
  background: #2b3035;
  box-shadow: 0 24px 70px rgba(0, 0, 0, 0.48);
  overflow-x: hidden;
  overflow-y: auto !important;
}

.fee-rule-dialog::backdrop {
  background: rgba(13, 17, 20, 0.72);
  backdrop-filter: blur(2px);
}

.fee-rule-dialog h3 {
  margin: 0 0 24px;
  font-size: 21px;
}

.dialog-close {
  position: absolute;
  top: 12px;
  right: 14px;
  padding: 4px 8px;
  border: 0;
  color: #c9ced2;
  background: transparent;
  font-size: 24px;
  cursor: pointer;
}

.fee-rule-dialog form {
  display: grid;
  gap: 16px;
}

.fee-rule-dialog label {
  display: grid;
  gap: 7px;
  color: #d9dde0;
  font-weight: 700;
}

.fee-rule-dialog .checkbox-label {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
}

.fee-rule-dialog .checkbox-label input {
  width: 18px;
  height: 18px;
  min-height: 18px;
  margin: 0;
}

.fee-rule-dialog input {
  box-sizing: border-box;
  width: 100%;
  min-height: 42px;
  padding: 9px 11px;
  border: 1px solid #69737b;
  border-radius: 0;
  color: #f1f3f5;
  background: #24292e;
  font: inherit;
}

.fee-rule-dialog input:focus {
  border-color: #d4b83f;
  outline: 1px solid #d4b83f;
}

.fee-rule-dialog input:disabled {
  color: #aeb6bc;
  background: #343a40;
  cursor: not-allowed;
  opacity: 1;
}

.dialog-actions {
  justify-content: flex-end;
  margin-top: 8px;
}

@media (max-width: 760px) {
  .fee-rule-header {
    align-items: stretch;
    flex-direction: column;
  }

  .header-actions {
    justify-content: flex-end;
  }

  .fee-rule-dialog {
    padding: 24px 18px 18px;
  }
}

</style>
