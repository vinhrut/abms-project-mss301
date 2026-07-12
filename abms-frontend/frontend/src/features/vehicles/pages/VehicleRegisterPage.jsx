import { useMemo, useState } from 'react'
import { useAuth } from '../../auth/context/useAuth.js'
import { vehicleService } from '../../../services/vehicleService.js'
import { validateVehicleForm } from '../utils/vehicleValidation.js'
import { extractApiErrorMessage } from '../../../utils/apiError.js'
import { apartmentService } from '../../../services/apartmentService.js'
import { useEffect } from 'react'

const initialState = {
  apartmentId: '',
  ownerId: '',
  licensePlate: '',
  type: 'MOTORBIKE',
  brand: '',
}

export function VehicleRegisterPage() {
  const { auth } = useAuth()
  const [formData, setFormData] = useState(initialState)
  const [errors, setErrors] = useState({})
  const [isSubmitting, setIsSubmitting] = useState(false)
  const [apiError, setApiError] = useState('')
  const [successMessage, setSuccessMessage] = useState('')
  const [apartments, setApartments] = useState([])
  const [loadingApartments, setLoadingApartments] = useState(false)

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

  const roleNote = useMemo(
    () =>
      ['STAFF', 'MANAGER'].includes(auth?.roleName)
        ? 'Bạn đang dùng tài khoản quản lý. Màn hình này vẫn cho phép tạo thử request để test end-to-end flow.'
        : 'Yêu cầu sẽ được tạo với trạng thái chờ duyệt (PENDING) và chỉ có hiệu lực sau khi staff/manager phê duyệt.',
    [auth?.roleName],
  )

  const resolvedOwnerId = auth?.userId || formData.ownerId

  const handleChange = (event) => {
    const { name, value } = event.target
    setFormData((prev) => ({ ...prev, [name]: value }))
    setErrors((prev) => ({ ...prev, [name]: '' }))
    setApiError('')
    setSuccessMessage('')
  }

  const handleSubmit = async (event) => {
    event.preventDefault()

    const payload = {
      ...formData,
      ownerId: resolvedOwnerId,
      licensePlate: formData.licensePlate.trim().toUpperCase(),
    }

    const validationErrors = validateVehicleForm(payload)
    setErrors(validationErrors)

    if (Object.keys(validationErrors).length > 0) {
      return
    }

    try {
      setIsSubmitting(true)
      const response = await vehicleService.registerVehicle(payload)
      setSuccessMessage(
        `Đã gửi yêu cầu đăng ký xe ${response.licensePlate || formData.licensePlate} thành công. Trạng thái hiện tại: ${response.status || 'PENDING'}.`,
      )
      setFormData(initialState)
    } catch (error) {
      setApiError(
        extractApiErrorMessage(
          error,
          'Không thể gửi yêu cầu đăng ký xe. Vui lòng kiểm tra dữ liệu và thử lại.',
        ),
      )
    } finally {
      setIsSubmitting(false)
    }
  }

  return (
    <div className="page-stack">
      <section className="page-header-card">
        <div>
          <span className="eyebrow">Vehicle registration</span>
          <h1>Đăng ký phương tiện</h1>
          <p>{roleNote}</p>
        </div>
      </section>

      <section className="content-card">
        <div className="info-grid">
          <article className="info-card">
            <strong>Luồng cư dân</strong>
            <p>Điền thông tin xe, gửi yêu cầu và chờ staff/manager duyệt.</p>
          </article>
          <article className="info-card">
            <strong>Business rule</strong>
            <p>Mỗi căn hộ chỉ được duyệt tối đa theo giới hạn xe đã cấu hình ở backend.</p>
          </article>
        </div>

        <form className="form-grid form-grid--two-columns" onSubmit={handleSubmit}>
          <label className="form-field">
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
            <span>Owner ID *</span>
            <input
              name="ownerId"
              placeholder="22222222-2222-2222-2222-222222222222"
              value={resolvedOwnerId || ''}
              onChange={handleChange}
              disabled={Boolean(auth?.userId)}
            />
            <small>
              {auth?.userId
                ? 'Tự động lấy từ tài khoản đang đăng nhập. Backend sẽ kiểm tra bạn có thuộc căn hộ đã chọn hay không.'
                : 'Nhập owner UUID hợp lệ.'}
            </small>
            {errors.ownerId ? <small className="field-error">{errors.ownerId}</small> : null}
          </label>

          <label className="form-field">
            <span>License plate *</span>
            <input
              name="licensePlate"
              placeholder="59A-12345"
              value={formData.licensePlate}
              onChange={handleChange}
            />
            <small>Ví dụ: 59A-12345, 30G1-67890</small>
            {errors.licensePlate ? <small className="field-error">{errors.licensePlate}</small> : null}
          </label>

          <label className="form-field">
            <span>Vehicle type *</span>
            <select name="type" value={formData.type} onChange={handleChange}>
              <option value="MOTORBIKE">Motorbike</option>
              <option value="CAR">Car</option>
            </select>
            {errors.type ? <small className="field-error">{errors.type}</small> : null}
          </label>

          <label className="form-field form-field--full">
            <span>Brand *</span>
            <input name="brand" placeholder="Honda" value={formData.brand} onChange={handleChange} />
            {errors.brand ? <small className="field-error">{errors.brand}</small> : null}
          </label>

          {apiError ? <div className="alert alert-error form-field--full">{apiError}</div> : null}
          {successMessage ? (
            <div className="alert alert-success form-field--full">{successMessage}</div>
          ) : null}

          <div className="form-actions form-field--full">
            <button type="submit" className="btn btn-primary" disabled={isSubmitting}>
              {isSubmitting ? 'Đang gửi yêu cầu...' : 'Gửi yêu cầu đăng ký'}
            </button>
            <button
              type="button"
              className="btn btn-ghost"
              onClick={() => {
                setFormData(initialState)
                setErrors({})
                setApiError('')
                setSuccessMessage('')
              }}
            >
              Clear form
            </button>
          </div>
        </form>
      </section>
    </div>
  )
}