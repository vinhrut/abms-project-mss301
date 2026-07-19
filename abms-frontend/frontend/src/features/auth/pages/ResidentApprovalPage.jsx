/* eslint-disable react-hooks/set-state-in-effect */
import { useEffect, useState } from 'react'
import { authService } from '../../../services/authService.js'
import { extractApiErrorMessage } from '../../../utils/apiError.js'

export function ResidentApprovalPage() {
  const [items, setItems] = useState([])
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState('')
  const [message, setMessage] = useState('')

  const loadPending = async () => {
    try {
      setLoading(true)
      setError('')
      const data = await authService.getPendingResidents()
      setItems(Array.isArray(data) ? data : [])
    } catch (apiError) {
      setError(extractApiErrorMessage(apiError, 'Không thể tải danh sách cư dân chờ duyệt.'))
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    loadPending()
  }, [])

  const handleAction = async (userId, action) => {
    try {
      setMessage('')
      if (action === 'approve') {
        await authService.approveResident(userId)
        setMessage('Đã duyệt cư dân thành công.')
      } else {
        await authService.rejectResident(userId)
        setMessage('Đã từ chối đăng ký cư dân.')
      }
      await loadPending()
    } catch (apiError) {
      setError(extractApiErrorMessage(apiError, 'Không thể cập nhật trạng thái cư dân.'))
    }
  }

  return (
    <div className="page-stack">
      <section className="page-header-card">
        <div>
          <span className="eyebrow">Resident approval</span>
          <h1>Duyệt cư dân đăng ký</h1>
          <p>Manager xác nhận cư dân có đúng thuộc tòa nhà/căn hộ trước khi cho phép đăng nhập hệ thống.</p>
        </div>
      </section>

      <section className="content-card">
        {error ? <div className="alert alert-error">{error}</div> : null}
        {message ? <div className="alert alert-success">{message}</div> : null}

        {loading ? <div className="page-status">Đang tải danh sách chờ duyệt...</div> : null}

        {!loading && items.length === 0 ? (
          <div className="empty-state">
            <h3>Không có cư dân chờ duyệt</h3>
            <p>Tất cả yêu cầu đăng ký hiện đã được xử lý.</p>
          </div>
        ) : null}

        {!loading && items.length > 0 ? (
          <div className="table-card">
            <table className="data-table">
              <thead>
                <tr>
                  <th>Họ tên</th>
                  <th>Email</th>
                  <th>Trạng thái</th>
                  <th>Thao tác</th>
                </tr>
              </thead>
              <tbody>
                {items.map((item) => (
                  <tr key={item.userId}>
                    <td>{item.message}</td>
                    <td>{item.email}</td>
                    <td>{item.status}</td>
                    <td>
                      <div className="table-actions">
                        <button className="btn btn-success" onClick={() => handleAction(item.userId, 'approve')}>
                          Approve
                        </button>
                        <button className="btn btn-danger" onClick={() => handleAction(item.userId, 'reject')}>
                          Reject
                        </button>
                      </div>
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
