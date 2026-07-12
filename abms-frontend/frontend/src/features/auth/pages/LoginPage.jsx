import { Link, useLocation } from 'react-router-dom'
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
        extractApiErrorMessage(error, 'Invalid username or password. Please try again.'),
      )
    } finally {
      setIsSubmitting(false)
    }
  }

  return (
    <div className="auth-card">
      <div className="auth-card__header">
        <span className="eyebrow">Authentication</span>
        <h2>Đăng nhập hệ thống</h2>
        <p>Đăng nhập để truy cập đúng màn hình theo vai trò: cư dân, staff hoặc manager.</p>
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
          <span>Password *</span>
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
          {isSubmitting ? 'Đang đăng nhập...' : 'Login'}
        </button>

        <div className="auth-inline-note">
          <strong>Gợi ý theo SRS:</strong> sau khi đăng nhập, cư dân sẽ đi vào luồng đăng ký/xem xe, còn staff/manager sẽ vào luồng duyệt xe.
        </div>
      </form>

      <div className="auth-card__footer">
        <p>
          Chưa có tài khoản? <Link to="/register">Đăng ký ngay</Link>
        </p>
        <small>Trang được bảo vệ. Sau khi đăng nhập, bạn sẽ được chuyển đến {redirectHint}.</small>
      </div>
    </div>
  )
}