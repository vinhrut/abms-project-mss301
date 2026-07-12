import { apiClient } from './apiClient.js'

export const apartmentService = {
  async getAllApartments() {
    const { data } = await apiClient.get('/api/v1/apartments')
    return data
  },
}