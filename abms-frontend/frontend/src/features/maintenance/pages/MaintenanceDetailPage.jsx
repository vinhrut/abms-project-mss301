import { useEffect, useMemo, useState } from 'react'
import { Link, useNavigate, useParams } from 'react-router-dom'
import { apartmentService } from '../../../services/apartmentService.js'
import { maintenanceService } from '../../../services/maintenanceService.js'
import { userService } from '../../../services/userService.js'
import { extractApiErrorMessage } from '../../../utils/apiError.js'
import { APP_ROUTES } from '../../../config/navigation.js'
import { ROLE_KEYS, getRoleLabel, normalizeRole } from '../../../config/roles.js'
import { useAuth } from '../../auth/context/useAuth.js'

const categoryLabels = {
  PLUMBING: 'Plumbing',
  ELECTRICAL: 'Electrical',
  HVAC: 'HVAC',
  CIVIL: 'Civil',
  OTHER: 'Other',
}

function formatStatusLabel(status) {
  if (status === 'IN_PROGRESS') return 'IN PROGRESS'
  if (status === 'CANCELLED') return 'CLOSED'
  return status || '—'
}

function statusBadgeClass(status) {
  if (status === 'IN_PROGRESS') return 'maint-status maint-status-progress'
  if (status === 'OPEN') return 'maint-status maint-status-open'
  if (status === 'RESOLVED') return 'maint-status maint-status-resolved'
  if (status === 'CANCELLED') return 'maint-status maint-status-closed'
  return 'maint-status'
}

function formatDate(value) {
  if (!value) return '—'
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return '—'
  const day = String(date.getDate()).padStart(2, '0')
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const year = String(date.getFullYear()).slice(-2)
  return `${day}/${month}/${year}`
}

function formatDateTime(value) {
  if (!value) return '—'
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return '—'
  const day = String(date.getDate()).padStart(2, '0')
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const year = date.getFullYear()
  const hours = String(date.getHours()).padStart(2, '0')
  const minutes = String(date.getMinutes()).padStart(2, '0')
  return `${day}/${month}/${year} ${hours}:${minutes}`
}

function getInitials(name) {
  if (!name) return '?'
  return name
    .split(' ')
    .filter(Boolean)
    .slice(0, 2)
    .map((part) => part[0]?.toUpperCase() || '')
    .join('')
}

function resolveActorLabel(user, fallbackId) {
  if (user?.fullName) {
    return user.fullName
  }
  if (user?.email) {
    return user.email
  }
  return fallbackId ? `${fallbackId.slice(0, 8)}…` : 'System'
}

function resolveActorRole(user) {
  if (!user?.roleName) {
    return 'User'
  }
  return getRoleLabel(user.roleName)
}

function buildHistorySummary(entry, userMap) {
  const actor = entry.changedBy ? userMap[entry.changedBy] : null
  const actorName = resolveActorLabel(actor, entry.changedBy)
  const actorRole = resolveActorRole(actor)

  if (!entry.fromStatus) {
    return {
      title: `Created ${formatStatusLabel(entry.toStatus)}`,
      subtitle: `by ${actorRole} · ${actorName}`,
      note: entry.note,
    }
  }

  return {
    title: `${formatStatusLabel(entry.fromStatus)} → ${formatStatusLabel(entry.toStatus)}`,
    subtitle: `by ${actorRole} · ${actorName}`,
    note: entry.note,
  }
}

