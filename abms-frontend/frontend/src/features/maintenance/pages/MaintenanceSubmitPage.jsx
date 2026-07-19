import { useMemo, useState } from 'react'
import { Link } from 'react-router-dom'
import { maintenanceService } from '../../../services/maintenanceService.js'
import { extractApiErrorMessage } from '../../../utils/apiError.js'
import { APP_ROUTES } from '../../../config/navigation.js'
import { useAuth } from '../../auth/context/useAuth.js'
import { validateMaintenanceForm } from '../utils/maintenanceValidation.js'

const initialState = {
  category: 'PLUMBING',
  priority: 'NORMAL',
  title: '',
  description: '',
}

export function MaintenanceSubmitPage() {
  const { auth } = useAuth()
  const [formData, setFormData] = useState(initialState)
  const [errors, setErrors] = useState({})
  const [apiError, setApiError] = useState('')
  const [successMessage, setSuccessMessage] = useState('')
  const [isSubmitting, setIsSubmitting] = useState(false)

  const note = useMemo(
    () =>
      'Căn hộ sẽ được lấy tự động từ residence đang active của bạn. Yêu cầu tạo với trạng thái OPEN để Building Manager phân công.',
    [],
  )

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
      senderId: auth?.userId,
      title: formData.title.trim(),
      description: formData.description.trim(),
    }

    const validationErrors = validateMaintenanceForm(payload)
    setErrors(validationErrors)
    if (Object.keys(validationErrors).length > 0) {
      return
    }

    try {
      setIsSubmitting(true)
      const response = await maintenanceService.submitRequest(payload)
      setSuccessMessage(
        `Đã gửi yêu cầu ${response.requestCode || ''} thành công. Trạng thái: ${response.status || 'OPEN'}.`,
      )
      setFormData(initialState)
    } catch (error) {
      setApiError(
        extractApiErrorMessage(
          error,
          'Không thể gửi yêu cầu bảo trì. Vui lòng kiểm tra dữ liệu và thử lại.',
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
          <span className="eyebrow">Maintenance</span>
          <h1>Gửi yêu cầu bảo trì</h1>
          <p>{note}</p>
        </div>
        <Link className="btn btn-secondary" to={APP_ROUTES.maintenance}>
          Quay lại danh sách
        </Link>
      </section>

      <section className="content-card">
        <form className="form-grid" onSubmit={handleSubmit}>
          <label className="form-field">
            <span>Hạng mục</span>
            <select name="category" value={formData.category} onChange={handleChange}>
              <option value="PLUMBING">Plumbing</option>
              <option value="ELECTRICAL">Electrical</option>
              <option value="HVAC">HVAC</option>
              <option value="CIVIL">Civil</option>
              <option value="OTHER">Other</option>
            </select>
            {errors.category ? <small className="field-error">{errors.category}</small> : null}
          </label>

          <label className="form-field">
            <span>Mức ưu tiên</span>
            <select name="priority" value={formData.priority} onChange={handleChange}>
              <option value="NORMAL">Normal</option>
              <option value="EMERGENCY">Emergency</option>
            </select>
            {errors.priority ? <small className="field-error">{errors.priority}</small> : null}
          </label>

          <label className="form-field form-field-full">
            <span>Tiêu đề</span>
            <input
              name="title"
              value={formData.title}
              onChange={handleChange}
              placeholder="Ví dụ: Rò nước nhà tắm"
              maxLength={200}
            />
            {errors.title ? <small className="field-error">{errors.title}</small> : null}
          </label>

          <label className="form-field form-field-full">
            <span>Mô tả</span>
            <textarea
              name="description"
              value={formData.description}
              onChange={handleChange}
              placeholder="Mô tả thêm tình trạng (không bắt buộc)"
              rows={4}
              maxLength={2000}
            />
            {errors.description ? <small className="field-error">{errors.description}</small> : null}
          </label>

          {apiError ? <div className="alert alert-error">{apiError}</div> : null}
          {successMessage ? <div className="alert alert-success">{successMessage}</div> : null}

          <div className="toolbar-actions">
            <button type="submit" className="btn btn-primary" disabled={isSubmitting}>
              {isSubmitting ? 'Đang gửi...' : 'Gửi yêu cầu'}
            </button>
          </div>
        </form>
      </section>
    </div>
  )
}
