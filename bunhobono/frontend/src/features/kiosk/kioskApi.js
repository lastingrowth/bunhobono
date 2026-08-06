import api from '@/shared/api/apiClient'

export const getKioskList = () => {
    return api.get('/kiosk')
}

export const deleteKiosk = (kioskNo) => {
    return api.delete(`/kiosk/${kioskNo}/delete`)
}

export const signupKiosk = (data) => {
    return api.post('/kiosk/signUp', data)
}
