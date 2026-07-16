/* 목록을 잘라서 현재 페이지 데이터만 만들어주는 JS */

import { computed, ref, watch } from "vue"

// 여러 목록 화면에서 재사용할 수 있는 페이지네이션 계산 함수
// items : 전체 목록
// pageSize : 한 페이지에 보여줄 개수
export const usePagination = (items, pageSize = 10) => {
    
    const currentPage = ref(1)

    // 전체 페이지 수를 계산
    // 목록이 비어 있어도 화면 처리를 쉽게 하기 위해 최소 1페이지로 둔다
    const totalPages = computed(() => {
        return Math.max(
            Math.ceil(items.value.length / pageSize),
            1
        )
    })

    // 현재 페이지에 보여줄 목록만 잘라낸다
    const paginatedItems = computed(() => {
        const start = (currentPage.value - 1) * pageSize
        const end = start + pageSize

        return items.value.slice(start,end)
    })

    // 페이지 버튼에 표시할 번호 목록
    // 5개 단위로 묶어서 보여준다
    const pageNumbers = computed(() => {
        const pageGroupSize = 5

        const currentGroup = Math.ceil(
            currentPage.value / pageGroupSize
        )

        const startPage = (currentGroup - 1) * pageGroupSize + 1

        const endPage = Math.min(
            startPage + pageGroupSize - 1,
            totalPages.value
        )

        return Array.from(
            { length : endPage - startPage + 1 },
            (_, index) => startPage + index
        )
    })

    // 선택한 페이지로 이동한다
    const setPage = (page) => {
        if (page < 1 || page > totalPages.value) {
            return
        }

        currentPage.value = page
    }

    // 검색이나 필터로 목록 길이가 바뀌었을 때, 현재 페이지가 범위를 벗어나지 않게 보장
    watch(items, () => {
        currentPage.value = 1
    })

    return {
        currentPage,
        totalPages,
        pageNumbers,
        paginatedItems,
        setPage
    }
}