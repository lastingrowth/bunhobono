import StatisticsView from '@/features/statistics/StatisticsView.vue'

// 관리자 통계 페이지 라우터
// /admin/statistics 주소로 접속하면 StatisticsView.vue 화면이 열린다.
export default [
    {
        path: 'statistics',
        name: 'statistics',
        component: StatisticsView,
    },
]