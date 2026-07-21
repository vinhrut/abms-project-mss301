import { useEffect, useMemo, useRef, useState } from 'react'
import { apartmentService } from '../../../services/apartmentService.js'
import { maintenanceService } from '../../../services/maintenanceService.js'
import { extractApiErrorMessage } from '../../../utils/apiError.js'
import { useAuth } from '../../auth/context/useAuth.js'
import { validateMaintenanceForm } from '../utils/maintenanceValidation.js'

const initialState = {
  category: '',
  priority: 'NORMAL',
  title: '',
  description: '',
}

const MAX_PHOTO_SIZE = 10 * 1024 * 1024
const ALLOWED_PHOTO_TYPES = ['image/jpeg', 'image/png']

export function MaintenanceSubmitModal({ open, onClose, onSubmitted }) {
  const { auth } = useAuth()
  const fileInputRef = useRef(null)
  const [formData, setFormData] = useState(initialState)
  const [errors, setErrors] = useState({})
  const [apiError, setApiError] = useState('')
  const [isSubmitting, setIsSubmitting] = useState(false)
  const [roomNumber, setRoomNumber] = useState('')
  const [photos, setPhotos] = useState([])

  useEffect(() => {
    if (!open) {
      return
    }

    setFormData(initialState)
    setErrors({})
    setApiError('')
    setPhotos([])

    const loadApartment = async () => {
      try {
        const apartments = await apartmentService.getMyApartments()
        const first = Array.isArray(apartments) ? apartments[0] : null
        setRoomNumber(first?.roomNumber || '—')
      } catch {
        setRoomNumber('—')
      }
    }

    loadApartment()
  }, [open, auth?.userId])

  useEffect(() => {
    if (!open) {
      return undefined
    }

    const handleEscape = (event) => {
      if (event.key === 'Escape' && !isSubmitting) {
        onClose()
      }
    }

    document.addEventListener('keydown', handleEscape)
    return () => document.removeEventListener('keydown', handleEscape)
  }, [open, isSubmitting, onClose])

  const canSubmit = useMemo(() => {
    return Boolean(formData.category && formData.priority && formData.title.trim())
  }, [formData])

  const handleChange = (event) => {
    const { name, value } = event.target
    setFormData((prev) => ({ ...prev, [name]: value }))
    setErrors((prev) => ({ ...prev, [name]: '' }))
    setApiError('')
  }

  const handlePriorityChange = (priority) => {
    setFormData((prev) => ({ ...prev, priority }))
    setErrors((prev) => ({ ...prev, priority: '' }))
    setApiError('')
  }

  const handlePhotoSelect = (event) => {
    const files = Array.from(event.target.files || [])
    const nextErrors = { ...errors }
    delete nextErrors.photos

    const validFiles = []
    for (const file of files) {
      if (!ALLOWED_PHOTO_TYPES.includes(file.type)) {
        nextErrors.photos = 'Chỉ chấp nhận JPG/PNG.'
        continue
      }
      if (file.size > MAX_PHOTO_SIZE) {
        nextErrors.photos = 'Mỗi ảnh tối đa 10MB.'
        continue
      }
      validFiles.push(file)
    }

    setErrors(nextErrors)
    if (validFiles.length > 0) {
      setPhotos((prev) => [...prev, ...validFiles])
    }
    event.target.value = ''
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
      onSubmitted?.(response)
      onClose()
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

  if (!open) {
    return null
  }

  return (
    <div className="modal-backdrop maint-submit-backdrop" role="presentation" onMouseDown={onClose}>
      <section
        className="maint-submit-modal"
        role="dialog"
        aria-modal="true"
        aria-label="Submit Maintenance Request"
        onMouseDown={(event) => event.stopPropagation()}
      >
        <header className="maint-submit-modal__header">
          <div>
            <span className="maint-submit-modal__breadcrumb">Resident Portal &gt;</span>
            <h2>Submit Maintenance Request</h2>
          </div>
          <button type="button" className="btn btn-ghost" onClick={onClose} disabled={isSubmitting}>
            ✕
          </button>
        </header>

        <form className="maint-submit-form" onSubmit={handleSubmit}>
          <div className="maint-submit-row">
            <label className="maint-submit-label">
              APARTMENT <span className="required">*</span>
            </label>
            <input className="maint-submit-input maint-submit-input--readonly" value={roomNumber} readOnly />
          </div>

          <div className="maint-submit-row">
            <label className="maint-submit-label" htmlFor="maint-category">
              CATEGORY <span className="required">*</span>
            </label>
            <div className="maint-submit-field">
              <select
                id="maint-category"
                name="category"
                value={formData.category}
                onChange={handleChange}
              >
                <option value="">Select category...</option>
                <option value="PLUMBING">Plumbing</option>
                <option value="ELECTRICAL">Electrical</option>
                <option value="HVAC">HVAC</option>
                <option value="CIVIL">Civil</option>
                <option value="OTHER">Other</option>
              </select>
              {errors.category ? <small className="field-error">{errors.category}</small> : null}
            </div>
          </div>

          <div className="maint-submit-row">
            <span className="maint-submit-label">
              PRIORITY <span className="required">*</span>
            </span>
            <div className="maint-submit-field maint-submit-priority">
              <label>
                <input
                  type="radio"
                  name="priority"
                  checked={formData.priority === 'NORMAL'}
                  onChange={() => handlePriorityChange('NORMAL')}
                />
                Normal
              </label>
              <label>
                <input
                  type="radio"
                  name="priority"
                  checked={formData.priority === 'EMERGENCY'}
                  onChange={() => handlePriorityChange('EMERGENCY')}
                />
                Emergency
              </label>
              {errors.priority ? <small className="field-error">{errors.priority}</small> : null}
            </div>
          </div>

          <div className="maint-submit-row">
            <label className="maint-submit-label" htmlFor="maint-title">
              TITLE <span className="required">*</span>
            </label>
            <div className="maint-submit-field">
              <input
                id="maint-title"
                name="title"
                value={formData.title}
                onChange={handleChange}
                placeholder="Enter a short description of the issue..."
                maxLength={200}
              />
              {errors.title ? <small className="field-error">{errors.title}</small> : null}
            </div>
          </div>

          <div className="maint-submit-row">
            <label className="maint-submit-label" htmlFor="maint-description">
              DESCRIPTION <span className="optional">Optional</span>
            </label>
            <div className="maint-submit-field">
              <textarea
                id="maint-description"
                name="description"
                value={formData.description}
                onChange={handleChange}
                placeholder="Provide additional details about the issue..."
                rows={4}
                maxLength={2000}
              />
              {errors.description ? <small className="field-error">{errors.description}</small> : null}
            </div>
          </div>

          <div className="maint-submit-row">
            <span className="maint-submit-label">
              PHOTOS <span className="optional">Optional · JPG/PNG · max 10MB</span>
            </span>
            <div className="maint-submit-field">
              <div className="maint-submit-photos">
                {photos.map((photo) => (
                  <span key={`${photo.name}-${photo.lastModified}`} className="maint-submit-photo-chip">
                    {photo.name}
                  </span>
                ))}
                <button
                  type="button"
                  className="maint-submit-photo-add"
                  onClick={() => fileInputRef.current?.click()}
                >
                  <span aria-hidden="true">+</span>
                  ADD
                </button>
                <input
                  ref={fileInputRef}
                  type="file"
                  accept="image/jpeg,image/png"
                  multiple
                  hidden
                  onChange={handlePhotoSelect}
                />
              </div>
              {errors.photos ? <small className="field-error">{errors.photos}</small> : null}
            </div>
          </div>

          {apiError ? <div className="alert alert-error">{apiError}</div> : null}

          <footer className="maint-submit-modal__footer">
            <button type="button" className="btn btn-secondary" onClick={onClose} disabled={isSubmitting}>
              Cancel
            </button>
            <button
              type="submit"
              className="btn btn-primary"
              disabled={isSubmitting || !canSubmit}
            >
              {isSubmitting ? 'Submitting...' : 'Submit Request'}
            </button>
          </footer>
        </form>
      </section>
    </div>
  )
}
