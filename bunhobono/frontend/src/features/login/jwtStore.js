import router from "@/router"
import api from "@/shared/api/apiClient"
import { jwtDecode } from "jwt-decode"
import { defineStore } from "pinia"


export const useJwtStore = defineStore('jwtStore', {
  
  state: () => ({
    token: null,
    userId: null,
    role: null,
    memStatus: null,
    errorMessage: null
  }),

  actions: {
    
    // 로그인
    async loginGo(loginId, loginPwd) {
      try {

        // 에러 초기화
        this.errorMessage = null

        // 로그인 요청
        const res = await api.post(`/login`, { loginId, loginPwd })
        
        // JWT 저장
        const token = res.data.token

        if (!token) {
          throw new Error('토큰이 없습니다.')
        }

        this.token = token


        // JWT 안에 있는 사용자 정보 읽기
        const decoded = jwtDecode(this.token)

        this.userId = decoded.loginId
        this.role = decoded.role
        this.memStatus = decoded.memStatus

        this.errorMessage = null
        
        /*
        |--------------------------------------------------------------------------
        | localStorage 저장
        |--------------------------------------------------------------------------
        | apiClient의 Request Interceptor가
        | localStorage의 jwtToken을 자동으로 읽어
        | Authorization 헤더를 추가해준다.
        */
        localStorage.setItem('token', token)
        localStorage.setItem('userId', this.userId)
        localStorage.setItem('memStatus', this.memStatus)


        // 로그인 성공 후 라우팅
        if (this.role === "ADMIN") {
          router.push("/admin")
        } else {
          router.push("/resident")
        }

        // vue에서 성공 판단용
        return true

      } catch (err) {
      const status = err.response?.status
        if (status === 401) {
          this.errorMessage =
            err.response?.data?.error ||
            err.response?.data?.message ||
            '로그인 실패'
        } else if (status) {
          this.errorMessage =
            err.response?.data?.error ||
            err.response?.data?.message ||
            '요청 처리 실패'
        } else {
          this.errorMessage = '네트워크 오류 또는 서버 미응답'
        }
        return false
      }
  },
    
    /*
    |--------------------------------------------------------------------------
    | 로그아웃
    |--------------------------------------------------------------------------
    */
    logout() {
      this.token = null
      this.userId = null
      this.role = null
      this.memStatus = null
      this.errorMessage = null

      localStorage.removeItem('token')
      localStorage.removeItem('userId')
      localStorage.removeItem('memStatus')

      router.push("/")
    },

    /*
    |--------------------------------------------------------------------------
    | 새로고침 시 로그인 상태 복원
    |--------------------------------------------------------------------------
    */
    loadFromStorage() {
      const token = localStorage.getItem('token')

      // 토큰 없으면 초기화
      if (!token) {
        this.logout()
        return
      }

      try {
        this.token = token

        const decoded = jwtDecode(token)

        this.userId = decoded.loginId
        this.role = decoded.role
        this.memStatus = decoded.memStatus

        this.errorMessage = null

      }  catch (err) {
        console.error('Invalid JWT token:', err)

        // 깨진 토큰이면 제거
        this.logout()
      }    
      
    }
  }
})
