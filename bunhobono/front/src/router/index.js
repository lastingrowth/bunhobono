import { createRouter, createWebHistory } from 'vue-router'
import TestView from '@/views/TestView.vue'
import memRouter from './memRouter'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    // {
    //   path: '/',
    //   name: 'home',
    //   component: HomeView,
    // },
    {
      path: '/test',
      name: 'test',
      component: TestView,
    },
    ...memRouter,
  ],
})

export default router
