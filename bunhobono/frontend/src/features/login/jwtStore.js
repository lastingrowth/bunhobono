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

        // 전출 신청·퇴사·빈 세대 상태의 계정은 로그인할 수 없도록 차단한다.
        // ON_LEAVE는 기존 휴직 기능을 유지하기 위해 로그인 차단 대상에 넣지 않는다.
        if (['WITHDRAW_PENDING', 'INACTIVE', 'EMPTY'].includes(this.memStatus)) {
          this.token = null
          this.userId = null
          this.role = null
          this.memStatus = null
          this.errorMessage = '사용할 수 없는 계정 상태입니다.'

          localStorage.removeItem('token')
          localStorage.removeItem('userId')
          localStorage.removeItem('memStatus')
          return false
        }

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
        } else if (this.role === "PENDING") {
          this.token = null
          this.userId = null
          this.role = null
          this.memStatus = null
          this.errorMessage = null

          localStorage.removeItem('token')
          localStorage.removeItem('userId')
          localStorage.removeItem('memStatus')

          alert("관리자 승인 후 로그인할 수 있습니다.")
          return false
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

      router.push("/login")
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
