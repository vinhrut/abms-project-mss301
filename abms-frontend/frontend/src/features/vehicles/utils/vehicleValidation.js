import { isValidUuid } from '../../../utils/validation.js'

export function validateVehicleForm(values) {
  const errors = {}

  if (!values.apartmentId?.trim()) {
    errors.apartmentId = 'Vui lòng chọn căn hộ.'
  } else if (!isValidUuid(values.apartmentId.trim())) {
    errors.apartmentId = 'Căn hộ được chọn không hợp lệ.'
  }

  if (!values.licensePlate?.trim()) {
    errors.licensePlate = 'Vui lòng nhập biển số xe.'
  } else if (!/^[0-9A-Z-]{6,15}$/i.test(values.licensePlate.trim())) {
    errors.licensePlate = 'Biển số xe chỉ gồm chữ, số và dấu gạch nối (6-15 ký tự).'
  }

  if (!values.type?.trim()) {
    errors.type = 'Vui lòng chọn loại xe.'
  }

  return errors
}