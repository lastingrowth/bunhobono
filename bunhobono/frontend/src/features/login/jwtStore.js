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

        // 로그인할 수 없는 상태일 때 토큰과 사용자 정보를 모두 비운다.
        const blockLogin = (message) => {
          this.token = null
          this.userId = null
          this.role = null
          this.memStatus = null
          this.errorMessage = message

          localStorage.removeItem('token')
          localStorage.removeItem('userId')
          localStorage.removeItem('memStatus')

          return false
        }

        // 가입 승인 대기 회원은 관리자 승인 전까지 로그인할 수 없다.
        if (this.role === 'PENDING') {
          return blockLogin('회원가입 승인 대기 중입니다. 관리자 승인 후 로그인할 수 있습니다.')
        }

        // 전출 신청 중인 입주민은 관리자 처리 전까지 로그인할 수 없다.
        if (this.memStatus === 'WITHDRAW_PENDING') {
          return blockLogin('전출 신청이 접수된 계정입니다. 관리자 처리 후 이용 상태가 변경됩니다.')
        }

        // 전출 완료 후 빈 세대로 남은 계정은 로그인할 수 없다.
        if (this.memStatus === 'EMPTY') {
          return blockLogin('회원가입 후 이용해주세요.')
        }

        // 퇴사 처리된 관리자 계정은 로그인할 수 없다.
        if (this.memStatus === 'INACTIVE') {
          return blockLogin('퇴사 처리된 관리자 계정입니다. 로그인할 수 없습니다.')
        }

        // 휴직 상태의 관리자 계정은 복귀 전까지 로그인할 수 없다.
        if (this.memStatus === 'ON_LEAVE') {
          return blockLogin('휴직 상태입니다. 관리자에게 복귀 신청을 문의하세요.')
        }

        // 로그인 가능한 상태이므로 기존 에러 문구를 비운다.
        this.errorMessage = null

        localStorage.setItem('token', token)
        localStorage.setItem('userId', this.userId)
        localStorage.setItem('memStatus', this.memStatus)

        // 로그인 성공 후 권한에 따라 화면을 이동한다.
        if (this.role === 'ADMIN') {
          router.push('/admin')
        } else {
          router.push('/resident')
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
