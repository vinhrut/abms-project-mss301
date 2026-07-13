import { useState } from 'react'
import { PageIntro } from '../../../components/ui/PageIntro.jsx'
import { authService } from '../../../services/authService.js'
import { extractApiErrorMessage } from '../../../utils/apiError.js'

export function ChangePasswordPage() {
  const [formData, setFormData] = useState({
    currentPassword: '',
    newPassword: '',
    confirmPassword: '',
  })
  const [submitting, setSubmitting] = useState(false)
  const [error, setError] = useState('')
  const [message, setMessage] = useState('')

  const handleChange = (event) => {
    const { name, value } = event.target
    setFormData((prev) => ({ ...prev, [name]: value }))
    setError('')
    setMessage('')
  }

  const handleSubmit = async (event) => {
    event.preventDefault()

    if (!formData.currentPassword || !formData.newPassword || !formData.confirmPassword) {
      setError('Vui lòng nhập đầy đủ current password, new password và confirm password.')
      return
    }

    if (formData.newPassword !== formData.confirmPassword) {
      setError('New password và confirm password không khớp.')
      return
    }

    try {
      setSubmitting(true)
      const response = await authService.changePassword({
        currentPassword: formData.currentPassword,
        newPassword: formData.newPassword,
      })
      setMessage(response?.message || 'Đổi mật khẩu thành công.')
      setFormData({ currentPassword: '', newPassword: '', confirmPassword: '' })
    } catch (apiError) {
      setError(extractApiErrorMessage(apiError, 'Không thể đổi mật khẩu.'))
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <div className="page-stack">
      <PageIntro
        eyebrow="Account security"
        title="Change Password"
        description="Tất cả role đều có thể chủ động đổi mật khẩu của chính mình theo nghiệp vụ backend mới."
      />

      <section className="content-card">
        <form className="form-grid" onSubmit={handleSubmit}>
          <label className="form-field">
            <span>Current password *</span>
            <input type="password" name="currentPassword" value={formData.currentPassword} onChange={handleChange} />
          </label>

          <label className="form-field">
            <span>New password *</span>
            <input type="password" name="newPassword" value={formData.newPassword} onChange={handleChange} />
          </label>

          <label className="form-field">
            <span>Confirm new password *</span>
            <input type="password" name="confirmPassword" value={formData.confirmPassword} onChange={handleChange} />
          </label>

          {error ? <div className="alert alert-error">{error}</div> : null}
          {message ? <div className="alert alert-success">{message}</div> : null}

          <div className="form-actions">
            <button type="submit" className="btn btn-primary" disabled={submitting}>
              {submitting ? 'Đang đổi mật khẩu...' : 'Đổi mật khẩu'}
            </button>
          </div>
        </form>
      </section>
    </div>
  )
}