import { defineStore } from 'pinia'
import axios from 'axios'
import { jwtDecode } from 'jwt-decode'

const API_URL = "http://localhost:80"

export const useJwtStore = defineStore('jwtStore', {
  state: () => ({
    token: null,
    userId: null,
    role: null,
    memStatus: null,
    errorMessage: null
  }),
  actions: {
    async loginGo(loginId, loginPwd) {
      try {
        const res = await axios.post(`${API_URL}/login`, { loginId, loginPwd })
        this.token = res.data.token
        this.userId = res.data.userId
        this.errorMessage = null

        localStorage.setItem('jwtToken', this.token)
        localStorage.setItem('userId', this.userId)
        localStorage.setItem('memStatus', this.memStatus)

        axios.defaults.headers.common['Authorization'] = `Bearer ${this.token}`

        const decoded = jwtDecode(this.token)   // 여기서 this.token 사용
        this.userId = decoded.loginId
        this.role = decoded.role
        this.memStatus = decoded.memStatus

        localStorage.setItem('memStatus', this.memStatus) // 올바른 키와 값 저장
  } catch (err) {
        if (err.response && err.response.status === 401) {
          this.errorMessage = err.response.data.error
        } else {
          this.errorMessage = '서버 오류 발생'
        }
      }
    },
    
    logout() {
      this.token = null
      this.userId = null
      this.role = null
      this.memStatus = null
      localStorage.removeItem('jwtToken')
      localStorage.removeItem('userId')
      delete axios.defaults.headers.common['Authorization']
    },
    loadFromStorage() {
      this.token = localStorage.getItem('jwtToken')
      this.userId = localStorage.getItem('userId')
      if (this.token) {
        axios.defaults.headers.common['Authorization'] = `Bearer ${this.token}`
        const decoded = jwtDecode(this.token)
        this.role = decoded.role
        this.memStatus = decoded.memStatus
      }
    }
  }
})
