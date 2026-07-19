import { isValidUuid } from '../../../utils/validation.js'
import {
  isLateFeeService,
  isMeterService,
  isParkingService,
  quantityFromLine,
} from './billingHelpers.js'

export function validateInvoiceCreateForm({ apartmentId, billingMonth, services, lines }) {
  const errors = {}

  if (!apartmentId?.trim()) {
    errors.apartmentId = 'Apartment là bắt buộc.'
  } else if (!isValidUuid(apartmentId.trim())) {
    errors.apartmentId = 'Apartment ID phải là UUID hợp lệ.'
  }

  if (!billingMonth?.trim()) {
    errors.billingMonth = 'Tháng thanh toán là bắt buộc.'
  }

  let enabledCount = 0

  for (const service of services) {
    const key = String(service.serviceId)
    const line = lines[key]
    if (!line?.enabled) {
      continue
    }

    enabledCount += 1
    const fieldKey = `service_${key}`

    if (isMeterService(service)) {
      const oldIndex = Number(line.oldIndex)
      const newIndex = Number(line.newIndex)
      if (Number.isNaN(oldIndex) || oldIndex < 0) {
        errors[fieldKey] = `${service.name}: chỉ số cũ phải >= 0.`
      } else if (Number.isNaN(newIndex) || newIndex < 0) {
        errors[fieldKey] = `${service.name}: chỉ số mới phải >= 0.`
      } else if (newIndex < oldIndex) {
        errors[fieldKey] = `${service.name}: chỉ số mới phải >= chỉ số cũ.`
      } else if (newIndex === oldIndex) {
        errors[fieldKey] = `${service.name}: tiêu thụ phải > 0 (bỏ chọn nếu không dùng).`
      }
      continue
    }

    if (isParkingService(service)) {
      const qty = quantityFromLine(line)
      if (qty <= 0) {
        errors[fieldKey] =
          `${service.name}: căn hộ chưa có xe APPROVED (ô tô / xe máy) để tính phí.`
      }
      continue
    }

    const qty = quantityFromLine(line)
    if (Number.isNaN(qty) || qty <= 0) {
      errors[fieldKey] = `${service.name}: số lượng phải > 0.`
    }

    if (isLateFeeService(service) && qty !== 1) {
      errors[fieldKey] = `${service.name}: số lượng cố định là 1.`
    }
  }

  if (enabledCount === 0) {
    errors.services = 'Chọn ít nhất một dịch vụ để tạo hóa đơn.'
  }

  return errors
}
