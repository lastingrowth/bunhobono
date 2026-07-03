import { defineStore } from 'pinia'
import axios from 'axios'

const API_URL = 'http://localhost:80'

export const useCarlogStore = defineStore('carlog', {
  state: () => ({
    carLogs: [],

    search: {
      gateNo: null,
      parkingNo: null,
      parkingState: '',
      carKind: '',
      carNo: '',
      sort: 'latest'
    }
  }),

  actions: {
    async getCarLogs() {
      const res = await axios.get(`${API_URL}/carlog/list`, {
        params: this.search
      })

      this.carLogs = res.data
    },

    async resetSearch() {
      this.search = {
        gateNo: null,
        parkingNo: null,
        parkingState: '',
        carKind: '',
        carNo: '',
        sort: 'latest'
      }

      await this.getCarLogs()
    }
  }
})