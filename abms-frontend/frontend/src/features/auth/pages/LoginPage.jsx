import { useState } from 'react'
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
    <div className="auth-card login-sketch-card">
      <div className="auth-card__header login-sketch-card__header">
        <div className="login-sketch-card__close" aria-hidden="true">
          <svg viewBox="0 0 48 48" role="img" focusable="false">
            <path d="M12 42V16.5L24 7l12 9.5V42" />
            <path d="M18 42V24h12v18" />
            <path d="M17 18h2M23 18h2M29 18h2M17 24h2M29 24h2M17 30h2M29 30h2" />
          </svg>
        </div>
        <h2>ĐĂNG NHẬP</h2>
        <p>Hệ thống Quản lý Chung cư ABMS</p>
      </div>

      <form className="form-grid login-sketch-form" onSubmit={handleSubmit}>
        <label className="form-field login-sketch-field">
          <span>EMAIL</span>
          <input
            type="email"
            name="email"
            placeholder="Nhập email"
            value={formData.email}
            onChange={handleChange}
          />
          {errors.email ? <small className="field-error">{errors.email}</small> : null}
        </label>

        <label className="form-field login-sketch-field">
          <span>MẬT KHẨU</span>
          <input
            type="password"
            name="password"
            placeholder="password123"
            value={formData.password}
            onChange={handleChange}
          />
          {errors.password ? <small className="field-error">{errors.password}</small> : null}
        </label>

        <a className="login-sketch-form__forgot" href="#forgot-password">
          Quên mật khẩu?
        </a>

        {apiError ? <div className="alert alert-error">{apiError}</div> : null}

        <button type="submit" className="btn btn-block login-sketch-form__submit" disabled={isSubmitting}>
          {isSubmitting ? 'ĐANG ĐĂNG NHẬP...' : 'ĐĂNG NHẬP'}
        </button>
      </form>

      <div className="auth-card__footer login-sketch-card__footer">
        <small>2026 ABMS Management</small>
      </div>
    </div>
  )
}