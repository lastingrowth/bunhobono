<template>
  <div>
    <h3>차량 등록</h3>

    <div>
      <input
        v-model="carNo"
        placeholder="차량번호"
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
          {{ dong }}동
        </option>
      </select>

      <select v-model="selectedHo">
        <option value="">호수 선택</option>
        <option
          v-for="ho in hoOptions"
          :key="ho"
          :value="ho"
        >
          {{ ho }}호
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
})

onMounted(async () => {
  if (memberStore.memberList.length === 0) {
    await memberStore.loadmemberList()
  }
})

async function add() {
  if (carNo.value.trim() === '') {
    alert('차량번호를 입력하세요')
    return
  }

  try {
    await vehicleStore.addVehicle({
      carNo: carNo.value.trim(),
      vehicleType: vehicleType.value,
      vehicleStatus: 'WAITING',
    })

    alert('차량이 등록되었습니다')
    reset()
    await vehicleStore.loadVehicleApproveList()
  } catch (error) {
    if (error.response?.status === 409) {
      alert(error.response.data?.message || '이미 등록 또는 승인 대기 중인 차량번호입니다.')
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

}
</script>
