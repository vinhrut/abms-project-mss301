import { isValidUuid } from '../../../utils/validation.js'

export function validateVehicleForm(values) {
  const errors = {}

  if (!values.apartmentId?.trim()) {
    errors.apartmentId = 'The * field is required.'
  } else if (!isValidUuid(values.apartmentId.trim())) {
    errors.apartmentId = 'Apartment ID phải là UUID hợp lệ.'
  }

  if (!values.licensePlate?.trim()) {
    errors.licensePlate = 'The * field is required.'
  } else if (!/^[0-9A-Z-]{6,15}$/i.test(values.licensePlate.trim())) {
    errors.licensePlate = 'Biển số xe chỉ gồm chữ, số và dấu gạch nối (6-15 ký tự).'
  }

  if (!values.type?.trim()) {
    errors.type = 'The * field is required.'
  }

  return errors
}