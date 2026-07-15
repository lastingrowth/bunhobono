<template>
    <div class="list-header">
        <h2>회원 관리</h2>
        <button type="button" @click="router.push('/admin/signup')">회원 추가</button>
    </div>

    <nav class="member-management-tabs" aria-label="회원관리 메뉴">
        <button
            v-for="section in managementSections"
            :key="section.value"
            type="button"
            :class="{ active: activeSection === section.value }"
            @click="changeSection(section.value)"
        >
            {{ section.label }}
        </button>
    </nav>

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
        <table border="">
            <thead>
                <tr>
                    <th>선택</th><th>가입유형</th><th>이름</th><th>동</th><th>호수</th>
                    <th>연락처</th><th>아이디</th>
                </tr>
            </thead>
            <tbody>
                <tr v-for="mem in paginatedPendingMembers" :key="mem.memberNo">
                    <td><input v-model="selectedMemberNos" type="checkbox" :value="mem.memberNo"></td>
                    <td>{{ mem.role }}</td>
                    <td><router-link :to="`/admin/members/${mem.memberNo}/detail`">{{ mem.memName }}</router-link></td>
                    <td>{{ mem.memDong }}</td><td>{{ mem.memHo }}</td>
                    <td>{{ mem.memPhone }}</td><td>{{ mem.loginId }}</td>
                </tr>
                <tr v-if="pendingMembers.length === 0"><td colspan="7">승인 대기 회원이 없습니다.</td></tr>
            </tbody>
        </table>
        <div class="member-pagination">
            <button type="button" :disabled="currentPage === 1" @click="setPage(currentPage - 1)">이전</button>
            <button v-for="page in pageNumbers" :key="page" type="button" :class="{ active: currentPage === page }" @click="setPage(page)">{{ page }}</button>
            <button type="button" :disabled="currentPage === totalPages" @click="setPage(currentPage + 1)">다음</button>
        </div>
    </section>
    </template>

    <template v-if="activeSection === 'withdrawn'">
    <div class="approval-actions">
        <button type="button" @click="selectAllWithdrawnMembers">{{ allWithdrawnSelected ? '전체해제' : '전체선택' }}</button>
        <button type="button" @click="restoreSelectedWithdrawnMembers">복원</button>
        <button type="button" @click="permanentlyDeleteSelectedWithdrawnMembers">영구삭제</button>
        <span>선택 {{ selectedWithdrawnMemberNos.length }}명</span>
    </div>

    <!-- 탈퇴한 회원을 승인된 회원 바로 위에서 확인한다. -->
    <section class="archive-alert-section">
        <h3>탈퇴한 회원 리스트 ({{ withdrawnMembers.length }}명)</h3>
        <table border="">
            <thead>
                <tr>
                    <th>선택</th><th>가입유형</th><th>이름</th><th>동</th><th>호수</th>
                    <th>아이디</th><th>상태</th><th>탈퇴일</th><th>경과일</th>
                </tr>
            </thead>
            <tbody>
                <tr v-for="mem in paginatedWithdrawnMembers" :key="mem.memberNo" :class="{ 'withdrawn-expired': getElapsedDays(mem.memDeleteAt) >= 3 }">
                    <td><input type="checkbox" :checked="selectedWithdrawnMemberNos.includes(mem.memberNo)" @change="toggleWithdrawnMember(mem.memberNo)"></td>
                    <td>{{ mem.role }}</td>
                    <td><router-link :to="`/admin/members/${mem.memberNo}/detail`">{{ mem.memName }}</router-link></td>
                    <td>{{ mem.memDong }}</td><td>{{ mem.memHo }}</td><td>{{ mem.loginId }}</td>
                    <td>{{ mem.memStatus }}</td><td>{{ mem.memDeleteAt }}</td>
                    <td>{{ getElapsedDays(mem.memDeleteAt) }}일</td>
                </tr>
                <tr v-if="withdrawnMembers.length === 0">
                    <td colspan="9">탈퇴한 회원이 없습니다.</td>
                </tr>
            </tbody>
        </table>
        <div class="member-pagination">
            <button type="button" :disabled="currentPage === 1" @click="setPage(currentPage - 1)">이전</button>
            <button v-for="page in pageNumbers" :key="page" type="button" :class="{ active: currentPage === page }" @click="setPage(page)">{{ page }}</button>
            <button type="button" :disabled="currentPage === totalPages" @click="setPage(currentPage + 1)">다음</button>
        </div>
    </section>
    </template>

    <template v-if="activeSection === 'approved'">
    <div class="approved-list-header">
        <h3>승인된 회원 ({{ approvedMembers.length }}명)</h3>
        <div class="member-search">
            <select v-model="searchType" @change="resetSearchInputs">
                <option value="all">전체출력</option>
                <option value="role">가입유형</option>
                <option value="name">이름</option>
                <option value="dongHo">동호수</option>
            </select>
            <select v-if="searchType === 'role'" v-model="searchKeyword">
                <option value="" disabled>가입유형 선택</option>
                <option value="ADMIN">관리자</option>
                <option value="RESIDENT">입주민</option>
            </select>
            <div v-else-if="searchType === 'dongHo'" class="member-search-fields">
                <select v-model="dong">
                    <option value="" disabled>동을 선택하세요</option>
                    <option v-for="dongOption in dongOptions" :key="dongOption" :value="dongOption">{{ dongOption }}동</option>
                </select>
                <input type="text" v-model="ho" placeholder="호수(숫자만 적어주세요)">
            </div>
            <input v-else-if="searchType !== 'all'" type="text" v-model="searchKeyword" placeholder="검색어 입력" />
            <button @click="searchGo">검색</button>
        </div>
    </div>

    <table border="">
        <thead>
            <tr>
                <th>번호</th><th>가입유형</th><th>이름</th><th>동</th><th>호수</th><th>연락처</th>
                <th>아이디</th><th>가입일</th><th>상태</th>
            </tr>
        </thead>
        <tbody>
            <tr v-for="mem in paginatedApprovedMembers" :key="mem.memberNo">
                <td>{{ mem.displayNo }}</td>
                <td>{{ mem.role }}</td>
                <td><router-link :to="`/admin/members/${mem.memberNo}/detail`">{{ mem.memName }}</router-link></td>
                <td>{{ mem.memDong }}</td><td>{{ mem.memHo }}</td><td>{{ mem.memPhone }}</td>
                <td>{{ mem.loginId }}</td><td>{{ mem.memCreateAt }}</td>
                <td>{{ mem.memStatus }}</td>
            </tr>
        </tbody>
    </table>
    <div class="member-pagination">
        <button type="button" :disabled="currentPage === 1" @click="setPage(currentPage - 1)">이전</button>
        <button v-for="page in pageNumbers" :key="page" type="button" :class="{ active: currentPage === page }" @click="setPage(page)">{{ page }}</button>
        <button type="button" :disabled="currentPage === totalPages" @click="setPage(currentPage + 1)">다음</button>
    </div>
    </template>
