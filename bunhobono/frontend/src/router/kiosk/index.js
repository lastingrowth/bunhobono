import billing from './billing'

// 로그인과 분리된 키오스크 전용 경로
export const kioskRoutes = [
    {
        path: '/kiosk',
        children: [
            ...billing,
        ],
    },
]