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

  async getBuildingById(buildingId) {
    if (!buildingId) {
      return null
    }

    const { data } = await apiClient.get(`/api/v1/buildings/${buildingId}`)
    return data
  },

  async getBuildingResidentsByBuildingId(buildingId) {
    if (!buildingId) {
      return []
    }

    const { data } = await apiClient.get(`/api/v1/buildings/${buildingId}/residents`)
    return data
  },

  async getApartmentResidentsByApartmentId(apartmentId) {
    if (!apartmentId) {
      return []
    }

    const { data } = await apiClient.get(`/api/v1/apartments/${apartmentId}/residents`)
    return data
  },

  async renewResidentContract(apartmentId, userId) {
    const { data } = await apiClient.post(`/api/v1/apartments/${apartmentId}/residents/${userId}/contracts/renew`)
    return data
  },

  async removeResidentFromApartment(apartmentId, userId) {
    const { data } = await apiClient.post(`/api/v1/apartments/${apartmentId}/residents/${userId}/remove`)
    return data
  },

  // contracts
  async getContracts(buildingId) {
    const url = buildingId ? `/api/v1/contracts?buildingId=${buildingId}` : '/api/v1/contracts'
    const { data } = await apiClient.get(url)
    return data
  },

  async getContractById(contractId) {
    const { data } = await apiClient.get(`/api/v1/contracts/${contractId}`)
    return data
  },

  async renewContract(contractId, payload) {
    const { data } = await apiClient.post(`/api/v1/contracts/${contractId}/renew`, payload)
    return data
  },
}
