import axios from 'axios'

const notificationClient = axios.create({
  baseURL: import.meta.env.VITE_NOTIFICATION_API_URL || 'http://localhost:8084',
  headers: { 'Content-Type': 'application/json' },
})

const authHeaders = (auth) => ({
  'X-User-Id': auth?.userId,
  'X-User-Role': auth?.roleName,
})

export const notificationService = {
  async create(payload, auth) {
    const { data } = await notificationClient.post('/api/v1/notifications/announce', payload, { headers: authHeaders(auth) })
    return data
  },
  async list(params, auth) {
    const cleanParams = Object.fromEntries(Object.entries(params).filter(([, value]) => value !== '' && value != null))
    const { data } = await notificationClient.get('/api/v1/notifications', { params: cleanParams, headers: authHeaders(auth) })
    return data
  },
  async detail(id, auth) {
    const { data } = await notificationClient.get(`/api/v1/notifications/${id}`, { headers: authHeaders(auth) })
    return data
  },
  async approve(id, auth) {
    const { data } = await notificationClient.put(`/api/v1/notifications/${id}/approve`, null, { headers: authHeaders(auth) })
    return data
  },
  async markRead(id, auth) {
    const { data } = await notificationClient.put(`/api/v1/notifications/${id}/read`, null, { headers: authHeaders(auth) })
    return data
  },
  async retryFailed(auth) {
    const { data } = await notificationClient.post('/api/v1/notifications/retry-failed', null, { headers: authHeaders(auth) })
    return data
  },
}
