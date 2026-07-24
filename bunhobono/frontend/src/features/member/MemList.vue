<template>
  <main class="member-list-page management-list-page">
    <div class="list-header management-list-header">
        <h2 class="management-list-title">회원 관리</h2>
        <div class="member-header-actions">
            <nav class="member-management-tabs" aria-label="회원관리 메뉴">
                <button
                    v-for="section in visibleManagementSections"
                    :key="section.value"
                    type="button"
                    :class="{ active: activeSection === section.value }"
                    @click="changeSection(section.value)"
                >
                    {{ section.label }}
                </button>
            </nav>
            <button type="button" @click="router.push('/admin/signup')">회원 추가</button>
        </div>
    </div>

    <template v-if="activeSection === 'pending'">
    <!-- 승인 대기 회원을 선택해 입주민 역할로 변경한다. -->
    <div class="approval-actions">
        <button type="button" @click="toggleSelectAll">{{ allVisibleSelected ? '전체해제' : '전체선택' }}</button>
        <button type="button" @click="approveSelectedMembers">승인</button>
        <span>선택 {{ selectedMemberNos.length }}명</span>
    </div>

    <!-- 승인 대기 회원에 대해서 분리해서 확인. -->
    <section class="pending-section">
        <h3>승인 대기 회원 ({{ pendingMembers.length }}명)</h3>
        <div class="admin-table-scroll">
        <table class="member-list-table" border="">
            <thead>
                <tr>
                    <th>선택</th><th>가입유형</th><th>이름</th><th>동</th><th>호수</th>
                    <th>연락처</th><th>아이디</th>
                </tr>
            </thead>
            <tbody>
                <tr v-for="mem in paginatedMembers" :key="mem.memberNo">
                    <td><input v-model="selectedMemberNos" type="checkbox" :value="mem.memberNo"></td>
                    <td>{{ mem.role }}</td>
                    <td><router-link :to="`/admin/members/${mem.memberNo}/detail`">{{ mem.memName }}</router-link></td>
                    <td>{{ mem.memDong }}</td><td>{{ mem.memHo }}</td>
                    <td>{{ mem.memPhone }}</td><td>{{ mem.loginId }}</td>
                </tr>
                <tr v-if="pendingMembers.length === 0"><td colspan="7">승인 대기 회원이 없습니다.</td></tr>
            </tbody>
        </table>
        </div>
        <div class="admin-pagination-area">
        <pagination
            :current-page="currentPage"
            :total-pages="totalPages"
            :page-numbers="pageNumbers"
            @change-page="setPage"/>
        </div>
    </section>
    </template>

    <template v-if="activeSection === 'withdrawn'">
        <div class="approval-actions">
            <button type="button" @click="selectAllWithdrawnMembers">
                {{ allWithdrawnSelected ? '전체해제' : '전체선택' }}
            </button>
            <button type="button" @click="restoreSelectedWithdrawnMembers">복원</button>
            <button type="button" @click="permanentlyDeleteSelectedWithdrawnMembers">전출 확정</button>
            <span>선택 {{ selectedWithdrawnMemberNos.length }}명</span>
        </div>

        <!-- 전출 신청 상태의 회원을 확인하고 복원 또는 전출 확정 처리한다. -->
        <section class="archive-alert-section">
            <h3>전출 신청 회원 목록 ({{ withdrawnMembers.length }}명)</h3>
            <div class="admin-table-scroll">
            <table class="member-list-table" border="">
                <thead>
                    <tr>
                        <th>선택</th>
                        <th>가입유형</th>
                        <th>이름</th>
                        <th>동</th>
                        <th>호수</th>
                        <th>아이디</th>
                        <th>상태</th>
                        <th>전출 신청일</th>
                        <th>경과일</th>
                    </tr>
                </thead>
                <tbody>
                    <tr v-for="mem in paginatedMembers" :key="mem.memberNo">
                        <td>
                            <input
                                type="checkbox"
                                :checked="selectedWithdrawnMemberNos.includes(mem.memberNo)"
                                @change="toggleWithdrawnMember(mem.memberNo)">
                        </td>
                        <td>{{ mem.role }}</td>
                        <td>
                            <router-link :to="`/admin/members/${mem.memberNo}/detail`">
                                {{ mem.memName }}
                            </router-link>
                        </td>
                        <td>{{ mem.memDong }}</td>
                        <td>{{ mem.memHo }}</td>
                        <td>{{ mem.loginId }}</td>
                        <td>{{ formatMemberStatus(mem.memStatus, mem.role) }}</td>
                        <td>{{ store.formatMemberDateTime(mem.memDeleteAt) }}</td>
                        <td>{{ getElapsedDays(mem.memDeleteAt) }}일</td>
                    </tr>

                    <tr v-if="withdrawnMembers.length === 0">
                        <td colspan="9">전출 신청 회원이 없습니다.</td>
                    </tr>
                </tbody>
            </table>
            </div>
            <div class="admin-pagination-area">
            <pagination
                :current-page="currentPage"
                :total-pages="totalPages"
                :page-numbers="pageNumbers"
                @change-page="setPage"/>
            </div>
        </section>
    </template>

    <template v-if="activeSection === 'archive'">
        <!-- 전출 확정 후 member_archive에 보관된 과거 거주 이력을 조회한다. -->
        <section class="archive-alert-section">
            <h3>전출 이력 ({{ archiveList.length }}명)</h3>

            <div class="admin-table-scroll">
                <table class="member-list-table" border="">
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
                        </tr>
                    </thead>

                    <tbody>
                        <tr
                            v-for="(member, index) in paginatedMembers"
                            :key="member.archiveNo">
                            <td>{{ (currentPage - 1) * pageSize + index + 1 }}</td>
                            <td>{{ member.memName || '-' }}</td>
                            <td>{{ member.loginId || '-' }}</td>
                            <td>{{ member.memDong || '-' }}</td>
                            <td>{{ member.memHo || '-' }}</td>
                            <td>{{ member.memPhone || '-' }}</td>
                            <td>{{ member.role || '-' }}</td>
                            <td>{{ store.formatMemberDateTime(member.deleteAt) }}</td>
                            <td>{{ store.formatMemberDateTime(member.archivedAt) }}</td>
                        </tr>

                        <tr v-if="archiveList.length === 0">
                            <td colspan="9">전출 이력이 없습니다.</td>
                        </tr>
                    </tbody>
                </table>
            </div>

            <div class="admin-pagination-area">
                <pagination
                    :current-page="currentPage"
                    :total-pages="totalPages"
                    :page-numbers="pageNumbers"
                    @change-page="setPage"/>
            </div>
        </section>
    </template>

    <template v-if="activeSection === 'approved'">
    <div class="approved-list-header">
       
        <div class="member-search">
            <select v-model="roleFilter" @change="handleMemberRoleFilter">
                <option value="">유형 전체</option>
                <option value="ADMIN">관리자</option>
                <option value="RESIDENT">입주민</option>
            </select>
            <select v-model="dong" @change="handleMemberDongFilter">
                <option value="">동 전체</option>
                <option v-for="dongOption in dongOptions" :key="dongOption" :value="dongOption">{{ dongOption }}동</option>
            </select>
            <select v-model="ho" :disabled="dong === ''" @change="handleMemberHoFilter">
                <option value="">호 전체</option>
                <option v-for="hoOption in hoOptions" :key="hoOption" :value="hoOption">{{ hoOption }}호</option>
            </select>
            <input v-model="searchKeyword" type="search" placeholder="이름 검색" @keyup.enter="searchGo" />
            <button type="button" @click="searchGo">검색</button>
            <button type="button" @click="resetMemberSearch">초기화</button>
        </div>
    </div>

    <div class="admin-table-scroll management-list-table member-table-wrap">
    <table class="member-list-table" border="">
        <colgroup>
            <col class="member-number-col">
            <col class="member-role-col">
            <col class="member-name-col">
            <col class="member-dong-col">
            <col class="member-ho-col">
            <col class="member-phone-col">
            <col class="member-id-col">
            <col class="member-created-col">
            <col class="member-status-col">
        </colgroup>
        <thead>
            <tr>
                <th>번호</th><th>가입유형</th><th>이름</th><th>동</th><th>호수</th><th>연락처</th>
                <th>아이디</th><th>가입일</th><th>상태</th>
            </tr>
        </thead>
        <tbody>
            <tr v-for="mem in paginatedMembers" :key="mem.memberNo">
                <td>{{ mem.displayNo }}</td>
                <td>{{ mem.role }}</td>
                <td><router-link :to="`/admin/members/${mem.memberNo}/detail`">{{ mem.memName }}</router-link></td>
                <td>{{ mem.memDong }}</td><td>{{ mem.memHo }}</td><td>{{ mem.memPhone }}</td>
                <td>{{ mem.loginId }}</td><td>{{ store.formatMemberDateTime(mem.memCreateAt) }}</td>
                <td>{{ formatMemberStatus(mem.memStatus, mem.role) }}</td>
            </tr>
        </tbody>
    </table>
    </div>
    <div class="admin-pagination-area">
    <pagination
            :current-page="currentPage"
            :total-pages="totalPages"
            :page-numbers="pageNumbers"
            @change-page="setPage"/>
    </div>
    </template>
  </main>
