import { apiClient } from './apiClient.js'

export const userService = {
  async getUsers() {
    const { data } = await apiClient.get('/api/v1/users')
    return data
  },

  async createManager(payload) {
    const { data } = await apiClient.post('/api/v1/users/managers', payload)
    return data
  },

  async createUser(payload) {
    const { data } = await apiClient.post('/api/v1/users', payload)
    return data
  },

  async lockUser(userId) {
    const { data } = await apiClient.post(`/api/v1/users/${userId}/lock`)
    return data
  },

  async unlockUser(userId) {
    const { data } = await apiClient.post(`/api/v1/users/${userId}/unlock`)
    return data
  },

  async getUserById(userId) {
    if (!userId) {
      return null
    }

    const { data } = await apiClient.get(`/api/v1/users/${userId}`)
    return data
  },
}