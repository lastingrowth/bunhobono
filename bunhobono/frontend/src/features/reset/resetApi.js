import api from '@/shared/api/apiClient'

export const resetDemo = () => {
  return api.post('/reset', null, {
    timeout: 120000,
  })
}
