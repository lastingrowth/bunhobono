import { defineStore } from 'pinia'
import axios from 'axios'

const API_URL = 'http://localhost:80'

export const useVehicleStore = defineStore('vehicle', {
  state: () => ({
    vehicles: []
  }),

  actions: {
    async getVehicles() {
      const res = await axios.get(`${API_URL}/vehicle/list`)
      this.vehicles = res.data
      console.log('vehicles:', this.vehicles)
    }
  }
})