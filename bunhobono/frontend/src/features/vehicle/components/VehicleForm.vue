<template>
  <div>
    <h3>차량 등록</h3>

    <div>
      <input
        v-model="carNo"
        placeholder="차량번호 예: 12가3456"
      >

      <select v-model="vehicleType">
        <option value="normal">등록차량</option>
        <option value="visit">방문차량</option>
      </select>

      <select v-model="selectedDong">
        <option value="">동 선택</option>
        <option
          v-for="dong in dongOptions"
          :key="dong"
          :value="dong"
        >
          {{ dongText(dong) }}
        </option>
      </select>

      <select v-model="selectedHo">
        <option value="">호수 선택</option>
        <option
          v-for="ho in hoOptions"
          :key="ho"
          :value="ho"
        >
          {{ hoText(ho) }}
        </option>
      </select>

      <select v-model.number="memberNo">
        <option :value="null">입주민 선택</option>
        <option
          v-for="member in filteredMembers"
          :key="member.memberNo"
          :value="member.memberNo"
        >
          {{ memberLabel(member) }}
        </option>
      </select>

      <button @click="add">등록</button>
      <button @click="reset">초기화</button>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { useMemStore } from '../../member/memStore'
import { useVehicleStore } from '../vehicleStore'

const vehicleStore = useVehicleStore()
const memberStore = useMemStore()

const carNo = ref('')
const vehicleType = ref('normal')
const selectedDong = ref('')
const selectedHo = ref('')
const memberNo = ref(null)

const carNoPattern = /^([가-힣]{2})?\d{2,3}[가-힣]\d{4}$/

const filteredMembers = computed(() => {
  return memberStore.memberList.filter((member) => {
    const dongMatched = !selectedDong.value
      || String(member.memDong ?? '') === selectedDong.value

    const hoMatched = !selectedHo.value
      || String(member.memHo ?? '') === selectedHo.value

    const isSelectableMember =
      member.memStatus === 'APPROVED'
      || member.role === 'resident'
      || member.role === 'RESIDENT'
      || member.role === 'admin'
      || member.role === 'ADMIN'

    return dongMatched && hoMatched && isSelectableMember
  })
})

const dongOptions = computed(() => {
  return [...new Set(
    memberStore.memberList
      .map((member) => member.memDong)
      .filter((dong) => dong !== null && dong !== undefined && dong !== '')
      .map((dong) => String(dong))
  )].sort((a, b) => Number(a) - Number(b))
})

const hoOptions = computed(() => {
  return [...new Set(
    memberStore.memberList
      .filter((member) => {
        return !selectedDong.value
          || String(member.memDong ?? '') === selectedDong.value
      })
      .map((member) => member.memHo)
      .filter((ho) => ho !== null && ho !== undefined && ho !== '')
      .map((ho) => String(ho))
  )].sort((a, b) => Number(a) - Number(b))
})

watch(selectedDong, () => {
  selectedHo.value = ''
  memberNo.value = null
})

watch(selectedHo, () => {
  memberNo.value = null
})

onMounted(async () => {
  if (memberStore.memberList.length === 0) {
    await memberStore.loadmemberList()
  }
})

async function add() {
  const normalizedCarNo = carNo.value.trim().replace(/\s/g, '')

  if (normalizedCarNo === '') {
    alert('차량번호를 입력하세요')
    return
  }

  if (!carNoPattern.test(normalizedCarNo)) {
    alert('차량번호 형식이 올바르지 않습니다. 예: 12가3456, 서울12가3456')
    return
  }

  if (!memberNo.value) {
    alert('입주민을 선택하세요')
    return
  }

  try {
    await vehicleStore.addVehicle({
      carNo: normalizedCarNo,
      vehicleType: vehicleType.value,
      vehicleStatus: 'WAITING',
      memberNo: memberNo.value
    })

    alert('차량이 등록되었습니다')
    reset()
    await vehicleStore.loadVehicleApproveList()
    await vehicleStore.loadVehicleList()
  } catch (error) {
    if (error.response?.status === 409) {
      alert('이미 등록 또는 승인 대기 중인 차량번호입니다.')
      reset()
      return
    }

    alert('차량 등록 중 오류가 발생했습니다.')
    reset()
  }
}

function reset() {
  carNo.value = ''
  vehicleType.value = 'normal'
  selectedDong.value = ''
  selectedHo.value = ''
  memberNo.value = null
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

function memberLabel(member) {
  const dong = dongText(member.memDong)
  const ho = hoText(member.memHo)

  return `${dong} ${ho} / ${member.memName}`
}
</script>