</template>

<script setup>
import { computed, onMounted, onUnmounted, ref } from 'vue';
import { useMemStore } from './memStore';
import { storeToRefs } from 'pinia';
import { useRoute, useRouter } from 'vue-router';
import { usePagination } from '@/shared/pagination/usePagination';
import Pagination from '@/shared/pagination/Pagination.vue';
import { useMemberArchiveStore } from '../member-archive/memberArchiveStore';

const store = useMemStore();
const archiveStore = useMemberArchiveStore()
const route = useRoute();
const router = useRouter();
const { memberList } = storeToRefs(store);
const { list: archiveList } = storeToRefs(archiveStore)
const searchKeyword = ref('');
const roleFilter = ref('');
const dong = ref('');
const ho = ref('');
const appliedMemberFilters = ref({ name: '', role: '', dong: '', ho: '' });
const selectedMemberNos = ref([]);
const selectedWithdrawnMemberNos = ref([]);
const currentTime = ref(Date.now());
const pageSize = 10;
let elapsedCheckTimer;
const managementSections = [
    { value: 'approved', label: '입주민 목록' },
    { value: 'pending', label: '가입 승인 대기' },
    { value: 'withdrawn', label: '전출 신청 관리' },
    { value: 'archive', label: '전출 이력'}
];
const visibleManagementSections = computed(() => managementSections.filter(
    (section) => section.value !== 'approved'
));
const requestedSection = String(route.query.section || 'approved');
const activeSection = ref(
    managementSections.some((section) => section.value === requestedSection)
        ? requestedSection
        : 'approved'
);

