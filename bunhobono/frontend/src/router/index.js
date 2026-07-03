import { createRouter, createWebHistory } from 'vue-router'

import HomeView from '@/views/HomeView.vue'
import LoginView from '@/components/login/LoginView.vue'
import AdminView from '@/components/login/AdminView.vue'
import memRouter from './routes/memRouter'
import CarlogView from '@/components/carlog/CarlogView.vue'
import VehicleView from '@/components/vehicle/VehicleView.vue'




const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    // 홈
    
  { path: '/', name: 'home', component: HomeView},
  { path: '/login', name: 'login', component: LoginView},
  { path: '/admin', component: AdminView, meta: { requiresRole: 'admin' }},
  { path: '/carlog', component: CarlogView, meta: { requiresRole: 'admin' }},
  { path: '/vehicle', component: VehicleView, meta: { requiresRole: 'admin' }},
  
  ...memRouter,
  ]
})

export default router