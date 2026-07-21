<template>
  <div>
    <h3>방문차량 신청</h3>

    <form @submit.prevent="submit">
      <table border="">
        <tbody>
          <tr>
            <th>차량번호</th>
            <td>
              <input
                v-model="form.carNo"
                placeholder="예: 12가3456"
                required
              >
            </td>
          </tr>

          <tr>
            <th>차량종류</th>
            <td>방문차량</td>
          </tr>

          <tr>
            <th>방문 시작</th>
            <td>
              <div class="visit-date-fields">
                <input
                  v-model="form.visitDate"
                  type="date"
                  :min="minimumVisitDate"
                  required
                  @change="clearInvalidTime"
                >

                <select
                  v-model="form.visitHour"
                  required
                  @change="clearInvalidMinute"
                >
                  <option value="">시 선택</option>
                  <option
                    v-for="hour in hourOptions"
                    :key="hour"
                    :value="String(hour)"
                    :disabled="isHourDisabled(hour)"
                  >
                    {{ pad(hour) }}시
                  </option>
                </select>

                <select v-model="form.visitMinute" required>
                  <option value="">분 선택</option>
                  <option
                    v-for="minute in minuteOptions"
                    :key="minute"
                    :value="String(minute)"
                    :disabled="isMinuteDisabled(minute)"
                  >
                    {{ pad(minute) }}분
                  </option>
                </select>
              </div>

              <small>
                현재시간으로부터 1시간 이후부터 신청할 수 있습니다.
              </small>
            </td>
          </tr>

          <tr>
            <th>방문 시간</th>
            <td>
              <select v-model.number="form.periodHours" required>
                <option :value="2">2시간</option>
                <option :value="4">4시간</option>
                <option :value="6">6시간</option>
                <option :value="8">8시간</option>
                <option :value="12">12시간</option>
              </select>
            </td>
          </tr>

          <tr>
            <th>예상 만료</th>
            <td>{{ expectedEndText || '-' }}</td>
          </tr>

          <tr>
            <td colspan="2" align="right">
              <button type="submit">신청</button>
              <button type="button" @click="emit('cancel')">
                취소
              </button>
            </td>
          </tr>
        </tbody>
      </table>
    </form>
  </div>
</template>

<script setup>
import {
  computed,
  onBeforeUnmount,
  onMounted,
  reactive,
  ref
} from 'vue'

const emit = defineEmits(['submit', 'cancel'])

const carNoPattern = /^([가-힣]{2})?\d{2,3}[가-힣]\d{4}$/

const form = reactive({
  carNo: '',
  visitDate: '',
  visitHour: '',
  visitMinute: '',
  periodHours: 2
})

const currentTime = ref(new Date())

const hourOptions = Array.from(
  { length: 24 },
  (_, index) => index
)

const minuteOptions = Array.from(
  { length: 60 },
  (_, index) => index
)

let currentTimeTimer = null

const minimumStartDate = computed(() => {
  return new Date(
    currentTime.value.getTime() + (60 * 60 * 1000)
  )
})

const minimumVisitDate = computed(() => {
  return formatDateValue(minimumStartDate.value)
})

const selectedStartDate = computed(() => {
  if (
    !form.visitDate
    || form.visitHour === ''
    || form.visitMinute === ''
  ) {
    return null
  }

  return makeLocalDate(
    form.visitDate,
    Number(form.visitHour),
    Number(form.visitMinute)
  )
})

const expectedEndText = computed(() => {
  if (!selectedStartDate.value) {
    return ''
  }

  return formatDateTimeText(
    makeEndDate(selectedStartDate.value)
  )
})

onMounted(() => {
  currentTimeTimer = window.setInterval(() => {
    currentTime.value = new Date()
    clearInvalidTime()
  }, 30000)
})

onBeforeUnmount(() => {
  window.clearInterval(currentTimeTimer)
})

function submit() {
  const normalizedCarNo = form.carNo
    .trim()
    .replace(/\s/g, '')

  if (!carNoPattern.test(normalizedCarNo)) {
    alert(
      '차량번호 형식이 올바르지 않습니다. 예: 12가3456, 서울12가3456'
    )
    return
  }

  currentTime.value = new Date()

  const startDate = selectedStartDate.value

  if (!startDate) {
    alert('방문 날짜와 시간을 선택하세요.')
    return
  }

  if (startDate < minimumStartDate.value) {
    alert(
      '방문 시작은 현재시간으로부터 1시간 이후로 선택해야 합니다.'
    )
    clearInvalidTime()
    return
  }

  const endDate = makeEndDate(startDate)

  emit('submit', {
    carNo: normalizedCarNo,
    startDate: formatDateTimeLocalValue(startDate),
    endDate: formatDateTimeLocalValue(endDate)
  })
}

function isHourDisabled(hour) {
  if (!form.visitDate) {
    return false
  }

  const endOfHour = makeLocalDate(
    form.visitDate,
    hour,
    59
  )

  return endOfHour < minimumStartDate.value
}

function isMinuteDisabled(minute) {
  if (!form.visitDate || form.visitHour === '') {
    return false
  }

  const selectedDate = makeLocalDate(
    form.visitDate,
    Number(form.visitHour),
    minute
  )

  return selectedDate < minimumStartDate.value
}

function clearInvalidTime() {
  if (
    form.visitHour !== ''
    && isHourDisabled(Number(form.visitHour))
  ) {
    form.visitHour = ''
    form.visitMinute = ''
    return
  }

  clearInvalidMinute()
}

function clearInvalidMinute() {
  if (
    form.visitMinute !== ''
    && isMinuteDisabled(Number(form.visitMinute))
  ) {
    form.visitMinute = ''
  }
}

function makeLocalDate(dateValue, hour, minute) {
  const [year, month, day] = dateValue
    .split('-')
    .map(Number)

  return new Date(
    year,
    month - 1,
    day,
    hour,
    minute,
    0,
    0
  )
}

function makeEndDate(startDate) {
  const endDate = new Date(startDate)

  endDate.setHours(
    endDate.getHours() + Number(form.periodHours)
  )

  return endDate
}

function formatDateValue(date) {
  const year = date.getFullYear()
  const month = pad(date.getMonth() + 1)
  const day = pad(date.getDate())

  return `${year}-${month}-${day}`
}

function formatDateTimeLocalValue(date) {
  const yyyy = date.getFullYear()
  const mm = pad(date.getMonth() + 1)
  const dd = pad(date.getDate())
  const hh = pad(date.getHours())
  const mi = pad(date.getMinutes())

  return `${yyyy}-${mm}-${dd}T${hh}:${mi}`
}

function formatDateTimeText(date) {
  const yyyy = date.getFullYear()
  const mm = pad(date.getMonth() + 1)
  const dd = pad(date.getDate())
  const hh = pad(date.getHours())
  const mi = pad(date.getMinutes())

  return `${yyyy}-${mm}-${dd} ${hh}:${mi}`
}

function pad(value) {
  return String(value).padStart(2, '0')
}
</script>

<style scoped>
.visit-date-fields {
  display: grid;
  grid-template-columns: minmax(150px, 1fr) 100px 100px;
  gap: 8px;
  margin-bottom: 6px;
}

@media (max-width: 600px) {
  .visit-date-fields {
    grid-template-columns: 1fr;
  }
}
</style>