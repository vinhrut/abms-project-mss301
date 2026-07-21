/* eslint-disable react-hooks/set-state-in-effect */
import { useCallback, useEffect, useState } from 'react'
import { PageIntro } from '../../../components/ui/PageIntro.jsx'
import { notificationService } from '../api/notificationService.js'
import { extractApiErrorMessage } from '../../../utils/apiError.js'
import { isRoleIn, ROLE_KEYS, getRoleLabel, normalizeRole } from '../../../config/roles.js'
import { useAuth } from '../../auth/context/useAuth.js'
import { formatDateTime, NOTIFICATION_TYPES } from '../utils/notificationUtils.js'

const STATUSES = [
  'SENT',
  'PENDING_APPRO  VAL',
  'APPROVED',
  'SCHEDULED',
  'PROCESSING',
  'PARTIAL_FAILED',
  'FAILED',
  'REJECTED',
  'CANCELLED',
  'PENDING',
]

const MANAGER_RECIPIENT_GROUPS = [
  { value: 'ALL', label: 'Chung (ALL)' },
  { value: 'RESIDENT', label: 'RESIDENT' },
  { value: 'STAFF', label: 'STAFF' },
  { value: 'TECHNICIAN', label: 'TECHNICIAN' },
  { value: 'USERS', label: 'USERS' },
]

const emptyDraft = { type: '', status: '', from: '', to: '', recipient: '' }

const badge = (value) => (
  <span className={`badge badge-${String(value || '').toLowerCase()}`}>{value || '-'}</span>
)

const readBadge = (read) => (
  <span className={`notification-read-badge ${read ? 'is-read' : 'is-unread'}`}>
    {read ? 'Đã đọc' : 'Chưa đọc'}
  </span>
)

function recipientOptionsForRole(roleName, isManager) {
  if (isManager) {
    return MANAGER_RECIPIENT_GROUPS
  }
  const role = normalizeRole(roleName)
  return [
    { value: 'ALL', label: 'Chung (ALL)' },
    { value: role, label: `${getRoleLabel(role)} (${role})` },
  ]
}

