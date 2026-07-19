/* eslint-disable react-hooks/set-state-in-effect */
import { useEffect, useMemo, useState } from 'react'
import { ROLE_KEYS } from '../../../config/roles.js'
import { apartmentService } from '../../../services/apartmentService.js'
import { userService } from '../../../services/userService.js'
import { vehicleService } from '../../../services/vehicleService.js'
import { useAuth } from '../../auth/context/useAuth.js'
import { extractApiErrorMessage } from '../../../utils/apiError.js'
import { validateVehicleForm } from '../utils/vehicleValidation.js'

function normalizeCollection(data) {
  if (Array.isArray(data)) return data
  if (Array.isArray(data?.content)) return data.content
  if (Array.isArray(data?.items)) return data.items
  if (Array.isArray(data?.data)) return data.data
  if (Array.isArray(data?.result)) return data.result
  return []
}

function getApartmentId(apartment) {
  if (typeof apartment === 'string') return apartment
  return apartment?.apartmentId || apartment?.id || apartment?.apartment_id || ''
}

function getApartmentDisplayName(apartment) {
  if (typeof apartment === 'string') return apartment
  return apartment?.roomNumber || apartment?.apartmentNumber || apartment?.code || apartment?.name || 'Căn hộ'
}

function getVehicleApartmentId(vehicle) {
  return vehicle?.apartmentId || vehicle?.apartment_id || vehicle?.apartment?.apartmentId || vehicle?.apartment?.id || ''
}

function getVehicleOwnerId(vehicle) {
  return vehicle?.ownerId || vehicle?.owner_id || vehicle?.owner?.userId || vehicle?.owner?.id || ''
}

function buildFallbackApartmentsFromVehicles(vehicleItems) {
  const apartmentIds = [...new Set(vehicleItems.map(getVehicleApartmentId).filter(Boolean))]
  return apartmentIds.map((apartmentId) => ({ apartmentId, roomNumber: apartmentId }))
}

function mergeApartments(currentApartments, incomingApartments) {
  const apartmentMap = new Map()

  currentApartments.forEach((apartment) => {
    const apartmentId = getApartmentId(apartment)
    if (apartmentId) apartmentMap.set(apartmentId, apartment)
  })

  incomingApartments.forEach((apartment) => {
    const apartmentId = getApartmentId(apartment)
    if (apartmentId) apartmentMap.set(apartmentId, apartment)
  })

  return [...apartmentMap.values()]
}

function getResidentLabel(user) {
  if (!user) return 'Không rõ cư dân'
  const name = user.fullName ? ` - ${user.fullName}` : ''
  return `${user.email || user.userId}${name}`
}

const statusClassMap = {
  PENDING: 'badge badge-warning',
  APPROVED: 'badge badge-success',
  REJECTED: 'badge badge-danger',
  CANCELLED: 'badge',
  INACTIVE: 'badge',
}

const emptyFilters = {
  status: '',
  type: '',
  licensePlate: '',
  apartmentId: '',
  ownerId: '',
}

const emptyLimitForm = {
  limitId: '',
  apartmentId: '',
  vehicleType: 'MOTORBIKE',
  maxQuantity: 0,
}

const emptyVehicleForm = {
  apartmentId: '',
  licensePlate: '',
  type: 'MOTORBIKE',
  brand: '',
}

