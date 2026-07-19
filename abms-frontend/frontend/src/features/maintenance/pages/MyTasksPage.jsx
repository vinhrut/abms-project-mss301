import { useEffect, useMemo, useState } from 'react'
import { Link } from 'react-router-dom'
import { maintenanceService } from '../../../services/maintenanceService.js'
import { extractApiErrorMessage } from '../../../utils/apiError.js'
import { APP_ROUTES } from '../../../config/navigation.js'
import { useAuth } from '../../auth/context/useAuth.js'

const statusClassMap = {
  OPEN: 'badge badge-warning',
  IN_PROGRESS: 'badge badge-info',
  RESOLVED: 'badge badge-success',
  CANCELLED: 'badge badge-danger',
}

export function MyTasksPage() {
  const { auth } = useAuth()
  const [tasks, setTasks] = useState([])
  const [statusFilter, setStatusFilter] = useState('IN_PROGRESS')
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState('')
  const [actionMessage, setActionMessage] = useState('')

  const loadTasks = async () => {
    if (!auth?.userId) {
      return
    }

    try {
      setLoading(true)
      setError('')
      const response = await maintenanceService.getMyTasks(auth.userId)
      setTasks(Array.isArray(response) ? response : [])
    } catch (apiError) {
      setError(extractApiErrorMessage(apiError, 'Không thể tải danh sách task được giao.'))
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    loadTasks()
  }, [auth?.userId])

  const filteredTasks = useMemo(() => {
    if (!statusFilter) {
      return tasks
    }
    return tasks.filter((task) => task.status === statusFilter)
  }, [tasks, statusFilter])

  const handleComplete = async (requestId) => {
    try {
      setActionMessage('')
      setError('')
      await maintenanceService.completeRequest(requestId)
      setActionMessage('Đã đánh dấu hoàn thành yêu cầu.')
      await loadTasks()
    } catch (apiError) {
      setError(extractApiErrorMessage(apiError, 'Không thể cập nhật trạng thái hoàn thành.'))
    }
  }

  return (
    <div className="page-stack">
      <section className="page-header-card">
        <div>
          <span className="eyebrow">Technician workspace</span>
          <h1>My Maintenance Tasks</h1>
          <p>Danh sách ticket được Building Manager phân công cho bạn.</p>
        </div>
        <Link className="btn btn-secondary" to={APP_ROUTES.maintenance}>
          Xem tất cả maintenance
        </Link>
      </section>

      <section className="content-card">
        <div className="toolbar-grid">
          <label className="form-field">
            <span>Status</span>
            <select value={statusFilter} onChange={(event) => setStatusFilter(event.target.value)}>
              <option value="">All</option>
              <option value="IN_PROGRESS">IN_PROGRESS</option>
              <option value="RESOLVED">RESOLVED</option>
            </select>
          </label>
          <div className="toolbar-actions">
            <button type="button" className="btn btn-primary" onClick={loadTasks}>
              Làm mới
            </button>
          </div>
        </div>

        {error ? <div className="alert alert-error">{error}</div> : null}
        {actionMessage ? <div className="alert alert-success">{actionMessage}</div> : null}
        {loading ? <div className="page-status">Đang tải task...</div> : null}

        {!loading && filteredTasks.length === 0 ? (
          <div className="page-status">Chưa có task nào được giao.</div>
        ) : null}

        {!loading && filteredTasks.length > 0 ? (
          <div className="table-wrap">
            <table className="data-table">
              <thead>
                <tr>
                  <th>Mã</th>
                  <th>Tiêu đề</th>
                  <th>Ưu tiên</th>
                  <th>Trạng thái</th>
                  <th>Apartment</th>
                  <th>Thao tác</th>
                </tr>
              </thead>
              <tbody>
                {filteredTasks.map((task) => (
                  <tr key={task.requestId}>
                    <td>{task.requestCode}</td>
                    <td>
                      <strong>{task.title}</strong>
                      {task.description ? <p className="muted-text">{task.description}</p> : null}
                    </td>
                    <td>{task.priority}</td>
                    <td>
                      <span className={statusClassMap[task.status] || 'badge'}>{task.status}</span>
                    </td>
                    <td className="mono-text">{task.apartmentId}</td>
                    <td>
                      {task.status === 'IN_PROGRESS' ? (
                        <button
                          type="button"
                          className="btn btn-primary"
                          onClick={() => handleComplete(task.requestId)}
                        >
                          Đánh dấu xong
                        </button>
                      ) : (
                        <span className="muted-text">—</span>
                      )}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        ) : null}
      </section>
    </div>
  )
}