const toDateTime = (value) => {
    if (!value) {
        return 0;
    }

    if (Array.isArray(value)) {
        const [year, month, day, hour = 0, minute = 0, second = 0] = value;
        return new Date(year, month - 1, day, hour, minute, second).getTime();
    }

    return new Date(value).getTime();
};

// 서버 상태값은 영어로 받고, 화면에는 사용자가 이해하기 쉬운 한글로 표시한다.
const formatMemberStatus = (status, role) => {
    if (status === 'ACTIVE') {
        return role === 'ADMIN' ? '근무' : '거주'
    }

    const statusMap = {
        WITHDRAW_PENDING: '전출 신청',
        EMPTY: '빈 세대',
        INACTIVE: '퇴사',
        ON_LEAVE: '휴직',
    }

    return statusMap[status] ?? status ?? '-'
}

// PENDING 역할 회원만 승인 대기 목록으로 분류한다.
const pendingMembers = computed(() => memberList.value.filter(
    (member) => String(member.role || '').toUpperCase() === 'PENDING'
));

// ADMIN과 RESIDENT 역할 회원만 승인된 회원 목록에 표시한다.
const approvedMembers = computed(() => memberList.value
    .filter((member) =>
        ['ADMIN', 'RESIDENT'].includes(String(member.role || '').toUpperCase())
        && !member.memDeleteAt
    )
    .map((member, index) => ({
        ...member,
        displayNo: index + 1
    }))
);

const filteredApprovedMembers = computed(() => {
    const filters = appliedMemberFilters.value;
    const normalizedName = filters.name.trim().toLowerCase();

    return approvedMembers.value.filter((member) => {
        const nameMatches = !normalizedName
            || String(member.memName || '').toLowerCase().includes(normalizedName);
        const roleMatches = !filters.role || String(member.role || '') === filters.role;
        const dongMatches = !filters.dong || Number(member.memDong) === Number(filters.dong);
        const hoMatches = !filters.ho || Number(member.memHo) === Number(filters.ho);

        return nameMatches && roleMatches && dongMatches && hoMatches;
    });
});

// 조회된 회원의 실제 동·호수 값으로 검색 선택지를 구성한다.
const dongOptions = computed(() => [...new Set(
    approvedMembers.value.map((member) => Number(member.memDong))
)]
    .filter(Number.isFinite)
    .sort((left, right) => left - right));

