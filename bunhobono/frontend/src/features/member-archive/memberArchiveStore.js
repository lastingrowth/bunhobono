import { defineStore } from 'pinia'
import { ref } from 'vue'
import {
    deleteMemberArchive,
    getMemberArchiveList,
} from './memberArchiveApi'

export const useMemberArchiveStore = defineStore('member-archive', () => {
    const list = ref([])
    const loading = ref(false)
    const errorMessage = ref('')

    // 전출 확정되어 archive에 보관된 회원 목록 조회
    const loadList = async () => {
        loading.value = true
        errorMessage.value = ''

        try {
            const res = await getMemberArchiveList()
            list.value = res.data || []
        } catch (error) {
            console.error(error)
            errorMessage.value = '전출 회원 이력을 불러오지 못했습니다.'
        } finally {
            loading.value = false
        }
    }

    // member_archive 기록만 영구 삭제
    // member 원본 데이터는 건드리지 않음
    const remove = async (archiveNo) => {
        await deleteMemberArchive(archiveNo)

        list.value = list.value.filter((item) => {
            return Number(item.archiveNo) !== Number(archiveNo)
        })
    }

    return {
        list,
        loading,
        errorMessage,

        loadList,
        remove,
    }
})