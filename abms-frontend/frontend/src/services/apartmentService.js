import { apiClient } from './apiClient.js'

export const apartmentService = {
  async getAllApartments() {
    const { data } = await apiClient.get('/api/v1/apartments')
    return data
  },

  async getApartmentsByBuildingId(buildingId) {
    if (!buildingId) {
      return []
    }

    const { data } = await apiClient.get(`/api/v1/buildings/${buildingId}/apartments`)
    return data
  },

  async getBuildings() {
    const { data } = await apiClient.get('/api/v1/buildings')
    return data
  },
}