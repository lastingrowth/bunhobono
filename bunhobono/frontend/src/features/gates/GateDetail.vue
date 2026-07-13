<template>
  <section v-if="gateStore.detail" class="info-detail-card">
    <div class="info-detail-header">
      <div>
        <span class="info-detail-category">GATE INFORMATION</span>
        <h2>게이트 상세</h2>
      </div>
    </div>

    <div class="info-detail-highlight">
      <span>게이트 이름</span>
      <strong>{{ gateStore.detail.gateName || '-' }}</strong>
    </div>

    <dl class="info-detail-list">
      <div>
        <dt>게이트 번호</dt>
        <dd>{{ gateStore.detail.gateNo ?? '-' }}</dd>
      </div>

      <div>
        <dt>게이트 타입</dt>
        <dd>{{ gateStore.detail.gateType || '-' }}</dd>
      </div>

      <div>
        <dt>주차장 이름</dt>
        <dd>{{ gateStore.detail.parkingName || '-' }}</dd>
      </div>

      <div>
        <dt>주차장 위치</dt>
        <dd>{{ gateStore.detail.parkingLocation || '-' }}</dd>
      </div>
    </dl>

    <div class="info-detail-actions">
      <button @click="router.back()">뒤로가기</button>
    </div>
  </section>

  <div v-else>
    <p>게이트 정보를 불러오는 중...</p>
  </div>
</template>

<script setup>
import { onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useGateStore } from './gateStore'

const route = useRoute()
const router = useRouter()
const gateStore = useGateStore()

onMounted(() => {
  gateStore.loadDetail(route.params.gateNo)
})
</script>