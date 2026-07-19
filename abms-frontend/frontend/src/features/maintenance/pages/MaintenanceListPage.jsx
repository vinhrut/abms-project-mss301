import { useEffect, useMemo, useState } from 'react'
import { Link } from 'react-router-dom'
import { maintenanceService } from '../../../services/maintenanceService.js'
import { userService } from '../../../services/userService.js'
import { extractApiErrorMessage } from '../../../utils/apiError.js'
import { APP_ROUTES } from '../../../config/navigation.js'
import { ROLE_KEYS, normalizeRole } from '../../../config/roles.js'
import { useAuth } from '../../auth/context/useAuth.js'

const statusClassMap = {
  OPEN: 'badge badge-warning',
  IN_PROGRESS: 'badge badge-info',
  RESOLVED: 'badge badge-success',
  CANCELLED: 'badge badge-danger',
}

export function MaintenanceListPage() {
  const { auth } = useAuth()
  const role = normalizeRole(auth?.roleName)
  const canAssign = role === ROLE_KEYS.ADMIN || role === ROLE_KEYS.MANAGER || role === ROLE_KEYS.STAFF
  const isResident = role === ROLE_KEYS.RESIDENT
  const isTechnician = role === ROLE_KEYS.TECHNICIAN

  const [requests, setRequests] = useState([])
  const [technicians, setTechnicians] = useState([])
  const [statusFilter, setStatusFilter] = useState('')
  const [priorityFilter, setPriorityFilter] = useState('')
  const [searchTerm, setSearchTerm] = useState('')
  const [assignSelections, setAssignSelections] = useState({})
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState('')
  const [actionMessage, setActionMessage] = useState('')

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
    loadRequests()
  }, [auth?.userId, isResident, isTechnician, statusFilter, priorityFilter])

  useEffect(() => {
    if (!canAssign) {
      return
    }

    const loadTechnicians = async () => {
      try {
        const users = await userService.getUsers()
        const list = (Array.isArray(users) ? users : []).filter(
          (user) => normalizeRole(user.roleName) === ROLE_KEYS.TECHNICIAN && user.status === 'ACTIVE',
        )
        setTechnicians(list)
      } catch {
        setTechnicians([])
      }
    }

    loadTechnicians()
  }, [canAssign])

  const filteredRequests = useMemo(() => {
    const normalized = searchTerm.trim().toLowerCase()
    if (!normalized) {
      return requests
    }

    return requests.filter((item) => {
      const code = item.requestCode?.toLowerCase() || ''
      const title = item.title?.toLowerCase() || ''
      const apartmentId = item.apartmentId?.toLowerCase() || ''
      return code.includes(normalized) || title.includes(normalized) || apartmentId.includes(normalized)
    })
  }, [requests, searchTerm])

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
                : 'Danh sách yêu cầu bảo trì'}
          </h1>
          <p>
            {canAssign
              ? 'Xem toàn bộ ticket, lọc theo trạng thái/ưu tiên và phân công kỹ thuật viên.'
              : isResident
                ? 'Theo dõi các yêu cầu bạn đã gửi và tạo yêu cầu mới khi cần.'
                : 'Xem các ticket đang được giao cho bạn.'}
          </p>
        </div>
        {isResident || canAssign ? (
          <Link className="btn btn-primary" to={APP_ROUTES.maintenanceSubmit}>
            Gửi yêu cầu mới
          </Link>
        ) : null}
      </section>

      <section className="content-card">
        <div className="toolbar-grid">
          {canAssign ? (
            <>
              <label className="form-field">
                <span>Status</span>
                <select value={statusFilter} onChange={(event) => setStatusFilter(event.target.value)}>
                  <option value="">All</option>
                  <option value="OPEN">OPEN</option>
                  <option value="IN_PROGRESS">IN_PROGRESS</option>
                  <option value="RESOLVED">RESOLVED</option>
                  <option value="CANCELLED">CANCELLED</option>
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
              placeholder="Tìm theo mã, tiêu đề hoặc apartment"
              value={searchTerm}
              onChange={(event) => setSearchTerm(event.target.value)}
            />
          </label>

          <div className="toolbar-actions">
            <button type="button" className="btn btn-primary" onClick={loadRequests}>
              Làm mới
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
            <table className="data-table">
              <thead>
                <tr>
                  <th>Mã</th>
                  <th>Tiêu đề</th>
                  <th>Hạng mục</th>
                  <th>Ưu tiên</th>
                  <th>Trạng thái</th>
                  <th>Apartment</th>
                  {canAssign ? <th>Phân công</th> : null}
                </tr>
              </thead>
              <tbody>
                {filteredRequests.map((item) => (
                  <tr key={item.requestId}>
                    <td>{item.requestCode}</td>
                    <td>
                      <strong>{item.title}</strong>
                      {item.description ? <p className="muted-text">{item.description}</p> : null}
                    </td>
                    <td>{item.category}</td>
                    <td>{item.priority}</td>
                    <td>
                      <span className={statusClassMap[item.status] || 'badge'}>{item.status}</span>
                    </td>
                    <td className="mono-text">{item.apartmentId}</td>
                    {canAssign ? (
                      <td>
                        {item.status === 'OPEN' || item.status === 'IN_PROGRESS' ? (
                          <div className="inline-actions">
                            <select
                              value={assignSelections[item.requestId] || item.technicianId || ''}
                              onChange={(event) =>
                                setAssignSelections((prev) => ({
                                  ...prev,
                                  [item.requestId]: event.target.value,
                                }))
                              }
                            >
                              <option value="">Chọn technician</option>
                              {technicians.map((tech) => (
                                <option key={tech.userId} value={tech.userId}>
                                  {tech.fullName || tech.email}
                                </option>
                              ))}
                            </select>
                            <button
                              type="button"
                              className="btn btn-secondary"
                              onClick={() => handleAssign(item.requestId)}
                            >
                              Assign
                            </button>
                          </div>
                        ) : (
                          <span className="muted-text">{item.technicianId || '—'}</span>
                        )}
                      </td>
                    ) : null}
                  </tr>
                ))}
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
