import { isValidEmail } from '../../../utils/validation.js'

export function validateLoginForm(values) {
  const errors = {}

  if (!values.email?.trim()) {
    errors.email = 'Vui lòng nhập email.'
  } else if (!isValidEmail(values.email)) {
    errors.email = 'Email không đúng định dạng.'
  }

  if (!values.password?.trim()) {
    errors.password = 'Vui lòng nhập mật khẩu.'
  }

  return errors
}

export function validateRegisterForm(values) {
  const errors = validateLoginForm(values)

  if (!values.fullName?.trim()) {
    errors.fullName = 'Vui lòng nhập họ tên.'
  }

  if (values.password?.trim() && values.password.trim().length < 8) {
    errors.password = 'Mật khẩu phải có ít nhất 8 ký tự.'
  }

  if (values.phone && !/^\d{9,15}$/.test(values.phone)) {
    errors.phone = 'Số điện thoại phải gồm 9-15 chữ số.'
  }

  if (values.idCard && values.idCard.trim().length < 9) {
    errors.idCard = 'ID card cần ít nhất 9 ký tự.'
  }

  if (!values.apartmentId?.trim()) {
    errors.apartmentId = 'Vui lòng chọn căn hộ.'
  }

  return errors
}