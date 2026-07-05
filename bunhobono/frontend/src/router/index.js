import { createRouter, createWebHistory } from 'vue-router'
import authRouter from './routes/authRouter'
import LayoutView from '@/views/common/LayoutView.vue'

import residentRouter from './routes/residentRouter'
import adminRouter from './routes/adminRouter'
import { useJwtStore } from '@/features/login/jwtStore'
import HomeView from '@/views/common/HomeView.vue'


const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    
    // 헤더 - 권한 (로그인 / 회원가입)
    ...authRouter,

    { 
      // 공통 레이아웃 
      path : '/',
      component : LayoutView,

      children : [

        // 비로그인 화면
        {
          path : '',
          name : 'home',
          component : HomeView
        },

        // 입주민 화면
        ...residentRouter,

        // 관리자 화면
        ...adminRouter,
      ]
    }

  ]
})

console.log('🔥 ROUTES:', router.getRoutes())

/*
|------------------------------------------
| 권한 가드
|------------------------------------------
 */
router.beforeEach((to, from, next) => {

  const jwtStore = useJwtStore()

  if (!jwtStore.token && localStorage.getItem('token')) {
    jwtStore.loadFromStorage()
  }

  const isLoginRequired = to.meta?.requireAuth
  const allowedRoles = to.meta?.allowedRoles

  // 로그인 필요 페이지인데 토큰이 없을 때
  if (isLoginRequired && !jwtStore.token) {
    return next('/login')
  }

  // role 체크 (admin/resident)
  if (allowedRoles && allowedRoles.length > 0) {
    const userRole = jwtStore.role

    if (!allowedRoles.includes(userRole)) {
      return next('/')
    }
  }

  next()
})
export default router