const hoOptions = computed(() => [...new Set(
    approvedMembers.value
        .filter((member) => Number(member.memDong) === Number(dong.value))
        .map((member) => Number(member.memHo))
)]
    .filter(Number.isFinite)
    .sort((left, right) => left - right));

// 전출 신청 관리는 아직 member 테이블에 남아 있고,
// 관리자 확정을 기다리는 WITHDRAW_PENDING 회원만 보여준다.
const withdrawnMembers = computed(() => memberList.value
    .filter((member) => {
        return member.memStatus === 'WITHDRAW_PENDING'
            && member.memName !== '미등록'
    })
    .sort((left, right) => toDateTime(right.memDeleteAt) - toDateTime(left.memDeleteAt))
)

const activeMembers = computed(() => {
    if (activeSection.value === 'pending') return pendingMembers.value
    if (activeSection.value === 'withdrawn') return withdrawnMembers.value
    if (activeSection.value === 'archive') return archiveList.value
    return filteredApprovedMembers.value
})

const {
    currentPage,
    totalPages,
    pageNumbers,
    paginatedItems : paginatedMembers,
    setPage
} = usePagination(activeMembers, pageSize);

const getElapsedDays = (deleteAt) => {
    const deletedTime = new Date(deleteAt).getTime();
    if (Number.isNaN(deletedTime)) return 0;
    return Math.max(0, Math.floor((currentTime.value - deletedTime) / (1000 * 60 * 60 * 24)));
};

const allWithdrawnSelected = computed(() =>
    withdrawnMembers.value.length > 0
    && withdrawnMembers.value.every((member) => selectedWithdrawnMemberNos.value.includes(member.memberNo))
);

// 전체선택은 현재 화면의 승인 대기 회원만 대상으로.
const visibleMemberNos = computed(() =>
    pendingMembers.value.map((member) => member.memberNo)
);

const allVisibleSelected = computed(() =>
    visibleMemberNos.value.length > 0
    && visibleMemberNos.value.every((memberNo) => selectedMemberNos.value.includes(memberNo))
);

const toggleSelectAll = () => {
    if (allVisibleSelected.value) {
        selectedMemberNos.value = [];
        return;
    }
    selectedMemberNos.value = [...visibleMemberNos.value];
};

const selectAllWithdrawnMembers = () => {
    if (allWithdrawnSelected.value) {
        selectedWithdrawnMemberNos.value = [];
        return;
    }
    selectedWithdrawnMemberNos.value = withdrawnMembers.value.map((member) => member.memberNo);
};

const toggleWithdrawnMember = (memberNo) => {
    selectedWithdrawnMemberNos.value = selectedWithdrawnMemberNos.value.includes(memberNo)
        ? selectedWithdrawnMemberNos.value.filter((selectedMemberNo) => selectedMemberNo !== memberNo)
        : [...selectedWithdrawnMemberNos.value, memberNo];
};

const resetSearchInputs = () => {
    searchKeyword.value = '';
    roleFilter.value = '';
    dong.value = '';
    ho.value = '';
};

const applySelectFilters = () => {
    currentPage.value = 1;
    appliedMemberFilters.value = {
        ...appliedMemberFilters.value,
        name: searchKeyword.value,
        role: roleFilter.value,
        dong: dong.value,
        ho: ho.value
    };
};

const handleMemberRoleFilter = () => {
    dong.value = '';
    ho.value = '';
    searchKeyword.value = '';
    applySelectFilters();
};

const handleMemberDongFilter = () => {
    ho.value = '';
    searchKeyword.value = '';
    applySelectFilters();
};

const handleMemberHoFilter = () => {
    searchKeyword.value = '';
    applySelectFilters();
};

const changeSection = async (section) => {
    activeSection.value = section
    currentPage.value = 1
    resetSearchInputs()
    appliedMemberFilters.value = { name: '', role: '', dong: '', ho: '' }
    selectedMemberNos.value = []
    selectedWithdrawnMemberNos.value = []

    if (section === 'archive') {
        await archiveStore.loadList()
        return
    }

    await store.loadmemberList()
}

const restoreSelectedWithdrawnMembers = async () => {
    if (selectedWithdrawnMemberNos.value.length === 0) {
        alert('복원할 회원을 선택해 주세요.');
        return;
    }

    if (!confirm('선택한 회원을 거주 상태로 복원하시겠습니까?')) {
        return;
    }

    const restoredCount = await store.restoreMembers(selectedWithdrawnMemberNos.value);
    currentPage.value = 1;
    selectedWithdrawnMemberNos.value = [];
    alert(`${restoredCount}명의 회원이 복원되었습니다.`);
};