export function NotificationListPage() {
  const { auth } = useAuth()
  const admin = isRoleIn(auth?.roleName, [ROLE_KEYS.ADMIN, ROLE_KEYS.MANAGER])
  const recipientOptions = recipientOptionsForRole(auth?.roleName, admin)
  const statusOptions = admin ? STATUSES : ['SENT']

  const [draft, setDraft] = useState(emptyDraft)
  const [query, setQuery] = useState({ ...emptyDraft, page: 0, size: 10 })
  const [data, setData] = useState({ content: [], totalPages: 0 })
  const [detail, setDetail] = useState(null)
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState('')
  const [message, setMessage] = useState('')

  useEffect(() => {
    const allowed = new Set(recipientOptionsForRole(auth?.roleName, admin).map((item) => item.value))
    setDraft((old) => (old.recipient && !allowed.has(old.recipient) ? { ...old, recipient: '' } : old))
    setQuery((old) => {
      const nextStatus = !admin && old.status && old.status !== 'SENT' ? '' : old.status
      const nextRecipient = old.recipient && !allowed.has(old.recipient) ? '' : old.recipient
      if (nextStatus === old.status && nextRecipient === old.recipient) return old
      return { ...old, status: nextStatus, recipient: nextRecipient, page: 0 }
    })
  }, [auth?.roleName, admin])

  const load = useCallback(async () => {
    setLoading(true)
    setError('')
    try {
      setData(await notificationService.list(query))
    } catch (e) {
      setError(extractApiErrorMessage(e, 'Không thể tải thông báo.'))
    } finally {
      setLoading(false)
    }
  }, [query])

  useEffect(() => {
    load()
  }, [load])

  const updateDraft = (name, value) => setDraft((old) => ({ ...old, [name]: value }))

  const applyFilter = () => {
    setQuery((old) => ({ ...old, ...draft, page: 0 }))
  }

  const act = async (fn, success) => {
    try {
      await fn()
      setMessage(success)
      await load()
    } catch (e) {
      setError(extractApiErrorMessage(e, 'Thao tác thất bại.'))
    }
  }

  const open = async (item) => {
    try {
      const d = await notificationService.detail(item.id)
      setDetail(d)
      if (!admin && d.status === 'SENT' && !d.read) {
        await notificationService.markRead(d.id)
        setData((old) => ({
          ...old,
          content: old.content.map((x) => (x.id === d.id ? { ...x, read: true } : x)),
        }))
      }
    } catch (e) {
      setError(extractApiErrorMessage(e, 'Không thể tải chi tiết.'))
    }
  }

  return (
    <div className="page-stack notification-history-page">
      <PageIntro
        eyebrow="Notification > History"
        title="Notification History"
        description="Xem và lọc lịch sử thông báo theo phạm vi tài khoản."
      />

      <section className="content-card">
        <div className="notification-filter-bar">
          <label className="form-field notification-filter-bar__dates">
            <span>Date Range</span>
            <div className="notification-date-range" role="group" aria-label="Date Range">
              <input
                type="date"
                aria-label="From date"
                value={draft.from}
                onChange={(e) => updateDraft('from', e.target.value)}
              />
              <span className="notification-date-range__sep">→</span>
              <input
                type="date"
                aria-label="To date"
                value={draft.to}
                onChange={(e) => updateDraft('to', e.target.value)}
              />
            </div>
          </label>

          <label className="form-field">
            <span>Type</span>
            <select
              aria-label="Type"
              value={draft.type}
              onChange={(e) => updateDraft('type', e.target.value)}
            >
              <option value="">Tất cả</option>
              {NOTIFICATION_TYPES.map((x) => (
                <option key={x} value={x}>
                  {x}
                </option>
              ))}
            </select>
          </label>

          <label className="form-field">
            <span>Status</span>
            <select
              aria-label="Status"
              value={draft.status}
              onChange={(e) => updateDraft('status', e.target.value)}
            >
              <option value="">{admin ? 'Tất cả' : 'SENT (mặc định)'}</option>
              {statusOptions.map((x) => (
                <option key={x} value={x}>
                  {x}
                </option>
              ))}
            </select>
          </label>

          <label className="form-field">
            <span>Recipient</span>
            <select
              aria-label="Recipient"
              value={draft.recipient}
              onChange={(e) => updateDraft('recipient', e.target.value)}
            >
              <option value="">
                {admin ? 'Tất cả nhóm' : 'Phạm vi của tôi'}
              </option>
              {recipientOptions.map((item) => (
                <option key={item.value} value={item.value}>
                  {item.label}
                </option>
              ))}
            </select>
          </label>

          <div className="notification-filter-bar__action">
            <button className="btn btn-primary" type="button" onClick={applyFilter} disabled={loading}>
              Filter
            </button>
          </div>
        </div>

        {error && <div className="alert alert-error">{error}</div>}
        {message && <div className="alert alert-success">{message}</div>}
        {loading && <div className="page-status">Đang tải...</div>}

        {!loading && !data.content?.length && (
          <div className="empty-state">
            <h3>Không có thông báo nào.</h3>
          </div>
        )}

        {!loading && data.content?.length > 0 && (
          <div className="table-card notification-table-card">
            <table className="data-table notification-history-table">
              <colgroup>
                <col className="col-title" />
                <col className="col-type" />
                <col className="col-date" />
                <col className="col-recipient" />
                <col className="col-status" />
                <col className="col-read" />
              </colgroup>
              <thead>
                <tr>
                  <th>Title</th>
                  <th>Type</th>
                  <th>Date Sent</th>
                  <th>Recipient</th>
                  <th>Status</th>
                  <th>Read</th>
                </tr>
              </thead>
              <tbody>
                {data.content.map((item) => (
                  <tr
                    key={item.id}
                    className={`notification-row ${item.read ? 'is-read' : 'is-unread'}`}
                    onClick={() => open(item)}
                  >
                    <td>
                      <span className="notification-cell-ellipsis" title={item.title}>
                        {item.title}
                      </span>
                    </td>
                    <td>{item.type}</td>
                    <td>{formatDateTime(item.sentAt)}</td>
                    <td>{item.recipientGroup || '-'}</td>
                    <td>{badge(item.status)}</td>
                    <td>{readBadge(item.read)}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}

        <div className="form-actions notification-history-paging">
          <button
            type="button"
            disabled={query.page <= 0 || loading}
            onClick={() => setQuery((old) => ({ ...old, page: old.page - 1 }))}
          >
            Trước
          </button>
          <span>
            Trang {query.page + 1}/{Math.max(data.totalPages || 1, 1)}
          </span>
          <button
            type="button"
            disabled={loading || query.page + 1 >= (data.totalPages || 1)}
            onClick={() => setQuery((old) => ({ ...old, page: old.page + 1 }))}
          >
            Sau
          </button>
        </div>
      </section>

      {detail && (
        <div className="modal-backdrop" role="presentation" onClick={() => setDetail(null)}>
          <section className="modal-card" role="dialog" onClick={(e) => e.stopPropagation()}>
            <h2>{detail.title}</h2>
            <p>{detail.content}</p>
            <p>Date Sent: {formatDateTime(detail.sentAt)}</p>
            <p>Recipient: {detail.recipientGroup || '-'}</p>
            <p>Status: {detail.status}</p>
            <p>Read: {readBadge(detail.read)}</p>
            <div className="table-actions">
              {admin && detail.status === 'PENDING_APPROVAL' && (
                <button type="button" onClick={() => act(() => notificationService.approve(detail.id), 'Đã approve.')}>
                  Approve
                </button>
              )}
              {admin && ['PENDING_APPROVAL', 'APPROVED', 'SCHEDULED'].includes(detail.status) && (
                <button type="button" onClick={() => act(() => notificationService.cancel(detail.id), 'Đã cancel.')}>
                  Cancel
                </button>
              )}
              <button type="button" onClick={() => setDetail(null)}>
                Đóng
              </button>
            </div>
          </section>
        </div>
      )}
    </div>
  )
}