export function VehicleListPage() {
  const { auth, normalizedRole, isElevatedRole } = useAuth()
  const isResident = normalizedRole === ROLE_KEYS.RESIDENT
  const canManage = isElevatedRole

  const [filters, setFilters] = useState(emptyFilters)
  const [vehicles, setVehicles] = useState([])
  const [pageInfo, setPageInfo] = useState({ page: 0, totalPages: 1, totalElements: 0 })
  const [limits, setLimits] = useState([])
  const [limitForm, setLimitForm] = useState(emptyLimitForm)
  const [vehicleForm, setVehicleForm] = useState(emptyVehicleForm)
  const [editingVehicleId, setEditingVehicleId] = useState('')
  const [formErrors, setFormErrors] = useState({})
  const [apartments, setApartments] = useState([])
  const [users, setUsers] = useState([])
  const [loading, setLoading] = useState(false)
  const [limitLoading, setLimitLoading] = useState(false)
  const [lookupLoading, setLookupLoading] = useState(false)
  const [formSubmitting, setFormSubmitting] = useState(false)
  const [error, setError] = useState('')
  const [message, setMessage] = useState('')

  const loadVehicles = async (page = 0, filterValues = filters) => {
    try {
      setLoading(true)
      setError('')
      const data = isResident
        ? await vehicleService.getMyVehicles()
        : await vehicleService.searchVehicles({
            page,
            size: 20,
            status: filterValues.status || undefined,
            type: filterValues.type || undefined,
            licensePlate: filterValues.licensePlate || undefined,
            apartmentId: filterValues.apartmentId || undefined,
            ownerId: filterValues.ownerId || undefined,
          })

      if (Array.isArray(data)) {
        const content = data
        setVehicles(content)
        setPageInfo({ page: 0, totalPages: 1, totalElements: content.length })
        if (isResident) {
          const fallbackApartments = buildFallbackApartmentsFromVehicles(content)
          setApartments((prev) => (prev.length > 0 ? prev : fallbackApartments))
          await loadApartmentDetails(fallbackApartments.map(getApartmentId))
        }
      } else {
        const content = normalizeCollection(data)
        setVehicles(content)
        setPageInfo({
          page: data?.number || 0,
          totalPages: data?.totalPages || 1,
          totalElements: data?.totalElements || 0,
        })
        if (isResident) {
          const fallbackApartments = buildFallbackApartmentsFromVehicles(content)
          setApartments((prev) => (prev.length > 0 ? prev : fallbackApartments))
          await loadApartmentDetails(fallbackApartments.map(getApartmentId))
        }
      }
    } catch (apiError) {
      setError(extractApiErrorMessage(apiError, 'Không thể tải dữ liệu phương tiện.'))
    } finally {
      setLoading(false)
    }
  }

  const loadLookups = async () => {
    if (!canManage) return
    try {
      setLookupLoading(true)
      setError('')
      const [apartmentData, userData] = await Promise.all([
        auth?.buildingId ? apartmentService.getApartmentsByBuildingId(auth.buildingId) : apartmentService.getAllApartments(),
        userService.getUsers(),
      ])
      setApartments(normalizeCollection(apartmentData))
      setUsers(normalizeCollection(userData))
    } catch (apiError) {
      setApartments([])
      setUsers([])
      setError(extractApiErrorMessage(apiError, 'Không thể tải danh sách căn hộ/cư dân để chọn.'))
    } finally {
      setLookupLoading(false)
    }
  }

  const loadResidentApartments = async () => {
    if (!isResident) return
    try {
      setLookupLoading(true)
      const data = await apartmentService.getMyApartments()
      const myApartments = normalizeCollection(data)
      if (myApartments.length > 0) {
        setApartments(myApartments)
        await loadApartmentDetails(myApartments.map(getApartmentId))
        if (myApartments.length === 1) {
          setVehicleForm((prev) => ({ ...prev, apartmentId: prev.apartmentId || getApartmentId(myApartments[0]) }))
        }
      }
    } catch (apiError) {
      setApartments((prev) => (prev.length > 0 ? prev : buildFallbackApartmentsFromVehicles(vehicles)))
      setError(extractApiErrorMessage(apiError, 'Không thể tải danh sách căn hộ của bạn. Trang sẽ dùng dữ liệu căn hộ từ các đăng ký xe hiện có nếu có.'))
    } finally {
      setLookupLoading(false)
    }
  }

  const loadApartmentDetails = async (apartmentIds = []) => {
    const uniqueApartmentIds = [...new Set(apartmentIds.filter(Boolean))]
    if (uniqueApartmentIds.length === 0) return

    const apartmentDetails = await Promise.all(
      uniqueApartmentIds.map(async (apartmentId) => {
        try {
          return await apartmentService.getApartmentById(apartmentId)
        } catch {
          return null
        }
      }),
    )

    const validApartmentDetails = apartmentDetails.filter(Boolean)
    if (validApartmentDetails.length > 0) {
      setApartments((prev) => mergeApartments(prev, validApartmentDetails))
    }
  }

  const loadLimits = async () => {
    if (!canManage) return
    try {
      setLimitLoading(true)
      const data = await vehicleService.getVehicleLimits(limitForm.apartmentId || undefined)
      setLimits(data)
    } catch (apiError) {
      setError(extractApiErrorMessage(apiError, 'Không thể tải cấu hình giới hạn xe.'))
    } finally {
      setLimitLoading(false)
    }
  }

  useEffect(() => {
    loadVehicles()
  }, [normalizedRole])

  useEffect(() => {
    if (canManage) loadLimits()
  }, [canManage])

  useEffect(() => {
    if (canManage) loadLookups()
  }, [canManage, auth?.buildingId])

  useEffect(() => {
    if (isResident) loadResidentApartments()
  }, [isResident])

  const apartmentById = useMemo(
    () => new Map(apartments.map((apartment) => [getApartmentId(apartment), apartment]).filter(([apartmentId]) => apartmentId)),
    [apartments],
  )

  const userById = useMemo(
    () => new Map(users.map((user) => [user.userId, user])),
    [users],
  )

  const residentUsers = useMemo(
    () => users.filter((user) => user.roleName === ROLE_KEYS.RESIDENT),
    [users],
  )

  const renderApartmentCell = (apartmentId) => {
    const apartment = apartmentById.get(apartmentId)
    return (
      <div>
        <strong>{apartment ? getApartmentDisplayName(apartment) : apartmentId || 'Căn hộ'}</strong>
        {apartment && getApartmentDisplayName(apartment) !== apartmentId ? <small>{apartmentId}</small> : null}
      </div>
    )
  }

  const renderOwnerCell = (ownerId) => {
    const owner = userById.get(ownerId)
    return (
      <div>
        <strong>{owner?.email || ownerId}</strong>
        {owner?.fullName ? <small>{owner.fullName}</small> : null}
      </div>
    )
  }

  const totalPending = useMemo(
    () => vehicles.filter((vehicle) => vehicle.status === 'PENDING').length,
    [vehicles],
  )

  const approvedVehicles = useMemo(
    () => vehicles.filter((vehicle) => vehicle.status === 'APPROVED'),
    [vehicles],
  )

  const submittedRequests = useMemo(
    () => vehicles.filter((vehicle) => vehicle.status !== 'APPROVED'),
    [vehicles],
  )

  const hasPendingRequest = totalPending > 0

  const updateFilter = (field, value) => {
    setFilters((prev) => ({ ...prev, [field]: value }))
    setMessage('')
  }

  const updateLimitForm = (field, value) => {
    setLimitForm((prev) => ({ ...prev, [field]: value }))
    setMessage('')
  }

  const updateVehicleForm = (field, value) => {
    setVehicleForm((prev) => ({ ...prev, [field]: value }))
    setFormErrors((prev) => ({ ...prev, [field]: '' }))
    setMessage('')
  }

  const resetVehicleForm = () => {
    setVehicleForm({
      ...emptyVehicleForm,
      apartmentId: apartments.length === 1 ? getApartmentId(apartments[0]) : '',
    })
    setEditingVehicleId('')
    setFormErrors({})
  }

  const handleSubmitVehicleForm = async (event) => {
    event.preventDefault()
    const payload = {
      apartmentId: vehicleForm.apartmentId.trim(),
      licensePlate: vehicleForm.licensePlate.trim().toUpperCase(),
      type: vehicleForm.type,
      brand: vehicleForm.brand.trim() || undefined,
    }

    const validationErrors = validateVehicleForm(payload)
    setFormErrors(validationErrors)
    if (Object.keys(validationErrors).length > 0) return

    try {
      setFormSubmitting(true)
      setError('')
      setMessage('')
      if (editingVehicleId) {
        await vehicleService.updateVehicle(editingVehicleId, payload)
        setMessage('Đã cập nhật đơn đăng ký xe. Đơn sẽ tiếp tục chờ manager duyệt.')
      } else {
        await vehicleService.registerVehicle(payload)
        setMessage('Đã gửi đơn đăng ký xe. Đơn mới hiển thị trong danh sách bên dưới.')
      }
      resetVehicleForm()
      await loadVehicles()
    } catch (apiError) {
      setError(extractApiErrorMessage(apiError, 'Không thể lưu đơn đăng ký xe. Vui lòng kiểm tra lại thông tin.'))
    } finally {
      setFormSubmitting(false)
    }
  }

  const handleEditVehicle = (vehicle) => {
    if (vehicle.status !== 'PENDING') return
    setEditingVehicleId(vehicle.vehicleId)
    setVehicleForm({
      apartmentId: getVehicleApartmentId(vehicle),
      licensePlate: vehicle.licensePlate || '',
      type: vehicle.type || 'MOTORBIKE',
      brand: vehicle.brand || '',
    })
    setFormErrors({})
    setMessage('Đang chỉnh sửa đơn chờ duyệt. Cập nhật thông tin rồi bấm lưu.')
  }

  const handleStatusAction = async (vehicleId, status) => {
    try {
      setError('')
      setMessage('')
      await vehicleService.updateVehicleStatus(vehicleId, status)
      setMessage(status === 'APPROVED' ? 'Đã duyệt đơn đăng ký gửi xe.' : 'Đã từ chối đơn đăng ký gửi xe.')
      await loadVehicles(pageInfo.page)
      await loadLimits()
    } catch (apiError) {
      setError(extractApiErrorMessage(apiError, 'Không thể cập nhật trạng thái xe.'))
    }
  }

  const handleCancel = async (vehicleId) => {
    try {
      setError('')
      setMessage('')
      await vehicleService.cancelVehicle(vehicleId)
      setMessage('Đã hủy đăng ký gửi xe.')
      if (editingVehicleId === vehicleId) resetVehicleForm()
      await loadVehicles()
    } catch (apiError) {
      setError(extractApiErrorMessage(apiError, 'Không thể hủy đăng ký xe.'))
    }
  }

  const handleSubmitLimit = async (event) => {
    event.preventDefault()
    try {
      setError('')
      setMessage('')
      const payload = {
        apartmentId: limitForm.apartmentId.trim(),
        vehicleType: limitForm.vehicleType,
        maxQuantity: Number(limitForm.maxQuantity),
      }
      if (limitForm.limitId) {
        await vehicleService.updateVehicleLimit(limitForm.limitId, payload)
        setMessage('Đã cập nhật giới hạn xe cho căn hộ.')
      } else {
        await vehicleService.createVehicleLimit(payload)
        setMessage('Đã tạo giới hạn xe cho căn hộ.')
      }
      setLimitForm(emptyLimitForm)
      await loadLimits()
    } catch (apiError) {
      setError(extractApiErrorMessage(apiError, 'Không thể lưu giới hạn xe.'))
    }
  }

  const handleEditLimit = (limit) => {
    setLimitForm({
      limitId: limit.limitId,
      apartmentId: limit.apartmentId,
      vehicleType: limit.vehicleType,
      maxQuantity: limit.maxQuantity,
    })
  }

  const handleDeleteLimit = async (limitId) => {
    if (!window.confirm('Bạn chắc chắn muốn xóa cấu hình giới hạn xe này?')) return
    try {
      setError('')
      setMessage('')
      await vehicleService.deleteVehicleLimit(limitId)
      setMessage('Đã xóa giới hạn xe.')
      await loadLimits()
    } catch (apiError) {
      setError(extractApiErrorMessage(apiError, 'Không thể xóa giới hạn xe.'))
    }
  }

  return (
    <div className="page-stack">
      <section className="page-header-card">
        <div>
          <span className="eyebrow">Vehicle management</span>
          <h1>{canManage ? 'Quản lý phương tiện' : 'Phương tiện của tôi'}</h1>
          <p>
            {canManage
              ? 'Duyệt đơn đăng ký gửi xe và quản lý giới hạn số lượng xe theo từng căn hộ trong phạm vi tòa nhà.'
              : 'Theo dõi trạng thái xe đã đăng ký và gửi/hủy đăng ký gửi xe.'}
          </p>
        </div>
      </section>

      {isResident ? (
        <section className="content-card">
          <div className="section-heading">
            <div>
              <span className="eyebrow">1. Approved vehicles</span>
              <h2>Xe hiện tại của tôi</h2>
              <p>Chỉ hiển thị các phương tiện đã được manager duyệt và đang được tính là xe hợp lệ của bạn.</p>
            </div>
          </div>

          <div className="info-grid">
            <article className="info-card">
              <strong>Xe đã duyệt</strong>
              <p>{approvedVehicles.length}</p>
            </article>
            <article className="info-card">
              <strong>Người dùng</strong>
              <p>{auth?.email || auth?.userId || 'Tài khoản hiện tại'}</p>
            </article>
          </div>

        
          {loading ? <div className="page-status">Đang tải dữ liệu phương tiện...</div> : null}

          {!loading && approvedVehicles.length === 0 ? (
            <div className="empty-state">
              <h3>Chưa có xe nào được duyệt</h3>
              <p>Khi manager approve đơn đăng ký, xe của bạn sẽ xuất hiện ở khu vực này.</p>
            </div>
          ) : null}

          {!loading && approvedVehicles.length > 0 ? (
            <div className="table-card">
              <table className="data-table">
                <thead>
                  <tr>
                    <th>Apartment</th>
                    <th>License Plate</th>
                    <th>Type</th>
                    <th>Brand</th>
                    <th>Status</th>
                    <th>Hủy đăng ký</th>
                  </tr>
                </thead>
                <tbody>
                  {approvedVehicles.map((vehicle) => (
                    <tr key={vehicle.vehicleId}>
                      <td>{renderApartmentCell(getVehicleApartmentId(vehicle))}</td>
                      <td>{vehicle.licensePlate}</td>
                      <td>{vehicle.type}</td>
                      <td>{vehicle.brand || '-'}</td>
                      <td><span className={statusClassMap[vehicle.status] || 'badge'}>{vehicle.status}</span></td>
                      <td>
                        <button className="btn btn-danger" type="button" onClick={() => handleCancel(vehicle.vehicleId)}>
                          Gửi đơn hủy đăng ký
                        </button>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          ) : null}
        </section>
      ) : null}

      {isResident ? (
        <section className="content-card">
          <div className="section-heading">
            <div>
              <span className="eyebrow">2. Submit request</span>
              <h2>{editingVehicleId ? 'Chỉnh sửa đơn đăng ký xe đang chờ duyệt' : 'Gửi đơn đăng ký xe / hủy đăng ký xe'}</h2>
              <p>
                Dùng form bên dưới để gửi đơn đăng ký xe mới. Nếu muốn hủy xe đã được duyệt, bấm nút
                “Gửi đơn hủy đăng ký” tại phần Xe hiện tại của tôi.
              </p>
            </div>
          </div>

          <div className="info-grid">
            <article className="info-card">
              <strong>Căn hộ của tôi</strong>
              <p>{lookupLoading ? 'Đang tải...' : `${apartments.length} căn hộ`}</p>
            </article>
            <article className="info-card">
              <strong>Đơn chờ duyệt</strong>
              <p>{totalPending}</p>
            </article>
            <article className="info-card">
              <strong>Có thể chỉnh sửa</strong>
              <p>{hasPendingRequest ? 'Có đơn PENDING' : 'Không có đơn PENDING'}</p>
            </article>
          </div>

          <form className="form-grid form-grid--two-columns" onSubmit={handleSubmitVehicleForm}>
            <label className="form-field">
              <span>Căn hộ *</span>
              <select value={vehicleForm.apartmentId} onChange={(event) => updateVehicleForm('apartmentId', event.target.value)} disabled={lookupLoading}>
                <option value="">{lookupLoading ? 'Đang tải căn hộ...' : 'Chọn căn hộ của bạn'}</option>
                {apartments.map((apartment) => (
                  <option key={getApartmentId(apartment)} value={getApartmentId(apartment)}>
                    {getApartmentDisplayName(apartment)}
                  </option>
                ))}
              </select>
              {formErrors.apartmentId ? <small className="field-error">{formErrors.apartmentId}</small> : <small>Danh sách này lấy từ các căn hộ đang gắn với tài khoản của bạn.</small>}
            </label>

            <article className="info-card">
              <strong>Chủ xe</strong>
              <p>{auth?.email || auth?.userId || 'Tài khoản hiện tại'}</p>
            </article>

            <label className="form-field">
              <span>Biển số *</span>
              <input value={vehicleForm.licensePlate} onChange={(event) => updateVehicleForm('licensePlate', event.target.value)} placeholder="59A-12345" />
              {formErrors.licensePlate ? <small className="field-error">{formErrors.licensePlate}</small> : <small>Ví dụ: 59A-12345, 30G1-67890.</small>}
            </label>

            <label className="form-field">
              <span>Loại xe *</span>
              <select value={vehicleForm.type} onChange={(event) => updateVehicleForm('type', event.target.value)}>
                <option value="MOTORBIKE">Motorbike</option>
                <option value="CAR">Car</option>
              </select>
              {formErrors.type ? <small className="field-error">{formErrors.type}</small> : null}
            </label>

            <label className="form-field form-field--full">
              <span>Brand / Model</span>
              <input value={vehicleForm.brand} onChange={(event) => updateVehicleForm('brand', event.target.value)} placeholder="Honda, Toyota..." />
              <small>Không bắt buộc. Có thể nhập hãng hoặc model xe để manager dễ nhận diện.</small>
            </label>

            <div className="form-actions form-field--full">
              <button className="btn btn-primary" type="submit" disabled={formSubmitting || apartments.length === 0}>
                {formSubmitting ? 'Đang lưu...' : editingVehicleId ? 'Lưu chỉnh sửa' : 'Gửi đơn đăng ký'}
              </button>
              <button className="btn btn-ghost" type="button" onClick={resetVehicleForm}>
                {editingVehicleId ? 'Hủy chỉnh sửa' : 'Xóa form'}
              </button>
            </div>
          </form>
        </section>
      ) : null}
      {error ? <div className="alert alert-error">{error}</div> : null}
      {message ? <div className="alert alert-success">{message}</div> : null}
      

      {isResident ? (
        <section className="content-card">
          <div className="section-heading">
            <div>
              <span className="eyebrow">3. Submitted requests</span>
              <h2>Các đơn đã gửi</h2>
              <p>Resident chỉ được chỉnh sửa hoặc hủy đơn khi đơn vẫn ở trạng thái PENDING, chưa được manager approve hoặc reject.</p>
            </div>
          </div>

          {!loading && submittedRequests.length === 0 ? (
            <div className="empty-state">
              <h3>Chưa có đơn nào đã gửi</h3>
              <p>Các đơn đăng ký/hủy đăng ký đang chờ xử lý hoặc đã bị từ chối sẽ hiển thị tại đây.</p>
            </div>
          ) : null}

          {!loading && submittedRequests.length > 0 ? (
            <div className="table-card">
              <table className="data-table">
                <thead>
                  <tr>
                    <th>Apartment</th>
                    <th>License Plate</th>
                    <th>Type</th>
                    <th>Brand</th>
                    <th>Status</th>
                    <th>Actions</th>
                  </tr>
                </thead>
                <tbody>
                  {submittedRequests.map((vehicle) => (
                    <tr key={vehicle.vehicleId}>
                      <td>{renderApartmentCell(getVehicleApartmentId(vehicle))}</td>
                      <td>{vehicle.licensePlate}</td>
                      <td>{vehicle.type}</td>
                      <td>{vehicle.brand || '-'}</td>
                      <td><span className={statusClassMap[vehicle.status] || 'badge'}>{vehicle.status}</span></td>
                      <td>
                        {vehicle.status === 'PENDING' ? (
                          <div className="table-actions">
                            <button className="btn btn-ghost" type="button" onClick={() => handleEditVehicle(vehicle)}>Chỉnh sửa</button>
                            <button className="btn btn-danger" type="button" onClick={() => handleCancel(vehicle.vehicleId)}>Hủy đơn</button>
                          </div>
                        ) : (
                          <span className="table-note">Manager đã xử lý, không thể chỉnh sửa</span>
                        )}
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          ) : null}
        </section>
      ) : null}

      {!isResident ? (
      <section className="content-card">
        <div className="info-grid">
          <article className="info-card">
            <strong>{canManage ? 'Đơn chờ duyệt' : 'Tổng xe của tôi'}</strong>
            <p>{canManage ? totalPending : vehicles.length}</p>
          </article>
          <article className="info-card">
            <strong>Người dùng</strong>
            <p>{auth?.email || auth?.userId || 'Unknown'}</p>
          </article>
        </div>

        {canManage ? (
          <div className="toolbar-grid">
            <label className="form-field">
              <span>Status</span>
              <select value={filters.status} onChange={(event) => updateFilter('status', event.target.value)}>
                <option value="">All</option>
                <option value="PENDING">PENDING</option>
                <option value="APPROVED">APPROVED</option>
                <option value="REJECTED">REJECTED</option>
                <option value="CANCELLED">CANCELLED</option>
                <option value="INACTIVE">INACTIVE</option>
              </select>
            </label>
            <label className="form-field">
              <span>Type</span>
              <select value={filters.type} onChange={(event) => updateFilter('type', event.target.value)}>
                <option value="">All</option>
                <option value="MOTORBIKE">MOTORBIKE</option>
                <option value="CAR">CAR</option>
              </select>
            </label>
            <label className="form-field">
              <span>License plate</span>
              <input value={filters.licensePlate} onChange={(event) => updateFilter('licensePlate', event.target.value)} placeholder="29A-12345" />
            </label>
            <label className="form-field">
              <span>Căn hộ</span>
              <select value={filters.apartmentId} onChange={(event) => updateFilter('apartmentId', event.target.value)} disabled={lookupLoading}>
                <option value="">{lookupLoading ? 'Đang tải căn hộ...' : 'Tất cả căn hộ'}</option>
                {apartments.map((apartment) => (
                  <option key={getApartmentId(apartment)} value={getApartmentId(apartment)}>
                    {getApartmentDisplayName(apartment)}
                  </option>
                ))}
              </select>
            </label>
            <label className="form-field">
              <span>Email cư dân</span>
              <select value={filters.ownerId} onChange={(event) => updateFilter('ownerId', event.target.value)} disabled={lookupLoading}>
                <option value="">{lookupLoading ? 'Đang tải cư dân...' : 'Tất cả cư dân'}</option>
                {residentUsers.map((user) => (
                  <option key={user.userId} value={user.userId}>
                    {getResidentLabel(user)}
                  </option>
                ))}
              </select>
            </label>
            <div className="toolbar-actions">
              <button className="btn btn-primary" type="button" onClick={() => loadVehicles()}>
                Lọc danh sách
              </button>
              <button className="btn btn-ghost" type="button" onClick={() => { setFilters(emptyFilters); loadVehicles(0, emptyFilters) }}>
                Xóa lọc
              </button>
            </div>
          </div>
        ) : null}

        
        {loading ? <div className="page-status">Đang tải dữ liệu phương tiện...</div> : null}

        {!loading && vehicles.length === 0 ? (
          <div className="empty-state">
            <h3>Chưa có dữ liệu phương tiện</h3>
            <p>{canManage ? 'Không có vehicle phù hợp với bộ lọc hiện tại.' : 'Bạn chưa có đăng ký gửi xe nào.'}</p>
          </div>
        ) : null}

        {!loading && vehicles.length > 0 ? (
          <div className="table-card">
            <table className="data-table">
              <thead>
                <tr>
                  <th>Apartment</th>
                  {canManage ? <th>Owner</th> : null}
                  <th>License Plate</th>
                  <th>Type</th>
                  <th>Brand</th>
                  <th>Status</th>
                  <th>Actions</th>
                </tr>
              </thead>
              <tbody>
                {vehicles.map((vehicle) => (
                  <tr key={vehicle.vehicleId}>
                    <td>{renderApartmentCell(getVehicleApartmentId(vehicle))}</td>
                    {canManage ? <td>{renderOwnerCell(getVehicleOwnerId(vehicle))}</td> : null}
                    <td>{vehicle.licensePlate}</td>
                    <td>{vehicle.type}</td>
                    <td>{vehicle.brand || '-'}</td>
                    <td><span className={statusClassMap[vehicle.status] || 'badge'}>{vehicle.status}</span></td>
                    <td>
                      {canManage && vehicle.status === 'PENDING' ? (
                        <div className="table-actions">
                          <button className="btn btn-success" type="button" onClick={() => handleStatusAction(vehicle.vehicleId, 'APPROVED')}>Approve</button>
                          <button className="btn btn-danger" type="button" onClick={() => handleStatusAction(vehicle.vehicleId, 'REJECTED')}>Reject</button>
                        </div>
                      ) : null}
                      {isResident && vehicle.status === 'PENDING' ? (
                        <div className="table-actions">
                          <button className="btn btn-ghost" type="button" onClick={() => handleEditVehicle(vehicle)}>Chỉnh sửa</button>
                          <button className="btn btn-danger" type="button" onClick={() => handleCancel(vehicle.vehicleId)}>Hủy đơn</button>
                        </div>
                      ) : null}
                      {(!canManage || vehicle.status !== 'PENDING') && (!isResident || vehicle.status !== 'PENDING') ? <span className="table-note">Không có thao tác</span> : null}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        ) : null}

        {canManage && pageInfo.totalPages > 1 ? (
          <div className="form-actions">
            <button className="btn btn-ghost" type="button" disabled={pageInfo.page <= 0} onClick={() => loadVehicles(pageInfo.page - 1)}>Trang trước</button>
            <span className="table-note">Trang {pageInfo.page + 1}/{pageInfo.totalPages} · {pageInfo.totalElements} bản ghi</span>
            <button className="btn btn-ghost" type="button" disabled={pageInfo.page + 1 >= pageInfo.totalPages} onClick={() => loadVehicles(pageInfo.page + 1)}>Trang sau</button>
          </div>
        ) : null}
      </section>
      ) : null}

      {canManage ? (
        <section className="content-card">
          <div className="section-heading">
            <div>
              <span className="eyebrow">Vehicle limits</span>
              <h2>Giới hạn xe theo căn hộ</h2>
            </div>
            <button className="btn btn-ghost" type="button" onClick={loadLimits}>Làm mới limit</button>
          </div>

          <form className="form-grid form-grid--two-columns" onSubmit={handleSubmitLimit}>
            <label className="form-field">
              <span>Căn hộ *</span>
              <select value={limitForm.apartmentId} onChange={(event) => updateLimitForm('apartmentId', event.target.value)} required disabled={lookupLoading}>
                <option value="">{lookupLoading ? 'Đang tải căn hộ...' : 'Chọn căn hộ'}</option>
                {apartments.map((apartment) => (
                  <option key={getApartmentId(apartment)} value={getApartmentId(apartment)}>
                    {getApartmentDisplayName(apartment)}
                  </option>
                ))}
              </select>
              <small>Chọn theo số phòng để không cần nhập UUID.</small>
            </label>
            <label className="form-field">
              <span>Vehicle type *</span>
              <select value={limitForm.vehicleType} onChange={(event) => updateLimitForm('vehicleType', event.target.value)}>
                <option value="MOTORBIKE">MOTORBIKE</option>
                <option value="CAR">CAR</option>
              </select>
            </label>
            <label className="form-field">
              <span>Max quantity *</span>
              <input type="number" min="0" value={limitForm.maxQuantity} onChange={(event) => updateLimitForm('maxQuantity', event.target.value)} required />
            </label>
            <div className="form-actions">
              <button className="btn btn-primary" type="submit">{limitForm.limitId ? 'Cập nhật limit' : 'Tạo limit'}</button>
              <button className="btn btn-ghost" type="button" onClick={() => setLimitForm(emptyLimitForm)}>Clear</button>
            </div>
          </form>

          {limitLoading ? <div className="page-status">Đang tải cấu hình limit...</div> : null}
          {!limitLoading && limits.length > 0 ? (
            <div className="table-card">
              <table className="data-table">
                <thead>
                  <tr><th>Apartment</th><th>Type</th><th>Approved</th><th>Max</th><th>Actions</th></tr>
                </thead>
                <tbody>
                  {limits.map((limit) => (
                    <tr key={limit.limitId}>
                      <td>{renderApartmentCell(limit.apartmentId)}</td>
                      <td>{limit.vehicleType}</td>
                      <td>{limit.approvedVehicleCount}</td>
                      <td>{limit.maxQuantity}</td>
                      <td>
                        <div className="table-actions">
                          <button className="btn btn-ghost" type="button" onClick={() => handleEditLimit(limit)}>Edit</button>
                          <button className="btn btn-danger" type="button" onClick={() => handleDeleteLimit(limit.limitId)}>Delete</button>
                        </div>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          ) : null}
        </section>
      ) : null}
    </div>
  )
}
