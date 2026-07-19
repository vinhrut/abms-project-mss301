import { isValidUuid } from '../../../utils/validation.js'

export function validateCashPaymentForm(values) {
  const errors = {}

  if (!values.invoiceId?.trim()) {
    errors.invoiceId = 'Invoice là bắt buộc.'
  } else if (!isValidUuid(values.invoiceId.trim())) {
    errors.invoiceId = 'Invoice ID phải là UUID hợp lệ.'
  }

  if (!values.payerId?.trim()) {
    errors.payerId = 'Payer ID là bắt buộc.'
  } else if (!isValidUuid(values.payerId.trim())) {
    errors.payerId = 'Payer ID phải là UUID hợp lệ.'
  }

  if (!values.collectorId?.trim()) {
    errors.collectorId = 'Collector ID là bắt buộc.'
  } else if (!isValidUuid(values.collectorId.trim())) {
    errors.collectorId = 'Collector ID phải là UUID hợp lệ.'
  }

  const paidAmount = Number(values.paidAmount)
  if (Number.isNaN(paidAmount) || paidAmount <= 0) {
    errors.paidAmount = 'Số tiền phải lớn hơn 0.'
  }

  return errors
}

export function validateVietQrConfirmForm(values) {
  const errors = {}

  if (!values.invoiceId?.trim()) {
    errors.invoiceId = 'Invoice là bắt buộc.'
  } else if (!isValidUuid(values.invoiceId.trim())) {
    errors.invoiceId = 'Invoice ID phải là UUID hợp lệ.'
  }

  if (!values.payerId?.trim()) {
    errors.payerId = 'Payer ID là bắt buộc.'
  } else if (!isValidUuid(values.payerId.trim())) {
    errors.payerId = 'Payer ID phải là UUID hợp lệ.'
  }

  const paidAmount = Number(values.paidAmount)
  if (Number.isNaN(paidAmount) || paidAmount <= 0) {
    errors.paidAmount = 'Số tiền phải lớn hơn 0.'
  }

  return errors
}