const permanentlyDeleteSelectedWithdrawnMembers = async () => {
    if (selectedWithdrawnMemberNos.value.length === 0) {
        alert('전출 확정할 회원을 선택해 주세요.');
        return;
    }

    if (!confirm('선택한 회원을 전출 확정 처리하시겠습니까?')) {
        return;
    }

    const deletedCount = await store.removeWithdrawnMembers(selectedWithdrawnMemberNos.value);
    currentPage.value = 1;
    selectedWithdrawnMemberNos.value = [];
    alert(`${deletedCount}명의 회원이 전출 확정 처리되었습니다.`);
};

const approveSelectedMembers = async () => {
    if (selectedMemberNos.value.length === 0) {
        alert('승인할 회원을 선택해 주세요.');
        return;
    }
    const approvedMemberNames = pendingMembers.value
        .filter((member) => selectedMemberNos.value.includes(member.memberNo))
        .map((member) => member.memName);

    await store.approveMembers(selectedMemberNos.value);
    currentPage.value = 1;
    selectedMemberNos.value = [];
    alert(`${approvedMemberNames.join(', ')}님이 승인되셨습니다.`);
    await changeSection('approved');
};

const searchGo = async () => {
    currentPage.value = 1;
    await store.loadmemberList();
    appliedMemberFilters.value = {
        name: searchKeyword.value,
        role: roleFilter.value,
        dong: dong.value,
        ho: ho.value
    };
};

const resetMemberSearch = async () => {
    resetSearchInputs();
    appliedMemberFilters.value = { name: '', role: '', dong: '', ho: '' };
    currentPage.value = 1;
    await store.loadmemberList();
};

onMounted(async () => {
    await store.loadmemberList();
    await archiveStore.loadList();
    elapsedCheckTimer = window.setInterval(() => {
        currentTime.value = Date.now();
    }, 60 * 1000);
});

onUnmounted(() => {
    window.clearInterval(elapsedCheckTimer);
});
</script>

<style scoped>
.list-header { display: flex; flex-wrap: wrap; align-items: center; justify-content: space-between; gap: 12px; }
.list-header h2 { margin: 0; }
.member-header-actions { display: flex; flex-wrap: wrap; align-items: center; justify-content: flex-end; gap: 10px; }
.pending-section { margin: 24px 0; padding: 16px; border: 1px solid #f0b8b8; background: #fff8f8; }
.archive-alert-section { margin: 24px 0; padding: 16px; border: 1px solid #e6a23c; background: #fffaf0; }
.approval-actions { display: flex; align-items: center; gap: 8px; margin: 16px 0; }
.member-management-tabs { display: flex; align-items: center; gap: 4px; margin: 0; }
.member-management-tabs button { padding: 9px 14px; border: 1px solid var(--border-color); border-radius: 7px; cursor: pointer; font-weight: 700; color: var(--text-color); background: var(--bg-header); }
.member-management-tabs button:hover { border-color: var(--primary); color: var(--primary); }
.member-management-tabs button.active { border-color: var(--bg-sidebar); color: var(--text-white); background: var(--bg-sidebar); box-shadow: 0 4px 10px rgba(35, 37, 38, 0.18); }
.approved-list-header { display: flex; align-items: center; justify-content: flex-start; flex-wrap: wrap; gap: 12px; margin: 24px 0 12px; }
.approved-list-header h3 { flex: none; margin: 0; }
.member-search, .member-search-fields { display: flex; align-items: center; flex-wrap: wrap; gap: 8px; }
.member-search { min-width: 0; padding: 6px 8px; border: 1px solid var(--border-color); border-radius: 8px; background: var(--bg-header); }
.member-search select, .member-search input, .member-search button { display: inline-block; min-height: 36px; }
.member-number-col { width: 6%; }
.member-role-col { width: 13%; }
.member-name-col { width: 10%; }
.member-dong-col { width: 7%; }
.member-ho-col { width: 7%; }
.member-phone-col { width: 15%; }
.member-id-col { width: 13%; }
.member-created-col { width: 19%; }
.member-status-col { width: 10%; }
table { width: 100%; border-collapse: collapse; }
th, td { padding: 8px; text-align: center; }

@media (max-width: 800px) {
    .approved-list-header { align-items: flex-start; flex-direction: column; }
    .member-search { width: 100%; }
}
</style>
