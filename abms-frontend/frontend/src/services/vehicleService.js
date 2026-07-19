import { apiClient } from './apiClient.js'

export const vehicleService = {
  normalizePage(data) {
    if (Array.isArray(data)) {
      return { content: data, totalPages: 1, totalElements: data.length, number: 0, size: data.length }
    }
    return data
  },

  async registerVehicle(payload) {
    const { data } = await apiClient.post('/api/v1/vehicles/register', payload)
    return data
  },

  async updateVehicle(vehicleId, payload) {
    const { data } = await apiClient.put(`/api/v1/vehicles/${vehicleId}`, payload)
    return data
  },

  async searchVehicles(params = {}) {
    const { data } = await apiClient.get('/api/v1/vehicles', { params })
    return this.normalizePage(data)
  },

  async getMyVehicles() {
    const { data } = await apiClient.get('/api/v1/vehicles/my')
    return data
  },

  async getVehiclesByApartmentId(apartmentId) {
    const { data } = await apiClient.get(`/api/v1/vehicles/apartment/${apartmentId}`)
    return Array.isArray(data) ? data : []
  },

  async updateVehicleStatus(vehicleId, status) {
    const endpoint =
      status === 'APPROVED'
        ? `/api/v1/vehicles/${vehicleId}/approve`
        : `/api/v1/vehicles/${vehicleId}/reject`

    const { data } = await apiClient.post(endpoint)
    return data
  },

  async cancelVehicle(vehicleId) {
    const { data } = await apiClient.post(`/api/v1/vehicles/${vehicleId}/cancel`)
    return data
  },

  async getVehicleLimits(apartmentId) {
    const { data } = await apiClient.get('/api/v1/vehicles/limits', {
      params: apartmentId ? { apartmentId } : {},
    })
    return Array.isArray(data) ? data : []
  },

  async createVehicleLimit(payload) {
    const { data } = await apiClient.post('/api/v1/vehicles/limits', payload)
    return data
  },

  async updateVehicleLimit(limitId, payload) {
    const { data } = await apiClient.put(`/api/v1/vehicles/limits/${limitId}`, payload)
    return data
  },

  async deleteVehicleLimit(limitId) {
    await apiClient.delete(`/api/v1/vehicles/limits/${limitId}`)
  },
}