<!-- 이전 / 숫자 / 다음 버튼 화면 -->
<template>
    <div class="pagination">
        <button
            type="button"
            :disabled="pageNumbers[0] === 1"
            @click="$emit('change-page', pageNumbers[0] - 1)">
            이전
        </button>

        <button
            v-for="page in pageNumbers"
            :key="page"
            type="button"
            :class="{ active : currentPage === page}"
            @click="$emit('change-page', page)">
            {{ page }}
        </button>

        <button
            type="button"
            :disabled="pageNumbers[pageNumbers.length -1] === totalPages"
            @click="$emit('change-page', pageNumbers[pageNumbers.length - 1] + 1)">
            다음
        </button>

        <span class="page-status" aria-live="polite">
            {{ currentPage }} / {{ totalPages }} 페이지
        </span>
    </div>
</template>

<script setup>
// 부모 화면에서 현재 페이지와 전체 페이지 정보를 받아온다
defineProps({
    currentPage : {
        type : Number,
        required : true
    },
    totalPages : {
        type : Number,
        required : true
    },
    pageNumbers : {
        type : Array,
        required : true
    }
})

// 페이지 버튼을 눌렀을 때 부모 화면으로 page 번호를 알려준다
defineEmits([
    'change-page'
])
</script>

<style scoped>
.pagination {
  margin-top: 16px;
  display: flex;
  justify-content: center;
  align-items: center;
  gap: 6px;
}

.pagination button {
  min-width: 34px;
  height: 32px;
  padding: 0 10px;
  border: 1px solid #d7deea;
  border-radius: 8px;
  cursor: pointer;
  color: #253047;
  background: #fff;
  font-size: 13px;
  font-weight: 700;
}

.pagination button:hover:not(:disabled) {
  border-color: #2f80ed;
  color: #2f80ed;
}

.pagination button.active {
  border-color: #2f80ed;
  color: #fff;
  background: #2f80ed;
}

.pagination button:disabled {
  cursor: default;
  opacity: 0.45;
}

.page-status {
  min-width: 86px;
  margin-left: 8px;
  color: #64748b;
  font-size: 13px;
  font-weight: 700;
  text-align: center;
  white-space: nowrap;
}
</style>
