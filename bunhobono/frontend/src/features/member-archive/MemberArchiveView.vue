<template>
    <main>
        <h2>전출 회원 관리</h2>

        <p>
            전출 확정 처리된 회원 이력을 확인하고, 필요 없는 이력은 삭제할 수 있습니다.
        </p>

        <p v-if="archiveStore.loading">
            전출 회원 이력을 불러오는 중입니다.
        </p>

        <p v-if="archiveStore.errorMessage">
            {{ archiveStore.errorMessage }}
        </p>

        <div class="admin-table-scroll">
        <table border="">
            <thead>
                <tr>
                    <th>번호</th>
                    <th>이름</th>
                    <th>아이디</th>
                    <th>동</th>
                    <th>호수</th>
                    <th>연락처</th>
                    <th>권한</th>
                    <th>전출일</th>
                    <th>보관일</th>
                    <th>관리</th>
                </tr>
            </thead>

            <tbody>
                <tr
                    v-for="(member, index) in paginatedArchives"
                    :key="member.archiveNo">
                    <td>{{ (currentPage - 1) * pageSize + index + 1 }}</td>
                    <td>{{ member.memName || '-' }}</td>
                    <td>{{ member.loginId || '-' }}</td>
                    <td>{{ member.memDong || '-' }}</td>
                    <td>{{ member.memHo || '-' }}</td>
                    <td>{{ member.memPhone || '-' }}</td>
                    <td>{{ member.role || '-' }}</td>
                    <td>{{ formatDate(member.deleteAt) }}</td>
                    <td>{{ formatDate(member.archivedAt) }}</td>
                    <td><button type="button" @click="remove(member.archiveNo)">이력 삭제</button></td>
                </tr>

                <tr v-if="list.length === 0">
                    <td colspan="10">보관된 전출 회원 이력이 없습니다.</td>
                </tr>
            </tbody>
        </table>
        </div>
        <div class="admin-pagination-area">
        <Pagination
            :current-page="currentPage"
            :total-pages="totalPages"
            :page-numbers="pageNumbers"
            @change-page="setPage" />
        </div>
    </main>
</template>

<script setup>
import { onMounted } from 'vue'
import { storeToRefs } from 'pinia'
import { useMemberArchiveStore } from './memberArchiveStore'
import { usePagination } from '@/shared/pagination/usePagination'
import Pagination from '@/shared/pagination/Pagination.vue'

const archiveStore = useMemberArchiveStore()

const { list } = storeToRefs(archiveStore)

const pageSize = 10

const {
    currentPage,
    totalPages,
    pageNumbers,
    paginatedItems: paginatedArchives,
    setPage,
} = usePagination(list, pageSize)

const formatDate = (value) => {
    if (!value) {
        return '-'
    }

    const date = new Date(value)

    if (Number.isNaN(date.getTime())) {
        return value
    }

    return date.toLocaleString('ko-KR')
}

const remove = async (archiveNo) => {
    if (!confirm('선택한 전출 회원 이력을 삭제하시겠습니까?')) {
        return
    }

    await archiveStore.remove(archiveNo)
}

onMounted(async () => {
    await archiveStore.loadList()
})
</script>
