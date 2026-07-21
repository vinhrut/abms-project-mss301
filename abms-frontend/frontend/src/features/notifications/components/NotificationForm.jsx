import { useState } from 'react'
import { DELIVERY_CHANNELS, NOTIFICATION_PRIORITIES } from '../utils/notificationUtils.js'

const RECIPIENT_GROUPS = [
  { value: 'ALL', label: 'All Residents' },
  { value: 'RESIDENT', label: 'RESIDENT' },
  { value: 'STAFF', label: 'STAFF' },
  { value: 'TECHNICIAN', label: 'TECHNICIAN' },
  { value: 'USERS', label: 'USERS' },
]

const PRIORITY_LABELS = {
  LOW: 'Low',
  NORMAL: 'Normal',
  HIGH: 'High',
  URGENT: 'Urgent',
}

const CHANNEL_LABELS = {
  IN_APP: 'In-App',
  EMAIL: 'Email',
}

const initialForm = {
  title: '',
  content: '',
  priority: 'NORMAL',
  recipientGroup: 'ALL',
  recipientFilter: '',
  channels: ['IN_APP'],
  scheduledAt: '',
}

function buildPayload(form) {
  const recipientIds = form.recipientFilter
    .split(',')
    .map((id) => id.trim())
    .filter(Boolean)

  return {
    title: form.title.trim(),
    content: form.content.trim(),
    priority: form.priority,
    recipientGroup: form.recipientGroup,
    channels: form.channels,
    recipientIds,
    scheduledAt: form.scheduledAt
      ? /^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}$/.test(form.scheduledAt)
        ? `${form.scheduledAt}:00`
        : form.scheduledAt
      : null,
  }
}

function validate(form) {
  const next = {}
  if (!form.title.trim()) next.title = 'Title is required.'
  else if (form.title.trim().length > 255) next.title = 'Title must be at most 255 characters.'
  if (!form.content.trim()) next.content = 'Content is required.'
  else if (form.content.trim().length > 2000) next.content = 'Content must be at most 2000 characters.'
  if (!form.recipientGroup) next.recipientGroup = 'Recipient group is required.'
  if (!form.channels.length) next.channels = 'Select at least one channel.'
  if (form.scheduledAt) {
    const scheduled = new Date(form.scheduledAt)
    if (Number.isNaN(scheduled.getTime())) next.scheduledAt = 'Invalid schedule datetime.'
    else if (scheduled.getTime() < Date.now() - 60_000) {
      next.scheduledAt = 'Schedule time must be in the present or future.'
    }
  }
  return next
}

