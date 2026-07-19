import { apiClient } from './apiClient.js'

export const paymentService = {
  async getPayments(params = {}) {
    const { data } = await apiClient.get('/api/v1/payments', { params })
    return data
  },

  async getPaymentsByInvoiceId(invoiceId) {
    const { data } = await apiClient.get(`/api/v1/payments/invoice/${invoiceId}`)
    return data
  },

  async getVietQr(invoiceId) {
    const { data } = await apiClient.get(`/api/v1/payments/vietqr/${invoiceId}`)
    return data
  },

  async confirmVietQr(payload) {
    const { data } = await apiClient.post('/api/v1/payments/vietqr/confirm', payload)
    return data
  },

  async recordCash(payload) {
    const { data } = await apiClient.post('/api/v1/payments/cash', payload)
    return data
  },

  async createVnPayPayment(payload) {
    const { data } = await apiClient.post('/api/v1/payments/vnpay/create', payload)
    return data
  },
}
