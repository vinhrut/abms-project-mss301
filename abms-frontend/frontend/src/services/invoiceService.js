import { apiClient } from './apiClient.js'

export const invoiceService = {
  async getServices() {
    const { data } = await apiClient.get('/api/v1/services')
    return data
  },

  async getInvoices(params = {}) {
    const { data } = await apiClient.get('/api/v1/invoices', { params })
    return data
  },

  async getInvoiceById(invoiceId) {
    const { data } = await apiClient.get(`/api/v1/invoices/${invoiceId}`)
    return data
  },

  async getInvoicesByApartmentId(apartmentId) {
    const { data } = await apiClient.get(`/api/v1/invoices/apartment/${apartmentId}`)
    return data
  },

  async createFromMeterReadings(payload) {
    const { data } = await apiClient.post('/api/v1/invoices/meter-readings', payload)
    return data
  },
}
