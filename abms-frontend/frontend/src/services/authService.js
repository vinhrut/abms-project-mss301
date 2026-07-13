import { apiClient } from './apiClient.js'

export const authService = {
  async login(payload) {
    const { data } = await apiClient.post('/api/v1/auth/login', payload)
    return data
  },

  async changePassword(payload) {
    const { data } = await apiClient.post('/api/v1/auth/change-password', payload)
    return data
  },

  async register(payload) {
    const { data } = await apiClient.post('/api/v1/auth/register', payload)
    return data
  },

  async getPendingResidents() {
    const { data } = await apiClient.get('/api/v1/auth/residents/pending')
    return data
  },

  async approveResident(userId) {
    const { data } = await apiClient.post(`/api/v1/auth/residents/${userId}/approve`)
    return data
  },

  async rejectResident(userId) {
    const { data } = await apiClient.post(`/api/v1/auth/residents/${userId}/reject`)
    return data
  },
}