import { apiClient } from '../../../services/apiClient.js'

const clean = (params = {}) => Object.fromEntries(Object.entries(params).filter(([, value]) => value !== '' && value != null))

export const notificationService = {
  async list(params = {}) { const { data } = await apiClient.get('/api/v1/notifications', { params: clean(params) }); return data },
  async detail(id) { const { data } = await apiClient.get(`/api/v1/notifications/${id}`); return data },
  async markRead(id) { const { data } = await apiClient.put(`/api/v1/notifications/${id}/read`); return data },
  async create(payload) { const { data } = await apiClient.post('/api/v1/notifications/announce', payload); return data },
  async approve(id) { const { data } = await apiClient.put(`/api/v1/notifications/${id}/approve`); return data },
  async reject(id, reason) { const { data } = await apiClient.put(`/api/v1/notifications/${id}/reject`, null, { params: { reason } }); return data },
  async cancel(id) { const { data } = await apiClient.put(`/api/v1/notifications/${id}/cancel`); return data },
  async retryFailed() { const { data } = await apiClient.post('/api/v1/notifications/retry-failed'); return data },
}