export function NotificationForm({ onSubmit, submitting, onCancel }) {
  const [form, setForm] = useState(initialForm)
  const [errors, setErrors] = useState({})
  const [previewOpen, setPreviewOpen] = useState(false)

  const change = ({ target }) => {
    setForm((old) => ({ ...old, [target.name]: target.value }))
    setErrors((old) => ({ ...old, [target.name]: '' }))
  }

  const toggleChannel = (channel) => {
    setForm((old) => ({
      ...old,
      channels: old.channels.includes(channel)
        ? old.channels.filter((item) => item !== channel)
        : [...old.channels, channel],
    }))
    setErrors((old) => ({ ...old, channels: '' }))
  }

  const openPreview = () => {
    const next = validate(form)
    setErrors(next)
    if (Object.keys(next).length) return
    setPreviewOpen(true)
  }

  const submit = async (event) => {
    event.preventDefault()
    const next = validate(form)
    setErrors(next)
    if (Object.keys(next).length) return

    const ok = await onSubmit(buildPayload(form))
    if (ok) {
      setForm(initialForm)
      setPreviewOpen(false)
    }
  }

  return (
    <>
      <form className="form-grid announcement-form" onSubmit={submit} noValidate>
        <label className="form-field form-field--full">
          <span>Title</span>
          <input
            name="title"
            value={form.title}
            maxLength={255}
            onChange={change}
            placeholder="Announcement title"
            aria-invalid={Boolean(errors.title)}
          />
          {errors.title ? <small className="field-error">{errors.title}</small> : null}
        </label>

        <label className="form-field form-field--full">
          <span>Content</span>
          <textarea
            name="content"
            rows={6}
            value={form.content}
            maxLength={2000}
            onChange={change}
            placeholder="Write the announcement content..."
            aria-invalid={Boolean(errors.content)}
          />
          {errors.content ? <small className="field-error">{errors.content}</small> : null}
        </label>

        <fieldset className="form-field form-field--full announcement-fieldset">
          <legend>Priority</legend>
          <div className="choice-row" role="radiogroup" aria-label="Priority">
            {NOTIFICATION_PRIORITIES.map((item) => (
              <label key={item} className="choice-option">
                <input
                  type="radio"
                  name="priority"
                  value={item}
                  checked={form.priority === item}
                  onChange={change}
                />
                {PRIORITY_LABELS[item] || item}
              </label>
            ))}
          </div>
        </fieldset>

        <div className="announcement-split form-field--full">
          <div className="announcement-split__group">
            <label className="form-field">
              <span>Recipient Group</span>
              <select name="recipientGroup" value={form.recipientGroup} onChange={change}>
                {RECIPIENT_GROUPS.map((item) => (
                  <option key={item.value} value={item.value}>
                    {item.label}
                  </option>
                ))}
              </select>
              {errors.recipientGroup ? (
                <small className="field-error">{errors.recipientGroup}</small>
              ) : null}
            </label>
            <label className="form-field">
              <span>Specific recipients (optional)</span>
              <input
                name="recipientFilter"
                value={form.recipientFilter}
                onChange={change}
                placeholder="UUID list, comma-separated"
              />
            </label>
          </div>

          <fieldset className="form-field announcement-fieldset">
            <legend>Channels</legend>
            <div className="choice-row">
              {DELIVERY_CHANNELS.map((item) => (
                <label key={item} className="choice-option">
                  <input
                    type="checkbox"
                    checked={form.channels.includes(item)}
                    onChange={() => toggleChannel(item)}
                  />
                  {CHANNEL_LABELS[item] || item}
                </label>
              ))}
            </div>
            {errors.channels ? <small className="field-error">{errors.channels}</small> : null}
          </fieldset>
        </div>

        <label className="form-field form-field--full">
          <span>Schedule send (optional)</span>
          <input
            type="datetime-local"
            step="1"
            name="scheduledAt"
            value={form.scheduledAt}
            onChange={change}
            aria-invalid={Boolean(errors.scheduledAt)}
          />
          {errors.scheduledAt ? <small className="field-error">{errors.scheduledAt}</small> : null}
          <small className="field-hint">Leave empty to submit for approval and send after approval.</small>
        </label>

        <div className="form-actions form-field--full">
          <button type="button" className="btn btn-secondary" onClick={openPreview} disabled={submitting}>
            Preview
          </button>
          <button type="submit" className="btn btn-primary" disabled={submitting}>
            {submitting ? 'Submitting...' : 'Submit'}
          </button>
          <button type="button" className="btn btn-secondary" onClick={onCancel} disabled={submitting}>
            Cancel
          </button>
        </div>
      </form>

      {previewOpen ? (
        <div className="modal-backdrop" role="presentation" onClick={() => setPreviewOpen(false)}>
          <section
            className="modal-card announcement-preview-modal"
            role="dialog"
            aria-modal="true"
            aria-labelledby="announcement-preview-title"
            onClick={(event) => event.stopPropagation()}
          >
            <h2 id="announcement-preview-title">Preview Announcement</h2>
            <dl className="announcement-preview-meta">
              <div>
                <dt>Priority</dt>
                <dd>{PRIORITY_LABELS[form.priority] || form.priority}</dd>
              </div>
              <div>
                <dt>Recipient Group</dt>
                <dd>
                  {RECIPIENT_GROUPS.find((item) => item.value === form.recipientGroup)?.label ||
                    form.recipientGroup}
                </dd>
              </div>
              <div>
                <dt>Channels</dt>
                <dd>{form.channels.map((item) => CHANNEL_LABELS[item] || item).join(', ')}</dd>
              </div>
              <div>
                <dt>Schedule</dt>
                <dd>{form.scheduledAt || 'Send after approval'}</dd>
              </div>
            </dl>
            <h3>{form.title.trim()}</h3>
            <p className="announcement-preview-body">{form.content.trim()}</p>
            {form.recipientFilter.trim() ? (
              <p>
                <small>Specific recipients: {form.recipientFilter.trim()}</small>
              </p>
            ) : null}
            <div className="modal-actions">
              <button type="button" className="btn btn-secondary" onClick={() => setPreviewOpen(false)}>
                Close
              </button>
              <button
                type="button"
                className="btn btn-primary"
                disabled={submitting}
                onClick={() => submit({ preventDefault() {} })}
              >
                {submitting ? 'Submitting...' : 'Submit'}
              </button>
            </div>
          </section>
        </div>
      ) : null}
    </>
  )
}
