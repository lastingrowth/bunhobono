<template>
    <div class="list-header">
        <h2>회원 목록 조회</h2>
        <button type="button" @click="router.push('/admin/signup')">회원 추가</button>
    </div>

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
                <tr v-for="mem in pendingMembers" :key="mem.memberNo">
                    <td><input v-model="selectedMemberNos" type="checkbox" :value="mem.memberNo"></td>
                    <td>{{ mem.role }}</td>
                    <td><router-link :to="`/admin/members/${mem.memberNo}/detail`">{{ mem.memName }}</router-link></td>
                    <td>{{ mem.memDong }}</td><td>{{ mem.memHo }}</td>
                    <td>{{ mem.memPhone }}</td><td>{{ mem.loginId }}</td>
                </tr>
                <tr v-if="pendingMembers.length === 0"><td colspan="7">승인 대기 회원이 없습니다.</td></tr>
            </tbody>
        </table>
    </section>

    <div class="approval-actions">
        <button type="button" @click="selectAllArchives">전체선택</button>
        <button type="button" @click="deleteSelectedArchives">삭제</button>
        <span>선택 {{ selectedArchiveMemberNos.length }}명</span>
    </div>

    <!-- Spring 스케줄러가 분류한 탈퇴 3일 경과 회원을 별도 알림으로 표시. -->
    <section class="archive-alert-section">
        <h3>탈퇴 후 3일 경과 회원 알림 ({{ memberArchiveAlerts.length }}명)</h3>
        <table border="">
            <thead>
                <tr>
                    <th>선택</th><th>가입유형</th><th>이름</th><th>동</th><th>호수</th>
                    <th>아이디</th><th>상태</th><th>탈퇴일</th><th>알림</th>
                </tr>
            </thead>
            <tbody>
                <tr v-for="mem in memberArchiveAlerts" :key="mem.memberNo">
                    <td><input v-model="selectedArchiveMemberNos" type="checkbox" :value="mem.memberNo"></td>
                    <td>{{ mem.role }}</td>
                    <td><router-link :to="`/admin/members/${mem.memberNo}/detail`">{{ mem.memName }}</router-link></td>
                    <td>{{ mem.memDong }}</td><td>{{ mem.memHo }}</td><td>{{ mem.loginId }}</td>
                    <td>{{ mem.memStatus }}</td><td>{{ mem.memDeleteAt }}</td>
                    <td>보관 삭제 확인 필요</td>
                </tr>
                <tr v-if="memberArchiveAlerts.length === 0">
                    <td colspan="9">탈퇴 후 3일이 지난 회원이 없습니다.</td>
                </tr>
            </tbody>
        </table>
    </section>

    <div>
        <select v-model="searchType">
            <option value="all">전체출력</option>
            <option value="role">가입유형</option>
            <option value="name">이름</option>
            <option value="dongHo">동호수</option>
        </select>
        <div v-if="searchType === 'dongHo'">
            <input type="text" v-model="dong" placeholder="동">
            <input type="text" v-model="ho" placeholder="호수">
        </div>
        <input v-else-if="searchType !== 'all'" type="text" v-model="searchKeyword" placeholder="검색어 입력" />
        <button @click="searchGo">검색</button>
    </div>

    <h3>승인된 회원 ({{ approvedMembers.length }}명)</h3>
    <table border="">
        <thead>
            <tr>
                <th>가입유형</th><th>이름</th><th>동</th><th>호수</th><th>연락처</th>
                <th>아이디</th><th>가입일</th><th>탈퇴일</th><th>상태</th>
            </tr>
        </thead>
        <tbody>
            <tr v-for="mem in approvedMembers" :key="mem.memberNo">
                <td>{{ mem.role }}</td>
                <td><router-link :to="`/admin/members/${mem.memberNo}/detail`">{{ mem.memName }}</router-link></td>
                <td>{{ mem.memDong }}</td><td>{{ mem.memHo }}</td><td>{{ mem.memPhone }}</td>
                <td>{{ mem.loginId }}</td><td>{{ mem.memCreateAt }}</td><td>{{ mem.memDeleteAt }}</td>
                <td>{{ mem.memStatus }}</td>
            </tr>
        </tbody>
    </table>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue';
import { useMemStore } from './memStore';
import { storeToRefs } from 'pinia';
import { useRouter } from 'vue-router';

const store = useMemStore();
const router = useRouter();
const { memberList, memberArchiveAlerts } = storeToRefs(store);
const searchType = ref('all');
const searchKeyword = ref('');
const dong = ref('');
const ho = ref('');
const selectedMemberNos = ref([]);
const selectedArchiveMemberNos = ref([]);

// PENDING 역할 회원만 승인 대기 목록으로 분류한다.
const pendingMembers = computed(() => memberList.value.filter(
    (member) => String(member.role || '').toUpperCase() === 'PENDING'
));

// ADMIN과 RESIDENT 역할 회원만 승인된 회원 목록에 표시한다.
const approvedMembers = computed(() => memberList.value.filter(
    (member) => ['ADMIN', 'RESIDENT'].includes(String(member.role || '').toUpperCase())
        && !memberArchiveAlerts.value.some((alertMember) => alertMember.memberNo === member.memberNo)
));

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

// 탈퇴 후 3일 경과 목록만 전체선택하며 전체해제 동작은 제공하지 않는다.
const selectAllArchives = () => {
    selectedArchiveMemberNos.value = memberArchiveAlerts.value.map((member) => member.memberNo);
};

const deleteSelectedArchives = async () => {
    if (selectedArchiveMemberNos.value.length === 0) {
        alert('삭제할 회원을 선택해 주세요.');
        return;
    }
    if (!confirm('선택한 회원을 완전히 삭제하시겠습니까?')) return;

    const deletedCount = await store.removeMemberArchives(selectedArchiveMemberNos.value);
    selectedArchiveMemberNos.value = [];
    alert(`${deletedCount}명의 회원이 삭제되었습니다.`);
};

const approveSelectedMembers = async () => {
    if (selectedMemberNos.value.length === 0) {
        alert('승인할 회원을 선택해 주세요.');
        return;
    }
    await store.approveMembers(selectedMemberNos.value);
    selectedMemberNos.value = [];
    alert('선택한 회원이 입주민으로 승인되었습니다.');
};

const searchGo = async () => {
    if (searchType.value === 'all') return store.loadmemberList();
    if (searchType.value === 'dongHo') {
        return store.search({ type: 'dongHo', dong: dong.value, ho: ho.value });
    }
    return store.search({ type: searchType.value, keyword: searchKeyword.value });
};

onMounted(async () => {
    await Promise.all([
        store.loadmemberList(),
        store.loadMemberArchiveAlerts()
    ]);
});
</script>

<style scoped>
.list-header { display: flex; align-items: center; justify-content: space-between; }
.list-header h2 { margin: 0; }
.pending-section { margin: 24px 0; padding: 16px; border: 1px solid #f0b8b8; background: #fff8f8; }
.archive-alert-section { margin: 24px 0; padding: 16px; border: 1px solid #e6a23c; background: #fffaf0; }
.approval-actions { display: flex; align-items: center; gap: 8px; margin: 16px 0; }
table { width: 100%; border-collapse: collapse; }
th, td { padding: 8px; text-align: center; }
</style>
