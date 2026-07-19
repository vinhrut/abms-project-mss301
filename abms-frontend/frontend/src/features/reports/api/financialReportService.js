import { apiClient } from '../../../services/apiClient.js'

export const financialReportService = {
  async preview(month, year, signal) {
    const { data } = await apiClient.get('/api/reports/financial/preview', { params: { month, year }, signal })
    return data
  },
  async exportReport(payload) {
    return apiClient.post('/api/reports/financial/export', payload, { responseType: 'blob' })
  },
}

export async function readReportError(error) {
  const blob = error?.response?.data
  if (blob instanceof Blob) {
    try { return JSON.parse(await blob.text()) } catch { return null }
  }
  return blob
}
