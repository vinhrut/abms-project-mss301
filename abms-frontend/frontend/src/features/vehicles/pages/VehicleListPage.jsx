/* eslint-disable react-hooks/set-state-in-effect */
import { useEffect, useMemo, useState } from 'react'
import { vehicleService } from '../../../services/vehicleService.js'
import { useAuth } from '../../auth/context/useAuth.js'
import { extractApiErrorMessage } from '../../../utils/apiError.js'

const statusMap = {
  PENDING: 'badge badge-warning',
  APPROVED: 'badge badge-success',
  REJECTED: 'badge badge-danger',
}

export function VehicleListPage() {
  const { auth, isElevatedRole } = useAuth()
  const [apartmentId, setApartmentId] = useState('')
  const [searchTerm, setSearchTerm] = useState('')
  const [vehicles, setVehicles] = useState([])
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState('')
  const [actionMessage, setActionMessage] = useState('')

  const loadVehicles = async (targetApartmentId = apartmentId) => {
    try {
      setLoading(true)
      setError('')

      let response = []

      if (isElevatedRole) {
        response = await vehicleService.getAllVehicles()
      } else if (auth?.userId) {
        response = await vehicleService.getVehiclesByOwnerId(auth.userId)
      } else if (targetApartmentId.trim()) {
        response = await vehicleService.getVehiclesByApartmentId(targetApartmentId.trim())
      }

      setVehicles(Array.isArray(response) ? response : [])
    } catch (apiError) {
      setError(
        extractApiErrorMessage(
          apiError,
          'Không thể tải danh sách phương tiện. Vui lòng thử lại.',
        ),
      )
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    if (isElevatedRole || auth?.userId) {
      loadVehicles()
    }
  }, [auth?.userId, isElevatedRole])

  const filteredVehicles = useMemo(() => {
    const normalized = searchTerm.trim().toLowerCase()

    if (!normalized) {
      return vehicles
    }

    return vehicles.filter((vehicle) => {
      const licensePlate = vehicle.licensePlate?.toLowerCase() || ''
      const itemApartmentId = vehicle.apartmentId?.toLowerCase() || ''
      return licensePlate.includes(normalized) || itemApartmentId.includes(normalized)
    })
  }, [searchTerm, vehicles])

  const handleStatusAction = async (vehicleId, status) => {
    try {
      setActionMessage('')
      await vehicleService.updateVehicleStatus(vehicleId, status)
      setActionMessage(
        status === 'APPROVED'
          ? 'Đã approve yêu cầu đăng ký xe.'
          : 'Đã reject yêu cầu đăng ký xe.',
      )
      await loadVehicles()
    } catch (apiError) {
      setError(
        extractApiErrorMessage(apiError, 'Không thể cập nhật trạng thái phương tiện.'),
      )
    }
  }

  return (
    <div className="page-stack">
      <section className="page-header-card">
        <div>
          <span className="eyebrow">Vehicle management</span>
          <h1>{isElevatedRole ? 'Duyệt đăng ký phương tiện' : 'Danh sách phương tiện'}</h1>
          <p>
            {isElevatedRole
              ? 'Xem toàn bộ yêu cầu đăng ký xe, tìm kiếm và duyệt các bản ghi PENDING.'
              : 'Xem các phương tiện thuộc tài khoản hiện tại và tra cứu nhanh theo biển số.'}
          </p>
        </div>
      </section>

      <section className="content-card">
        <div className="info-grid">
          <article className="info-card">
            <strong>{isElevatedRole ? 'Manager / Staff mode' : 'Resident mode'}</strong>
            <p>
              {isElevatedRole
                ? 'Backend đã mở API lấy toàn bộ vehicle để phục vụ màn hình duyệt xe.'
                : 'Danh sách đang tự nạp theo ownerId của tài khoản đang đăng nhập.'}
            </p>
          </article>
          <article className="info-card">
            <strong>Người dùng hiện tại</strong>
            <p>{auth?.email || 'Unknown user'}</p>
          </article>
        </div>

        <div className="toolbar-grid">
          {!isElevatedRole ? (
            <label className="form-field">
              <span>Apartment ID (optional fallback)</span>
              <input
                placeholder="Nhập apartment UUID nếu muốn tra cứu thủ công"
                value={apartmentId}
                onChange={(event) => {
                  setApartmentId(event.target.value)
                  setActionMessage('')
                }}
              />
            </label>
          ) : null}

          <label className="form-field">
            <span>Search</span>
            <input
              placeholder="Tìm theo biển số hoặc apartment"
              value={searchTerm}
              onChange={(event) => {
                setSearchTerm(event.target.value)
                setActionMessage('')
              }}
            />
          </label>

          <div className="toolbar-actions">
            <button type="button" className="btn btn-primary" onClick={() => loadVehicles()}>
              Làm mới danh sách
            </button>
          </div>
        </div>

        {error ? <div className="alert alert-error">{error}</div> : null}
        {actionMessage ? <div className="alert alert-success">{actionMessage}</div> : null}

        {loading ? <div className="page-status">Đang tải dữ liệu phương tiện...</div> : null}

        {!loading && filteredVehicles.length === 0 ? (
          <div className="empty-state">
            <h3>Chưa có dữ liệu phương tiện</h3>
            <p>
              {isElevatedRole
                ? 'Hiện chưa có vehicle nào trong hệ thống hoặc không có bản ghi phù hợp với bộ lọc tìm kiếm.'
                : 'Tài khoản này chưa có vehicle nào hoặc chưa có dữ liệu phù hợp với điều kiện tìm kiếm.'}
            </p>
          </div>
        ) : null}

        {!loading && filteredVehicles.length > 0 ? (
          <div className="table-card">
            <table className="data-table">
              <thead>
                <tr>
                  <th>Apartment</th>
                  <th>Owner</th>
                  <th>License Plate</th>
                  <th>Type</th>
                  <th>Brand</th>
                  <th>Status</th>
                  <th>Actions</th>
                </tr>
              </thead>
              <tbody>
                {filteredVehicles.map((vehicle) => (
                  <tr key={vehicle.vehicleId || `${vehicle.apartmentId}-${vehicle.licensePlate}`}>
                    <td>{vehicle.apartmentId}</td>
                    <td>{vehicle.ownerId}</td>
                    <td>{vehicle.licensePlate}</td>
                    <td>{vehicle.type}</td>
                    <td>{vehicle.brand}</td>
                    <td>
                      <span className={statusMap[vehicle.status] || 'badge'}>
                        {vehicle.status || 'PENDING'}
                      </span>
                    </td>
                    <td>
                      {vehicle.status === 'PENDING' && isElevatedRole ? (
                        <div className="table-actions">
                          <button
                            type="button"
                            className="btn btn-success"
                            onClick={() => handleStatusAction(vehicle.vehicleId, 'APPROVED')}
                          >
                            Approve
                          </button>
                          <button
                            type="button"
                            className="btn btn-danger"
                            onClick={() => handleStatusAction(vehicle.vehicleId, 'REJECTED')}
                          >
                            Reject
                          </button>
                        </div>
                      ) : (
                        <span className="table-note">Không có thao tác</span>
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
