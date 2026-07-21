import { useLocation } from 'react-router-dom'
import { useMemo, useState } from 'react'
import { useAuth } from '../context/useAuth.js'
import { validateLoginForm } from '../utils/authValidation.js'
import { extractApiErrorMessage } from '../../../utils/apiError.js'

const initialForm = {
  email: '',
  password: '',
}

export function LoginPage() {
  const [formData, setFormData] = useState(initialForm)
  const [errors, setErrors] = useState({})
  const [apiError, setApiError] = useState('')
  const [isSubmitting, setIsSubmitting] = useState(false)
  const { login } = useAuth()
  const location = useLocation()

  const redirectHint = useMemo(
    () => location.state?.from?.pathname || '/dashboard',
    [location.state],
  )

  const handleChange = (event) => {
    const { name, value } = event.target
    setFormData((prev) => ({ ...prev, [name]: value }))
    setErrors((prev) => ({ ...prev, [name]: '' }))
    setApiError('')
  }

  const handleSubmit = async (event) => {
    event.preventDefault()

    const validationErrors = validateLoginForm(formData)
    setErrors(validationErrors)

    if (Object.keys(validationErrors).length > 0) {
      return
    }

    try {
      setIsSubmitting(true)
      await login(formData)
    } catch (error) {
      setApiError(
        extractApiErrorMessage(error, 'Email hoặc mật khẩu không đúng. Vui lòng thử lại.'),
      )
    } finally {
      setIsSubmitting(false)
    }
  }

  return (
    <div className="auth-card">
      <div className="auth-card__header">
        <span className="eyebrow">Đăng nhập</span>
        <h2>Đăng nhập hệ thống</h2>
        <p>Đăng nhập để truy cập đúng workspace theo vai trò. Tài khoản được cấp bởi Admin hoặc Manager, không đăng ký công khai.</p>
      </div>

      <form className="form-grid" onSubmit={handleSubmit}>
        <label className="form-field">
          <span>Email *</span>
          <input
            type="email"
            name="email"
            placeholder="user@example.com"
            value={formData.email}
            onChange={handleChange}
          />
          {errors.email ? <small className="field-error">{errors.email}</small> : null}
        </label>

        <label className="form-field">
          <span>Mật khẩu *</span>
          <input
            type="password"
            name="password"
            placeholder="••••••••"
            value={formData.password}
            onChange={handleChange}
          />
          {errors.password ? <small className="field-error">{errors.password}</small> : null}
        </label>

        {apiError ? <div className="alert alert-error">{apiError}</div> : null}

        <button type="submit" className="btn btn-primary btn-block" disabled={isSubmitting}>
          {isSubmitting ? 'Đang đăng nhập...' : 'Đăng nhập'}
        </button>

        <div className="auth-inline-note">
          <strong>Ghi chú nghiệp vụ:</strong> nếu bạn chưa có tài khoản, vui lòng liên hệ Admin hoặc Ban quản lý tòa nhà để được cấp quyền truy cập.
        </div>
      </form>

      <div className="auth-card__footer">
        <small>Trang được bảo vệ. Sau khi đăng nhập, hệ thống sẽ chuyển bạn đến khu vực phù hợp.</small>
      </div>
    </div>
  )
}