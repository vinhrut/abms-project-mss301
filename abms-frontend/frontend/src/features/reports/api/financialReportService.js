import axios from 'axios'
import { getStoredAuth } from '../../../utils/storage.js'
const client = axios.create({ baseURL: import.meta.env.VITE_REPORT_API_URL || 'http://localhost:8085', headers: { 'Content-Type': 'application/json' } })
client.interceptors.request.use((config) => {
  const token = getStoredAuth()?.token
  if (token) config.headers.Authorization = `Bearer ${token}`
  return config
})
export const financialReportService = {
  async preview(month, year, signal) { const { data } = await client.get('/api/reports/financial/preview', { params: { month, year }, signal }); return data },
  async exportReport(payload) { return client.post('/api/reports/financial/export', payload, { responseType: 'blob' }) },
}

export async function readReportError(error) {
  const blob = error?.response?.data
  if (blob instanceof Blob) { try { const parsed = JSON.parse(await blob.text()); return parsed } catch { return null } }
  return error?.response?.data
}