</template>

<script setup>
import { computed, onMounted, onUnmounted, ref } from 'vue';
import { useMemStore } from './memStore';
import { storeToRefs } from 'pinia';
import { useRoute, useRouter } from 'vue-router';

const store = useMemStore();
const route = useRoute();
const router = useRouter();
const { memberList } = storeToRefs(store);
const searchType = ref('all');
const searchKeyword = ref('');
const dong = ref('');
const ho = ref('');
const dongOptions = [0, 101, 102, 103, 104, 105, 106, 107, 108];
const selectedMemberNos = ref([]);
const selectedWithdrawnMemberNos = ref([]);
const currentTime = ref(Date.now());
const currentPage = ref(1);
const pageSize = 10;
let elapsedCheckTimer;
const managementSections = [
    { value: 'approved', label: '승인회원관리' },
    { value: 'pending', label: '승인대기회원' },
    { value: 'withdrawn', label: '탈퇴회원관리' }
];
const requestedSection = String(route.query.section || 'approved');
const activeSection = ref(
    managementSections.some((section) => section.value === requestedSection)
        ? requestedSection
        : 'approved'
);

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

const withdrawnMembers = computed(() => memberList.value
    .filter((member) => Boolean(member.memDeleteAt))
    .sort((left, right) => new Date(right.memDeleteAt) - new Date(left.memDeleteAt))
);

const activeMembers = computed(() => {
    if (activeSection.value === 'pending') return pendingMembers.value;
    if (activeSection.value === 'withdrawn') return withdrawnMembers.value;
    return approvedMembers.value;
});

const totalPages = computed(() => Math.max(Math.ceil(activeMembers.value.length / pageSize), 1));

const pageNumbers = computed(() => {
    let startPage = Math.max(currentPage.value - 2, 1);
    let endPage = Math.min(startPage + 4, totalPages.value);
    startPage = Math.max(endPage - 4, 1);
    return Array.from({ length: endPage - startPage + 1 }, (_, index) => startPage + index);
});

const paginateMembers = (members) => {
    const start = (currentPage.value - 1) * pageSize;
    return members.slice(start, start + pageSize);
};

const paginatedApprovedMembers = computed(() => paginateMembers(approvedMembers.value));
const paginatedPendingMembers = computed(() => paginateMembers(pendingMembers.value));
const paginatedWithdrawnMembers = computed(() => paginateMembers(withdrawnMembers.value));

