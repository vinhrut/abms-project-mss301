import { apiClient } from './apiClient.js'

export const maintenanceService = {
  async submitRequest(payload) {
    const { data } = await apiClient.post('/api/v1/maintenance-requests', payload)
    return data
  },

  async getAllRequests(params = {}) {
    const { data } = await apiClient.get('/api/v1/maintenance-requests', { params })
    return data
  },

  async getMyRequests(senderId) {
    const { data } = await apiClient.get('/api/v1/maintenance-requests/mine', {
      params: senderId ? { senderId } : undefined,
    })
    return data
  },

  async getMyTasks(technicianId) {
    const { data } = await apiClient.get('/api/v1/maintenance-requests/my-tasks', {
      params: technicianId ? { technicianId } : undefined,
    })
    return data
  },

  async assignStaff(requestId, technicianId) {
    const { data } = await apiClient.post(`/api/v1/maintenance-requests/${requestId}/assign`, {
      technicianId,
    })
    return data
  },

  async completeRequest(requestId) {
    const { data } = await apiClient.post(`/api/v1/maintenance-requests/${requestId}/complete`)
    return data
  },

  async getRequestById(requestId) {
    const { data } = await apiClient.get(`/api/v1/maintenance-requests/${requestId}`)
    return data
  },

  async getHistory(requestId) {
    const { data } = await apiClient.get(`/api/v1/maintenance-requests/${requestId}/history`)
    return data
  },
}
