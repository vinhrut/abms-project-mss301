import { apiClient } from './apiClient.js'

export const vehicleService = {
  async registerVehicle(payload) {
    const { data } = await apiClient.post('/api/v1/vehicles/', payload)
    return data
  },

  async getAllVehicles() {
    const { data } = await apiClient.get('/api/v1/vehicles')
    return data
  },

  async getVehiclesByApartmentId(apartmentId) {
    const { data } = await apiClient.get(`/api/v1/vehicles/apartment/${apartmentId}`)
    return data
  },

  async getVehiclesByOwnerId(ownerId) {
    const { data } = await apiClient.get(`/api/v1/vehicles/owner/${ownerId}`)
    return data
  },

  async updateVehicleStatus(vehicleId, status) {
    const endpoint =
      status === 'APPROVED'
        ? `/api/v1/vehicles/${vehicleId}/approve`
        : `/api/v1/vehicles/${vehicleId}/reject`

    const { data } = await apiClient.post(endpoint)
    return data
  },
}