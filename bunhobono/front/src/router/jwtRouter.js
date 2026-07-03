import { createRouter, createWebHistory } from 'vue-router'

import HomeView from '@/views/HomeView.vue'
import LoginView from '@/components/login/LoginView.vue'
import AdminView from '@/components/login/AdminView.vue'




const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    // 홈
    
  { path: '/', name: 'home', component: HomeView},
  { path: '/login', name: 'login', component: LoginView},
  { path: '/admin', component: AdminView, meta: { requiresRole: 'admin' }},
  
  ]
})

export default router