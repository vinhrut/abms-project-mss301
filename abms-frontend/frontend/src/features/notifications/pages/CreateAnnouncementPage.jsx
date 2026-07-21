import { useState } from 'react'
import { Navigate, useNavigate } from 'react-router-dom'
import { PageIntro } from '../../../components/ui/PageIntro.jsx'
import { APP_ROUTES } from '../../../config/navigation.js'
import { extractApiErrorMessage } from '../../../utils/apiError.js'
import { useAuth } from '../../auth/context/useAuth.js'
import { notificationService } from '../api/notificationService.js'
import { NotificationForm } from '../components/NotificationForm.jsx'
import { canManageNotifications } from '../utils/notificationUtils.js'

const SUCCESS_CODE = 'MSG-SUC-006'

export function CreateAnnouncementPage() {
  const { auth } = useAuth()
  const navigate = useNavigate()
  const [submitting, setSubmitting] = useState(false)
  const [apiError, setApiError] = useState('')
  const [successMessage, setSuccessMessage] = useState('')

  if (!canManageNotifications(auth?.roleName)) {
    return <Navigate to={APP_ROUTES.notifications} replace />
  }

  const handleSubmit = async (payload) => {
    setApiError('')
    setSuccessMessage('')
    try {
      setSubmitting(true)
      const created = await notificationService.create(payload)
      const status = created?.status || 'PENDING_APPROVAL'
      const scheduledNote = payload.scheduledAt
        ? ` Scheduled for ${payload.scheduledAt}.`
        : ''
      setSuccessMessage(
        `${SUCCESS_CODE}: Announcement submitted successfully. Status: ${status}.${scheduledNote}`,
      )
      return true
    } catch (error) {
      setApiError(
        extractApiErrorMessage(
          error,
          'Unable to submit the announcement. Please check the form and try again.',
        ),
      )
      return false
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <div className="page-stack create-announcement-page">
      <PageIntro
        eyebrow="Notification > Create Announcement"
        title="Create Announcement"
        description="Compose and send a building announcement to selected resident groups via In-App and/or Email. Approval may be required before dispatch."
      />

      <section className="content-card create-announcement-card">
        {apiError ? <div className="alert alert-error">{apiError}</div> : null}
        {successMessage ? <div className="alert alert-success">{successMessage}</div> : null}

        <NotificationForm
          submitting={submitting}
          onSubmit={handleSubmit}
          onCancel={() => navigate(APP_ROUTES.notifications)}
        />
      </section>
    </div>
  )
}