export function MaintenanceDetailPage() {
  const { requestId } = useParams()
  const navigate = useNavigate()
  const { auth } = useAuth()
  const role = normalizeRole(auth?.roleName)
  const canAssign = role === ROLE_KEYS.ADMIN || role === ROLE_KEYS.MANAGER || role === ROLE_KEYS.STAFF
  const isTechnician = role === ROLE_KEYS.TECHNICIAN

  const [request, setRequest] = useState(null)
  const [history, setHistory] = useState([])
  const [users, setUsers] = useState([])
  const [roomNumber, setRoomNumber] = useState('')
  const [selectedTechnicianId, setSelectedTechnicianId] = useState('')
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [actionMessage, setActionMessage] = useState('')

  const userMap = useMemo(() => {
    const map = {}
    users.forEach((user) => {
      map[user.userId] = user
    })
    return map
  }, [users])

  const technicians = useMemo(
    () =>
      users.filter(
        (user) => normalizeRole(user.roleName) === ROLE_KEYS.TECHNICIAN && user.status === 'ACTIVE',
      ),
    [users],
  )

  const assignedTechnician = request?.technicianId ? userMap[request.technicianId] : null

  const staffNote = useMemo(() => {
    const noteEntry = history.find((entry) => entry.note && entry.note !== 'Request submitted')
    return noteEntry?.note || ''
  }, [history])

  const canComplete =
    request?.status === 'IN_PROGRESS' &&
    (canAssign || (isTechnician && request?.technicianId === auth?.userId))

  const canAssignNow =
    canAssign && (request?.status === 'OPEN' || request?.status === 'IN_PROGRESS')

  const loadDetail = async () => {
    if (!requestId) {
      return
    }

    try {
      setLoading(true)
      setError('')

      const [requestData, historyData, userData] = await Promise.all([
        maintenanceService.getRequestById(requestId),
        maintenanceService.getHistory(requestId),
        userService.getUsers().catch(() => []),
      ])

      setRequest(requestData)
      setHistory(Array.isArray(historyData) ? historyData : [])
      setUsers(Array.isArray(userData) ? userData : [])
      setSelectedTechnicianId(requestData?.technicianId || '')

      if (requestData?.apartmentId) {
        try {
          let apartment = null
          if (auth?.buildingId) {
            const apartments = await apartmentService.getApartmentsByBuildingId(auth.buildingId)
            apartment = apartments.find((item) => item.apartmentId === requestData.apartmentId)
          }
          if (!apartment) {
            try {
              apartment = await apartmentService.getApartmentById(requestData.apartmentId)
            } catch {
              const myApartments = await apartmentService.getMyApartments().catch(() => [])
              apartment = myApartments.find((item) => item.apartmentId === requestData.apartmentId)
            }
          }
          setRoomNumber(apartment?.roomNumber || '')
        } catch {
          setRoomNumber('')
        }
      }
    } catch (apiError) {
      setError(extractApiErrorMessage(apiError, 'Không thể tải chi tiết yêu cầu bảo trì.'))
      setRequest(null)
      setHistory([])
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    loadDetail()
  }, [requestId, auth?.buildingId])

  const handleAssign = async () => {
    if (!selectedTechnicianId) {
      setError('Vui lòng chọn kỹ thuật viên trước khi phân công.')
      return
    }

    try {
      setActionMessage('')
      setError('')
      await maintenanceService.assignStaff(requestId, selectedTechnicianId)
      setActionMessage('Đã phân công kỹ thuật viên thành công.')
      await loadDetail()
    } catch (apiError) {
      setError(extractApiErrorMessage(apiError, 'Không thể phân công kỹ thuật viên.'))
    }
  }

  const handleComplete = async () => {
    try {
      setActionMessage('')
      setError('')
      await maintenanceService.completeRequest(requestId)
      setActionMessage('Đã xác nhận hoàn thành yêu cầu.')
      await loadDetail()
    } catch (apiError) {
      setError(extractApiErrorMessage(apiError, 'Không thể cập nhật trạng thái hoàn thành.'))
    }
  }

  if (loading) {
    return (
      <div className="page-stack">
        <div className="page-status">Đang tải chi tiết yêu cầu...</div>
      </div>
    )
  }

  if (!request) {
    return (
      <div className="page-stack">
        <section className="content-card">
          {error ? <div className="alert alert-error">{error}</div> : null}
          <div className="page-status">Không tìm thấy yêu cầu bảo trì.</div>
          <Link className="btn btn-secondary" to={APP_ROUTES.maintenance}>
            Quay lại danh sách
          </Link>
        </section>
      </div>
    )
  }

  return (
    <div className="page-stack">
      <section className="page-header-card page-header-split maint-detail-header">
        <div>
          <span className="eyebrow">Maintenance detail</span>
          <h1>🔧 Maintenance Request Detail – {request.requestCode}</h1>
          <p>Xem thông tin chi tiết, lịch sử trạng thái và thao tác xử lý yêu cầu.</p>
        </div>
        <span className={statusBadgeClass(request.status)}>{formatStatusLabel(request.status)}</span>
      </section>

      <section className="content-card maint-detail-card">
        <div className="maint-detail-summary">
          <div className="maint-detail-field">
            <span>CODE</span>
            <strong className="maint-detail-link">{request.requestCode}</strong>
          </div>
          <div className="maint-detail-field">
            <span>APARTMENT</span>
            <strong>{roomNumber || '—'}</strong>
          </div>
          <div className="maint-detail-field">
            <span>DATE SUBMITTED</span>
            <strong>{formatDate(request.createdAt)}</strong>
          </div>
          <div className="maint-detail-field">
            <span>CATEGORY</span>
            <strong>{categoryLabels[request.category] || request.category}</strong>
          </div>
          <div className="maint-detail-field">
            <span>PRIORITY</span>
            <strong>
              {request.priority === 'EMERGENCY' ? (
                <span className="maint-priority-emergency">
                  <span aria-hidden="true">⚠</span> Emergency
                </span>
              ) : (
                'Normal'
              )}
            </strong>
          </div>
          <div className="maint-detail-field">
            <span>STATUS</span>
            <strong>
              <span className={statusBadgeClass(request.status)}>
                {formatStatusLabel(request.status)}
              </span>
            </strong>
          </div>
        </div>

        <div className="maint-detail-section">
          <span className="maint-detail-label">TITLE</span>
          <div className="maint-detail-box">{request.title}</div>
        </div>

        <div className="maint-detail-section">
          <span className="maint-detail-label">DESCRIPTION</span>
          <div className="maint-detail-box maint-detail-box--tall">
            {request.description || '—'}
          </div>
        </div>

        <div className="maint-detail-section">
          <span className="maint-detail-label">PHOTOS</span>
          <div className="maint-detail-photos">
            <span className="muted-text">Chưa có ảnh đính kèm.</span>
          </div>
        </div>

        <div className="maint-detail-staff-grid">
          <div className="maint-detail-section">
            <span className="maint-detail-label">ASSIGNED STAFF</span>
            {assignedTechnician ? (
              <div className="maint-staff-card">
                <div className="maint-staff-avatar">{getInitials(assignedTechnician.fullName)}</div>
                <div>
                  <strong>{assignedTechnician.fullName || assignedTechnician.email}</strong>
                  <p className="muted-text">
                    Assigned at {formatDateTime(request.assignedAt)}
                  </p>
                </div>
              </div>
            ) : (
              <div className="maint-detail-box muted-text">Chưa phân công kỹ thuật viên.</div>
            )}
          </div>

          <div className="maint-detail-section">
            <span className="maint-detail-label">STAFF NOTE</span>
            <div className="maint-detail-box maint-detail-box--tall">
              {staffNote || '—'}
            </div>
          </div>
        </div>

        <div className="maint-detail-section">
          <span className="maint-detail-label">STATUS HISTORY</span>
          <div className="maint-history-list">
            {history.length === 0 ? (
              <div className="muted-text">Chưa có lịch sử trạng thái.</div>
            ) : (
              history.map((entry) => {
                const summary = buildHistorySummary(entry, userMap)
                return (
                  <article key={entry.historyId} className="maint-history-item">
                    <div className="maint-history-dot" aria-hidden="true" />
                    <div>
                      <div className="maint-history-meta">{formatDateTime(entry.changedAt)}</div>
                      <div className="maint-history-title">{summary.title}</div>
                      <div className="maint-history-subtitle">{summary.subtitle}</div>
                      {summary.note ? <div className="maint-history-note">{summary.note}</div> : null}
                    </div>
                  </article>
                )
              })
            )}
          </div>
        </div>

        {error ? <div className="alert alert-error">{error}</div> : null}
        {actionMessage ? <div className="alert alert-success">{actionMessage}</div> : null}

        <div className="maint-detail-actions">
          {canAssignNow ? (
            <div className="maint-action-group">
              <button type="button" className="btn btn-secondary" onClick={handleAssign}>
                👤 Assign Staff
              </button>
              <select
                value={selectedTechnicianId}
                onChange={(event) => setSelectedTechnicianId(event.target.value)}
              >
                <option value="">Chọn technician</option>
                {technicians.map((tech) => (
                  <option key={tech.userId} value={tech.userId}>
                    {tech.fullName || tech.email}
                  </option>
                ))}
              </select>
            </div>
          ) : null}

          <button type="button" className="btn btn-secondary" onClick={() => navigate(APP_ROUTES.maintenance)}>
            ↩ Quay lại danh sách
          </button>

          {canComplete ? (
            <button type="button" className="btn btn-primary" onClick={handleComplete}>
              ✓ Confirm Complete
            </button>
          ) : null}
        </div>
      </section>
    </div>
  )
}
