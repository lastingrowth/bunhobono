import BillingFailView from '@/features/billing/BillingFailView.vue'
import BillingSuccessView from '@/features/billing/BillingSuccessView.vue'
import BillingView from '@/features/billing/BillingView.vue'

// 키오스크 주차요금 정산 화면
export default [
    {
        path: '',
        name: 'KioskBilling',
        component: BillingView,
    },
    {
        path: 'payment/success',
        name: 'KioskBillingSuccess',
        component: BillingSuccessView,
    },
    {
        path: 'payment/fail',
        name: 'KioskBillingFail',
        component: BillingFailView,
    },
]