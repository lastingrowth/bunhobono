import { defineStore } from 'pinia'
import { ref } from 'vue'
import { deleteKiosk, getKioskList, signupKiosk } from './kioskApi'

export const useKioskStore = defineStore('kiosk', () => {
    const list = ref([])
    const loading = ref(false)
    const errorMessage = ref('')

    const loadList = async () => {
        loading.value = true
        errorMessage.value = ''

        try {
            const response = await getKioskList()
            list.value = Array.isArray(response.data) ? response.data : []
        } catch (error) {
            console.error('키오스크 목록 조회 실패', error)
            list.value = []
            errorMessage.value = '키오스크 목록을 불러오지 못했습니다.'
        } finally {
            loading.value = false
        }
    }

    const remove = async (kioskNo) => {
        try {
            const response = await deleteKiosk(kioskNo)

            if (response.data === 1) {
                list.value = list.value.filter((kiosk) => kiosk.kioskNo !== kioskNo)
                return { success: true }
            }

            return { success: false, message: '키오스크 삭제에 실패했습니다.' }
        } catch (error) {
            console.error('키오스크 삭제 실패', error)
            return {
                success: false,
                message: '관련 데이터가 연결되어 있어 키오스크를 삭제할 수 없습니다.',
            }
        }
    }

    const signup = async (data) => {
        try {
            const response = await signupKiosk(data)

            if (response.data === 1) {
                await loadList()
                return { success: true }
            }

            return { success: false, message: '키오스크 등록에 실패했습니다.' }
        } catch (error) {
            console.error('키오스크 등록 실패', error)
            return { success: false, message: '키오스크 등록에 실패했습니다.' }
        }
    }

    return {
        list,
        loading,
        errorMessage,
        loadList,
        remove,
        signup,
    }
})
