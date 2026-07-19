import { useState } from 'react'
import { DELIVERY_CHANNELS, NOTIFICATION_PRIORITIES } from '../utils/notificationUtils.js'

const initialForm = { title: '', content: '', priority: 'NORMAL', recipientGroup: '', channels: ['IN_APP'], recipientIds: '', scheduledAt: '' }

export function NotificationForm({ onSubmit, submitting }) {
  const [form, setForm] = useState(initialForm)
  const [errors, setErrors] = useState({})
  const change = ({ target }) => setForm((old) => ({ ...old, [target.name]: target.value }))
  const toggleChannel = (channel) => setForm((old) => ({ ...old, channels: old.channels.includes(channel) ? old.channels.filter((item) => item !== channel) : [...old.channels, channel] }))

  const submit = async (event) => {
    event.preventDefault()
    const next = {}
    if (!form.title.trim()) next.title = 'Tiêu đề là bắt buộc.'
    else if (form.title.trim().length > 255) next.title = 'Tiêu đề tối đa 255 ký tự.'
    if (!form.content.trim()) next.content = 'Nội dung là bắt buộc.'
    else if (form.content.trim().length > 2000) next.content = 'Nội dung tối đa 2000 ký tự.'
    if (!form.recipientGroup.trim()) next.recipientGroup = 'Nhóm người nhận là bắt buộc.'
    if (!form.channels.length) next.channels = 'Chọn ít nhất một kênh gửi.'
    setErrors(next)
    if (Object.keys(next).length) return
    const payload = {
      ...form,
      title: form.title.trim(), content: form.content.trim(), recipientGroup: form.recipientGroup.trim(),
      recipientIds: form.recipientIds.split(',').map((id) => id.trim()).filter(Boolean),
      scheduledAt: form.scheduledAt ? (/^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}$/.test(form.scheduledAt) ? `${form.scheduledAt}:00` : form.scheduledAt) : null,
    }
    const ok = await onSubmit(payload)
    if (ok) setForm(initialForm)
  }

  return <form className="form-grid form-grid--two-columns" onSubmit={submit}>
    <label className="form-field"><span>Tiêu đề *</span><input name="title" value={form.title} maxLength={255} onChange={change} />{errors.title && <small className="field-error">{errors.title}</small>}</label>
    <label className="form-field"><span>Độ ưu tiên</span><select name="priority" value={form.priority} onChange={change}>{NOTIFICATION_PRIORITIES.map((item) => <option key={item}>{item}</option>)}</select></label>
    <label className="form-field form-field--full"><span>Nội dung *</span><textarea name="content" rows="4" value={form.content} maxLength={2000} onChange={change} />{errors.content && <small className="field-error">{errors.content}</small>}</label>
    <label className="form-field"><span>Nhóm người nhận *</span><input name="recipientGroup" value={form.recipientGroup} onChange={change} placeholder="ALL, RESIDENT..." />{errors.recipientGroup && <small className="field-error">{errors.recipientGroup}</small>}</label>
    <label className="form-field"><span>Hẹn giờ gửi</span><input type="datetime-local" step="1" name="scheduledAt" value={form.scheduledAt} onChange={change} /></label>
    <label className="form-field form-field--full"><span>UUID người nhận (phân cách bằng dấu phẩy)</span><input name="recipientIds" value={form.recipientIds} onChange={change} /></label>
    <div className="form-field form-field--full"><span>Kênh gửi *</span><div className="checkbox-row">{DELIVERY_CHANNELS.map((item) => <label key={item}><input type="checkbox" checked={form.channels.includes(item)} onChange={() => toggleChannel(item)} /> {item}</label>)}</div>{errors.channels && <small className="field-error">{errors.channels}</small>}</div>
    <div className="form-actions form-field--full"><button className="btn btn-primary" disabled={submitting}>{submitting ? 'Đang tạo...' : 'Tạo thông báo'}</button></div>
  </form>
}
