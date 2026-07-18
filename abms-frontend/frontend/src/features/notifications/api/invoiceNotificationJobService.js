import { apiClient } from '../../../services/apiClient.js'
import { getStoredAuth } from '../../../utils/storage.js'

const BASE = "/api/v1/notifications/invoice-jobs";

export const invoiceNotificationJobService = {
  async getHistory(page = 0, size = 10) {
    const response = await apiClient.get(BASE, { params: { page, size }, headers: authHeaders() });
    return response.data;
  },
  async runNow(period) {
    const response = await apiClient.post(`${BASE}/run`, null, { params: period ? { period } : {}, headers: authHeaders() });
    return response.data;
  },
  async retry(id) {
    const response = await apiClient.post(`${BASE}/${id}/retry`, null, { headers: authHeaders() });
    return response.data;
  },
};

function authHeaders() {
  const auth = getStoredAuth()
  return { 'X-User-Id': auth?.userId, 'X-User-Role': auth?.roleName }
}
