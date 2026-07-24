
import { defineStore } from "pinia"
import { computed, ref } from "vue"
import { useRouter } from "vue-router"

const MAX_HISTORY = 30

export const useHistoryStore = defineStore('historyStore', () => {

    const router = useRouter()

    // 방문한 페이지 목록
    const history = ref([])

    // 현재 위치
    const currentIndex = ref(-1)

    // 뒤로가기 가능 여부
    const canBack = computed(() => currentIndex.value > 0)

    // 앞으로 가기 가능 여부 
    const canForward = computed(() => currentIndex.value < history.value.length - 1)

    // 이동 기록 추가
    function push(path) {
        
        // 같은 페이지면 추가하지 않음
        if (history.value[currentIndex.value] === path) {
            return
        }

        // 뒤로 갔다가 다른 페이지로 이동한 경우
        history.value.splice(currentIndex.value + 1)

        history.value.push(path)

        // 최대 30개 유지
        if (history.value.length > MAX_HISTORY) {
            history.value.shift()
        }

        currentIndex.value = history.value.length - 1
    }

    // 뒤로 가기
    function back() {
        if (!canBack.value) return

        currentIndex.value--

        router.push(history.value[currentIndex.value])
    }

    // 앞으로 가기
    function forward() {
        if (!canForward.value) return

        currentIndex.value++

        router.push(history.value[currentIndex.value])
    }

    // 로그아웃 시 초기화
    function clear() {
        history.value = []
        indexedDB.value = -1
    }

    return {
        history,
        currentIndex,
        canBack,
        canForward,
        push,
        back,
        forward,
        clear,
    }
})