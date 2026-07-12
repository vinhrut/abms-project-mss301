import { Link } from 'react-router-dom'
import { useEffect, useState } from 'react'
import { useAuth } from '../context/useAuth.js'
import { validateRegisterForm } from '../utils/authValidation.js'
import { extractApiErrorMessage } from '../../../utils/apiError.js'
import { apartmentService } from '../../../services/apartmentService.js'

const initialForm = {
  email: '',
  password: '',
  fullName: '',
  phone: '',
  idCard: '',
  apartmentId: '',
  relationship: 'OWNER',
  residenceType: 'PERMANENT',
}

export function RegisterPage() {
  const [formData, setFormData] = useState(initialForm)
  const [errors, setErrors] = useState({})
  const [apiError, setApiError] = useState('')
  const [isSubmitting, setIsSubmitting] = useState(false)
  const [apartments, setApartments] = useState([])
  const [loadingApartments, setLoadingApartments] = useState(false)
  const [successMessage, setSuccessMessage] = useState('')
  const { register } = useAuth()

  useEffect(() => {
    const loadApartments = async () => {
      try {
        setLoadingApartments(true)
        const data = await apartmentService.getAllApartments()
        setApartments(Array.isArray(data) ? data : [])
      } catch {
        setApartments([])
      } finally {
        setLoadingApartments(false)
      }
    }

    loadApartments()
  }, [])

  const handleChange = (event) => {
    const { name, value } = event.target
    setFormData((prev) => ({ ...prev, [name]: value }))
    setErrors((prev) => ({ ...prev, [name]: '' }))
    setApiError('')
    setSuccessMessage('')
  }

  const handleSubmit = async (event) => {
    event.preventDefault()

    const validationErrors = validateRegisterForm(formData)
    setErrors(validationErrors)

    if (Object.keys(validationErrors).length > 0) {
      return
    }

    try {
      setIsSubmitting(true)
      const response = await register(formData)
      setSuccessMessage(
        response?.message || 'Đăng ký thành công. Tài khoản đang chờ manager phê duyệt.',
      )
      setFormData(initialForm)
    } catch (error) {
      setApiError(extractApiErrorMessage(error, 'Không thể đăng ký. Vui lòng thử lại.'))
    } finally {
      setIsSubmitting(false)
    }
  }

  return (
    <div className="auth-card auth-card--wide">
      <div className="auth-card__header">
        <span className="eyebrow">Resident onboarding</span>
        <h2>Tạo tài khoản ABMS</h2>
        <p>Tạo tài khoản cư dân để đăng nhập, đăng ký xe và theo dõi trạng thái duyệt xe.</p>
      </div>

      <form className="form-grid form-grid--two-columns" onSubmit={handleSubmit}>
        <label className="form-field">
          <span>Họ và tên *</span>
          <input name="fullName" value={formData.fullName} onChange={handleChange} />
          {errors.fullName ? <small className="field-error">{errors.fullName}</small> : null}
        </label>

        <label className="form-field">
          <span>Email *</span>
          <input type="email" name="email" value={formData.email} onChange={handleChange} />
          {errors.email ? <small className="field-error">{errors.email}</small> : null}
        </label>

        <label className="form-field">
          <span>Password *</span>
          <input
            type="password"
            name="password"
            value={formData.password}
            onChange={handleChange}
          />
          {errors.password ? <small className="field-error">{errors.password}</small> : null}
          <small>Mật khẩu nên có ít nhất 8 ký tự để dễ dùng lâu dài.</small>
        </label>

        <label className="form-field">
          <span>Số điện thoại</span>
          <input name="phone" value={formData.phone} onChange={handleChange} />
          {errors.phone ? <small className="field-error">{errors.phone}</small> : null}
        </label>

        <label className="form-field form-field--full">
          <span>Số CCCD / ID card</span>
          <input name="idCard" value={formData.idCard} onChange={handleChange} />
          {errors.idCard ? <small className="field-error">{errors.idCard}</small> : null}
        </label>

        <label className="form-field form-field--full">
          <span>Căn hộ *</span>
          <select name="apartmentId" value={formData.apartmentId} onChange={handleChange}>
            <option value="">{loadingApartments ? 'Đang tải căn hộ...' : 'Chọn căn hộ'}</option>
            {apartments.map((apartment) => (
              <option key={apartment.apartmentId} value={apartment.apartmentId}>
                {apartment.roomNumber} - {apartment.apartmentId}
              </option>
            ))}
          </select>
          {errors.apartmentId ? <small className="field-error">{errors.apartmentId}</small> : null}
        </label>

        <label className="form-field">
          <span>Quan hệ cư trú</span>
          <select name="relationship" value={formData.relationship} onChange={handleChange}>
            <option value="OWNER">Owner</option>
            <option value="TENANT">Tenant</option>
            <option value="FAMILY">Family</option>
          </select>
        </label>

        <label className="form-field">
          <span>Loại cư trú</span>
          <select name="residenceType" value={formData.residenceType} onChange={handleChange}>
            <option value="PERMANENT">Permanent</option>
            <option value="TEMPORARY">Temporary</option>
          </select>
        </label>

        {apiError ? <div className="alert alert-error form-field--full">{apiError}</div> : null}
        {successMessage ? (
          <div className="alert alert-success form-field--full">{successMessage}</div>
        ) : null}

        <div className="form-actions form-field--full">
          <button type="submit" className="btn btn-primary" disabled={isSubmitting}>
            {isSubmitting ? 'Đang tạo tài khoản...' : 'Register'}
          </button>
          <Link to="/login" className="btn btn-ghost">
            Quay lại đăng nhập
          </Link>
        </div>

        <div className="auth-inline-note form-field--full">
          Tài khoản đăng ký từ màn hình này sẽ ở trạng thái <strong>chờ manager duyệt</strong>, không đăng nhập ngay như trước.
        </div>
      </form>
    </div>
  )
}