import { useEffect, useMemo, useState } from 'react'
import { Link, useSearchParams } from 'react-router-dom'
import { apartmentService } from '../../../services/apartmentService.js'
import { maintenanceService } from '../../../services/maintenanceService.js'
import { userService } from '../../../services/userService.js'
import { extractApiErrorMessage } from '../../../utils/apiError.js'
import { APP_ROUTES, getMaintenanceDetailRoute } from '../../../config/navigation.js'
import { ROLE_KEYS, normalizeRole } from '../../../config/roles.js'
import { useAuth } from '../../auth/context/useAuth.js'
import { MaintenanceSubmitModal } from '../components/MaintenanceSubmitModal.jsx'

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

export function MaintenanceListPage() {
  const { auth } = useAuth()
  const [searchParams, setSearchParams] = useSearchParams()
  const role = normalizeRole(auth?.roleName)
  const canAssign = role === ROLE_KEYS.ADMIN || role === ROLE_KEYS.MANAGER || role === ROLE_KEYS.STAFF
  const isResident = role === ROLE_KEYS.RESIDENT
  const isTechnician = role === ROLE_KEYS.TECHNICIAN
  const canSubmit = isResident

  const [requests, setRequests] = useState([])
  const [apartmentMap, setApartmentMap] = useState({})
  const [technicians, setTechnicians] = useState([])
  const [technicianMap, setTechnicianMap] = useState({})
  const [statusFilter, setStatusFilter] = useState('')
  const [priorityFilter, setPriorityFilter] = useState('')
  const [searchTerm, setSearchTerm] = useState('')
  const [assignSelections, setAssignSelections] = useState({})
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState('')
  const [actionMessage, setActionMessage] = useState('')
  const [submitOpen, setSubmitOpen] = useState(false)

  const loadRequests = async () => {
    try {
      setLoading(true)
      setError('')

      let response = []
      if (isResident && auth?.userId) {
        response = await maintenanceService.getMyRequests(auth.userId)
      } else if (isTechnician && auth?.userId) {
        response = await maintenanceService.getMyTasks(auth.userId)
      } else {
        response = await maintenanceService.getAllRequests({
          status: statusFilter || undefined,
          priority: priorityFilter || undefined,
        })
      }

      setRequests(Array.isArray(response) ? response : [])
    } catch (apiError) {
      setError(
        extractApiErrorMessage(apiError, 'Không thể tải danh sách yêu cầu bảo trì.'),
      )
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    if (searchParams.get('submit') === '1' && canSubmit) {
      setSubmitOpen(true)
      setSearchParams({}, { replace: true })
    }
  }, [searchParams, setSearchParams, canSubmit])

  useEffect(() => {
    loadRequests()
  }, [auth?.userId, isResident, isTechnician, statusFilter, priorityFilter])

  useEffect(() => {
    const loadApartments = async () => {
      try {
        const apartments = auth?.buildingId
          ? await apartmentService.getApartmentsByBuildingId(auth.buildingId)
          : await apartmentService.getAllApartments()
        const map = {}
        ;(Array.isArray(apartments) ? apartments : []).forEach((apartment) => {
          map[apartment.apartmentId] = apartment.roomNumber || apartment.apartmentId
        })
        setApartmentMap(map)
      } catch {
        setApartmentMap({})
      }
    }

    loadApartments()
  }, [auth?.buildingId])

  useEffect(() => {
    if (!canAssign) {
      return
    }

    const loadTechnicians = async () => {
      try {
        const users = await userService.getUsers()
        const allUsers = Array.isArray(users) ? users : []
        const map = {}
        allUsers.forEach((user) => {
          map[user.userId] = user.fullName || user.email || user.userId
        })
        setTechnicianMap(map)
        setTechnicians(
          allUsers.filter(
            (user) => normalizeRole(user.roleName) === ROLE_KEYS.TECHNICIAN && user.status === 'ACTIVE',
          ),
        )
      } catch {
        setTechnicians([])
        setTechnicianMap({})
      }
    }

    loadTechnicians()
  }, [canAssign])

  const filteredRequests = useMemo(() => {
    let scoped = requests
    if (auth?.buildingId) {
      const allowedApartmentIds = new Set(Object.keys(apartmentMap))
      if (allowedApartmentIds.size > 0) {
        scoped = requests.filter((item) => allowedApartmentIds.has(item.apartmentId))
      }
    }

    const normalized = searchTerm.trim().toLowerCase()
    if (!normalized) {
      return scoped
    }

    return scoped.filter((item) => {
      const code = item.requestCode?.toLowerCase() || ''
      const title = item.title?.toLowerCase() || ''
      const room = (apartmentMap[item.apartmentId] || item.apartmentId || '').toLowerCase()
      const technician = (technicianMap[item.technicianId] || '').toLowerCase()
      return (
        code.includes(normalized)
        || title.includes(normalized)
        || room.includes(normalized)
        || technician.includes(normalized)
      )
    })
  }, [requests, searchTerm, apartmentMap, technicianMap, auth?.buildingId])

  const handleAssign = async (requestId) => {
    const technicianId = assignSelections[requestId]
    if (!technicianId) {
      setError('Vui lòng chọn kỹ thuật viên trước khi phân công.')
      return
    }

    try {
      setActionMessage('')
      setError('')
      await maintenanceService.assignStaff(requestId, technicianId)
      setActionMessage('Đã phân công kỹ thuật viên thành công.')
      await loadRequests()
    } catch (apiError) {
      setError(extractApiErrorMessage(apiError, 'Không thể phân công kỹ thuật viên.'))
    }
  }

  return (
    <div className="page-stack">
      <section className="page-header-card">
        <div>
          <span className="eyebrow">Maintenance management</span>
          <h1>
            {isResident
              ? 'Yêu cầu bảo trì của tôi'
              : isTechnician
                ? 'Công việc được giao'
                : 'Maintenance Requests'}
          </h1>
          <p>
            {canAssign
              ? 'Danh sách yêu cầu bảo trì theo mã, căn hộ, ưu tiên và trạng thái.'
              : isResident
                ? 'Theo dõi các yêu cầu bạn đã gửi và tạo yêu cầu mới khi cần.'
                : 'Xem các ticket đang được giao cho bạn.'}
          </p>
        </div>
        {canSubmit ? (
          <button type="button" className="btn btn-primary" onClick={() => setSubmitOpen(true)}>
            Gửi yêu cầu mới
          </button>
        ) : null}
      </section>

      <MaintenanceSubmitModal
        open={submitOpen}
        onClose={() => setSubmitOpen(false)}
        onSubmitted={(response) => {
          setActionMessage(
            `Đã gửi yêu cầu ${response.requestCode || ''} thành công. Trạng thái: ${response.status || 'OPEN'}.`,
          )
          loadRequests()
        }}
      />

      <section className="content-card">
        <div className="toolbar-grid">
          {canAssign ? (
            <>
              <label className="form-field">
                <span>Status</span>
                <select value={statusFilter} onChange={(event) => setStatusFilter(event.target.value)}>
                  <option value="">All</option>
                  <option value="OPEN">OPEN</option>
                  <option value="IN_PROGRESS">IN PROGRESS</option>
                  <option value="RESOLVED">RESOLVED</option>
                  <option value="CANCELLED">CLOSED</option>
                </select>
              </label>
              <label className="form-field">
                <span>Priority</span>
                <select value={priorityFilter} onChange={(event) => setPriorityFilter(event.target.value)}>
                  <option value="">All</option>
                  <option value="NORMAL">Normal</option>
                  <option value="EMERGENCY">Emergency</option>
                </select>
              </label>
            </>
          ) : null}

          <label className="form-field">
            <span>Search</span>
            <input
              placeholder="Search by code, apartment or title"
              value={searchTerm}
              onChange={(event) => setSearchTerm(event.target.value)}
            />
          </label>

          <div className="toolbar-actions">
            <button type="button" className="btn btn-primary" onClick={loadRequests}>
              Refresh
            </button>
          </div>
        </div>

        {error ? <div className="alert alert-error">{error}</div> : null}
        {actionMessage ? <div className="alert alert-success">{actionMessage}</div> : null}
        {loading ? <div className="page-status">Đang tải dữ liệu...</div> : null}

        {!loading && filteredRequests.length === 0 ? (
          <div className="page-status">Chưa có yêu cầu bảo trì nào.</div>
        ) : null}

        {!loading && filteredRequests.length > 0 ? (
          <div className="table-wrap">
            <table className="data-table maint-table">
              <thead>
                <tr>
                  <th>CODE</th>
                  <th>APARTMENT</th>
                  <th>TITLE</th>
                  <th>CATEGORY</th>
                  <th>PRIORITY</th>
                  <th>STATUS</th>
                  <th>DATE</th>
                  {canAssign ? <th>ASSIGN</th> : null}
                </tr>
              </thead>
              <tbody>
                {filteredRequests.map((item) => {
                  const isEmergency = item.priority === 'EMERGENCY'
                  const roomLabel = apartmentMap[item.apartmentId] || '—'

                  return (
                    <tr key={item.requestId}>
                      <td className="maint-code">
                        <Link to={getMaintenanceDetailRoute(item.requestId)}>{item.requestCode}</Link>
                      </td>
                      <td className="maint-apartment">{roomLabel}</td>
                      <td className="maint-title">{item.title}</td>
                      <td>{categoryLabels[item.category] || item.category}</td>
                      <td>
                        {isEmergency ? (
                          <span className="maint-priority-emergency">
                            <span aria-hidden="true">⚠</span> Emergency
                          </span>
                        ) : (
                          <span>Normal</span>
                        )}
                      </td>
                      <td>
                        <span className={statusBadgeClass(item.status)}>
                          {formatStatusLabel(item.status)}
                        </span>
                      </td>
                      <td>{formatDate(item.createdAt)}</td>
                      {canAssign ? (
                        <td className="maint-assign-cell">
                          {!item.technicianId && item.status === 'OPEN' ? (
                            <div className="maint-assign">
                              <div className="maint-assign-select-wrap">
                                <select
                                  className="maint-assign-select"
                                  value={assignSelections[item.requestId] || ''}
                                  onChange={(event) =>
                                    setAssignSelections((prev) => ({
                                      ...prev,
                                      [item.requestId]: event.target.value,
                                    }))
                                  }
                                  aria-label="Chọn kỹ thuật viên"
                                >
                                  <option value="">Select technician</option>
                                  {technicians.map((tech) => (
                                    <option key={tech.userId} value={tech.userId}>
                                      {tech.fullName || tech.email}
                                    </option>
                                  ))}
                                </select>
                              </div>
                              <button
                                type="button"
                                className="btn btn-secondary maint-assign-btn"
                                onClick={() => handleAssign(item.requestId)}
                              >
                                Assign
                              </button>
                            </div>
                          ) : item.technicianId ? (
                            <span className="maint-assigned-tech">
                              {technicianMap[item.technicianId] || 'Đã phân công'}
                            </span>
                          ) : (
                            <span className="muted-text">—</span>
                          )}
                        </td>
                      ) : null}
                    </tr>
                  )
                })}
              </tbody>
            </table>
          </div>
        ) : null}

        {!canAssign && !isResident ? (
          <p className="muted-text">
            Technician nên dùng trang <Link to={APP_ROUTES.maintenanceTasks}>My Tasks</Link> để cập nhật tiến độ.
          </p>
        ) : null}
      </section>
    </div>
  )
}
