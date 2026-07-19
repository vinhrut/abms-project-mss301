import { apiClient } from '../../../services/apiClient.js'
const BASE = '/api/v1/notifications/invoice-jobs'

export const invoiceNotificationJobService = {
  async getHistory(page = 0, size = 10) {
    const response = await apiClient.get(BASE, { params: { page, size } }); return response.data
  },
  async runNow(period) {
    const response = await apiClient.post(`${BASE}/run`, null, { params: period ? { period } : {} }); return response.data
  },
  async retry(id) {
    const response = await apiClient.post(`${BASE}/${id}/retry`); return response.data
  },
};
