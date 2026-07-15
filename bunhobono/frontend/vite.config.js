import { fileURLToPath, URL } from 'node:url'

import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import vueDevTools from 'vite-plugin-vue-devtools'

// https://vite.dev/config/
export default defineConfig({
  plugins: [
    vue(),
    vueDevTools(),
  ],
  resolve: {
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url)),
    },
  },
  server: {
    // 다른 컴퓨터에서도 현재 Vue 개발 서버에 접속할 수 있도록 허용
    host : '0.0.0.0',
    proxy: {
      // Spring API 요청 전달
      '/api' : {
        target: 'http://localhost:80',
        changeOrigin: true
      },

      // Vue의 /fastapi 요청을 FASTAPI 8000 포트로 전달
      // 다른 컴퓨터에서 접속해도 서버 컴퓨터의 FastAPI를 사용한다
      '/fastapi' : {
        target : 'http://localhost:8000',
        changeOrigin : true,
        rewrite : path => path.replace(/^\/fastapi/, '')
      }
    }
  }
})