const setPage = (page) => {
    if (page < 1 || page > totalPages.value) return;
    currentPage.value = page;
};

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
    dong.value = '';
    ho.value = '';
};

const changeSection = async (section) => {
    activeSection.value = section;
    currentPage.value = 1;
    resetSearchInputs();
    searchType.value = 'all';
    selectedMemberNos.value = [];
    selectedWithdrawnMemberNos.value = [];
    await store.loadmemberList();
};

const restoreSelectedWithdrawnMembers = async () => {
    if (selectedWithdrawnMemberNos.value.length === 0) {
        alert('복원할 회원을 선택해 주세요.');
        return;
    }
    if (!confirm('선택한 회원을 거주 상태로 복원하시겠습니까?')) return;

    const restoredCount = await store.restoreMembers(selectedWithdrawnMemberNos.value);
    currentPage.value = 1;
    selectedWithdrawnMemberNos.value = [];
    alert(`${restoredCount}명의 회원이 복원되었습니다.`);
};

const permanentlyDeleteSelectedWithdrawnMembers = async () => {
    if (selectedWithdrawnMemberNos.value.length === 0) {
        alert('영구 삭제할 회원을 선택해 주세요.');
        return;
    }
    if (!confirm('선택한 회원을 영구 삭제하시겠습니까? 이 작업은 되돌릴 수 없습니다.')) return;

    const deletedCount = await store.removeWithdrawnMembers(selectedWithdrawnMemberNos.value);
    currentPage.value = 1;
    selectedWithdrawnMemberNos.value = [];
    alert(`${deletedCount}명의 회원이 영구 삭제되었습니다.`);
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
    if (searchType.value === 'all') return store.loadmemberList();
    if (searchType.value === 'dongHo') {
        return store.search({ type: 'dongHo', dong: dong.value, ho: ho.value });
    }
    return store.search({ type: searchType.value, keyword: searchKeyword.value });
};

onMounted(async () => {
    await store.loadmemberList();
    elapsedCheckTimer = window.setInterval(() => {
        currentTime.value = Date.now();
    }, 60 * 1000);
});

onUnmounted(() => {
    window.clearInterval(elapsedCheckTimer);
});
</script>

<style scoped>
.list-header { display: flex; align-items: center; justify-content: space-between; }
.list-header h2 { margin: 0; }
.pending-section { margin: 24px 0; padding: 16px; border: 1px solid #f0b8b8; background: #fff8f8; }
.archive-alert-section { margin: 24px 0; padding: 16px; border: 1px solid #e6a23c; background: #fffaf0; }
.withdrawn-expired, .withdrawn-expired a { color: #dc2626; font-weight: 700; }
.approval-actions { display: flex; align-items: center; gap: 8px; margin: 16px 0; }
.member-management-tabs { display: flex; align-items: center; gap: 4px; margin: 18px 0; }
.member-management-tabs button { padding: 9px 14px; border: 1px solid var(--border-color); border-radius: 7px; cursor: pointer; font-weight: 700; color: var(--text-color); background: var(--bg-header); }
.member-management-tabs button:hover { border-color: var(--primary); color: var(--primary); }
.member-management-tabs button.active { border-color: var(--primary); color: var(--text-white); background: var(--primary); }
.approved-list-header { display: flex; align-items: center; justify-content: flex-start; flex-wrap: wrap; gap: 12px; margin: 24px 0 12px; }
.approved-list-header h3 { flex: none; margin: 0; }
.member-search, .member-search-fields { display: flex; align-items: center; flex-wrap: wrap; gap: 8px; }
.member-search { min-width: 0; padding: 6px 8px; border: 1px solid var(--border-color); border-radius: 8px; background: var(--bg-header); }
.member-search select, .member-search input, .member-search button { display: inline-block; min-height: 36px; }
.member-pagination { margin-top: 12px; display: flex; justify-content: center; align-items: center; gap: 5px; }
.member-pagination button { min-width: 34px; height: 30px; padding: 0 9px; border: 1px solid var(--border-color); border-radius: 7px; cursor: pointer; font-size: 12px; color: var(--text-color); background: var(--bg-header); }
.member-pagination button:hover:not(:disabled) { border-color: var(--primary); color: var(--primary); }
.member-pagination button.active { border-color: var(--primary); color: var(--text-white); background: var(--primary); }
.member-pagination button:disabled { cursor: default; opacity: 0.45; }
table { width: 100%; border-collapse: collapse; }
th, td { padding: 8px; text-align: center; }

@media (max-width: 800px) {
    .approved-list-header { align-items: flex-start; flex-direction: column; }
    .member-search { width: 100%; }
}
</style>
