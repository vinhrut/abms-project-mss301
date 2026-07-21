/* eslint-disable react-hooks/set-state-in-effect */
import { useEffect, useMemo, useState } from 'react'
import { ROLE_KEYS } from '../../../config/roles.js'
import { apartmentService } from '../../../services/apartmentService.js'
import { userService } from '../../../services/userService.js'
import { vehicleService } from '../../../services/vehicleService.js'
import { useAuth } from '../../auth/context/useAuth.js'
import { extractApiErrorMessage } from '../../../utils/apiError.js'
import { isValidUuid } from '../../../utils/validation.js'
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

function getValidApartmentId(apartment) {
  const apartmentId = getApartmentId(apartment)
  return isValidUuid(apartmentId) ? apartmentId : ''
}

function getApartmentDisplayName(apartment) {
  if (typeof apartment === 'string') return 'Căn hộ chưa xác định'
  return apartment?.roomNumber || apartment?.apartmentNumber || apartment?.code || apartment?.name || 'Căn hộ chưa xác định'
}

function getVehicleApartmentId(vehicle) {
  return vehicle?.apartmentId || vehicle?.apartment_id || vehicle?.apartment?.apartmentId || vehicle?.apartment?.id || ''
}

function getVehicleOwnerId(vehicle) {
  return vehicle?.ownerId || vehicle?.owner_id || vehicle?.owner?.userId || vehicle?.owner?.id || ''
}

function buildFallbackApartmentsFromVehicles(vehicleItems) {
  const apartmentIds = [...new Set(vehicleItems.map(getVehicleApartmentId).filter(Boolean))]
  return apartmentIds.map((apartmentId) => ({ apartmentId, roomNumber: 'Căn hộ chưa xác định' }))
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

const statusLabelMap = {
  PENDING: 'Chờ duyệt',
  PENDING_CANCEL: 'Chờ duyệt hủy',
  APPROVED: 'Đã duyệt',
  REJECTED: 'Từ chối',
  CANCELLED: 'Đã hủy',
  INACTIVE: 'Không hoạt động',
}

const vehicleTypeLabelMap = {
  MOTORBIKE: 'Xe máy',
  CAR: 'Ô tô',
}

const statusClassMap = {
  PENDING: 'badge badge-warning',
  PENDING_CANCEL: 'badge badge-warning',
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
}

const residentModalPageSize = 3

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
  const [residentVehicleModal, setResidentVehicleModal] = useState('')
  const [approvedVehiclePage, setApprovedVehiclePage] = useState(0)
  const [submittedRequestPage, setSubmittedRequestPage] = useState(0)

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
          setVehicleForm((prev) => ({ ...prev, apartmentId: prev.apartmentId || getValidApartmentId(myApartments[0]) }))
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

  const selectableResidentApartments = useMemo(
    () => apartments.filter((apartment) => getValidApartmentId(apartment)),
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
        <strong>{apartment ? getApartmentDisplayName(apartment) : 'Căn hộ chưa xác định'}</strong>
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

  const approvedVehicleTotalPages = Math.max(1, Math.ceil(approvedVehicles.length / residentModalPageSize))
  const submittedRequestTotalPages = Math.max(1, Math.ceil(submittedRequests.length / residentModalPageSize))

  const paginatedApprovedVehicles = useMemo(
    () => approvedVehicles.slice(
      approvedVehiclePage * residentModalPageSize,
      approvedVehiclePage * residentModalPageSize + residentModalPageSize,
    ),
    [approvedVehicles, approvedVehiclePage],
  )

  const paginatedSubmittedRequests = useMemo(
    () => submittedRequests.slice(
      submittedRequestPage * residentModalPageSize,
      submittedRequestPage * residentModalPageSize + residentModalPageSize,
    ),
    [submittedRequests, submittedRequestPage],
  )

  useEffect(() => {
    setApprovedVehiclePage((page) => Math.min(page, approvedVehicleTotalPages - 1))
  }, [approvedVehicleTotalPages])

  useEffect(() => {
    setSubmittedRequestPage((page) => Math.min(page, submittedRequestTotalPages - 1))
  }, [submittedRequestTotalPages])

  const openResidentVehicleModal = (modalName) => {
    if (modalName === 'approved') setApprovedVehiclePage(0)
    if (modalName === 'submitted') setSubmittedRequestPage(0)
    setResidentVehicleModal(modalName)
  }

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
      apartmentId: selectableResidentApartments.length === 1 ? getValidApartmentId(selectableResidentApartments[0]) : '',
    })
    setEditingVehicleId('')
    setFormErrors({})
  }

  const handleSubmitVehicleForm = async (event) => {
    event.preventDefault()
    const selectedApartmentId = vehicleForm.apartmentId.trim()
    const validSelectedApartmentId = isValidUuid(selectedApartmentId) ? selectedApartmentId : ''
    const payload = {
      licensePlate: vehicleForm.licensePlate.trim().toUpperCase(),
      type: vehicleForm.type,
    }
    if (validSelectedApartmentId) {
      payload.apartmentId = validSelectedApartmentId
    }

    const validationErrors = validateVehicleForm(
      { ...payload, apartmentId: validSelectedApartmentId },
      { allowApartmentFallback: isResident && selectableResidentApartments.length === 0 },
    )
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
    })
    setFormErrors({})
    setResidentVehicleModal('')
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
    <div className="page-stack vehicle-page">
      {/* <section className="page-header-card vehicle-page__header">
        <div>
          <span className="eyebrow">Quản lý phương tiện</span>
          <h1>{canManage ? 'Quản lý phương tiện' : 'Phương tiện của tôi'}</h1>
          <p>
            {canManage
              ? 'Duyệt đơn đăng ký gửi xe và quản lý giới hạn số lượng xe theo từng căn hộ trong phạm vi tòa nhà.'
              : 'Theo dõi trạng thái xe đã đăng ký và gửi/hủy đăng ký gửi xe.'}
          </p>
        </div>
      </section>  */}

      {isResident ? (
        <section className="content-card vehicle-register-card">
          <div className="section-heading">
            <div>
              <h2>{editingVehicleId ? 'Chỉnh sửa đơn đăng ký xe' : 'Đăng ký phương tiện mới'}</h2>
              <p>
                Vui lòng điền chính xác thông tin phương tiện để Ban Quản Lý phê duyệt.
              </p>
            </div>
          </div>

          <form className="vehicle-register-form" onSubmit={handleSubmitVehicleForm}>
            <label className="form-field vehicle-form-field">
              <span>Chọn căn hộ</span>
              <select value={vehicleForm.apartmentId} onChange={(event) => updateVehicleForm('apartmentId', event.target.value)} disabled={lookupLoading}>
                <option value="">{lookupLoading ? 'Đang tải căn hộ...' : 'Chọn căn hộ của bạn'}</option>
                {selectableResidentApartments.map((apartment) => (
                  <option key={getValidApartmentId(apartment)} value={getValidApartmentId(apartment)}>
                    {getApartmentDisplayName(apartment)}
                  </option>
                ))}
              </select>
              {formErrors.apartmentId ? (
                <small className="field-error">{formErrors.apartmentId}</small>
              ) : selectableResidentApartments.length === 0 && !lookupLoading ? (
                <small>Không tải được danh sách căn hộ. Hệ thống sẽ tự kiểm tra căn hộ ACTIVE của tài khoản khi gửi đơn.</small>
              ) : (
                <small>Danh sách này lấy từ các căn hộ đang gắn với tài khoản của bạn.</small>
              )}
            </label>

            <label className="form-field vehicle-form-field">
              <span>Biển số xe</span>
              <input value={vehicleForm.licensePlate} onChange={(event) => updateVehicleForm('licensePlate', event.target.value)} placeholder="59A-12345" />
              {formErrors.licensePlate ? <small className="field-error">{formErrors.licensePlate}</small> : <small>Ví dụ: 59A-12345, 30G1-67890.</small>}
            </label>

            <label className="form-field vehicle-form-field vehicle-form-field--full">
              <span>Loại phương tiện</span>
              <div className="vehicle-type-options">
                <button
                  type="button"
                  className={`vehicle-type-option ${vehicleForm.type === 'MOTORBIKE' ? 'vehicle-type-option--active' : ''}`}
                  onClick={() => updateVehicleForm('type', 'MOTORBIKE')}
                >
                  <span className="vehicle-radio-dot" />
                  <span>🏍️</span>
                  <strong>Xe máy</strong>
                </button>
                <button
                  type="button"
                  className={`vehicle-type-option ${vehicleForm.type === 'CAR' ? 'vehicle-type-option--active' : ''}`}
                  onClick={() => updateVehicleForm('type', 'CAR')}
                >
                  <span className="vehicle-radio-dot" />
                  <span>🚗</span>
                  <strong>Ô tô</strong>
                </button>
              </div>
              {formErrors.type ? <small className="field-error">{formErrors.type}</small> : null}
            </label>

            <div className="vehicle-register-actions">
              <button className="btn btn-ghost" type="button" onClick={resetVehicleForm}>
                {editingVehicleId ? 'Hủy chỉnh sửa' : 'Hủy'}
              </button>
              <button className="btn btn-primary" type="submit" disabled={formSubmitting || lookupLoading}>
                {formSubmitting ? 'Đang lưu...' : editingVehicleId ? 'Lưu chỉnh sửa' : 'Gửi đơn đăng ký'}
              </button>
            </div>
          </form>
        </section>
      ) : null}
        

      {error ? <div className="alert alert-error">{error}</div> : null}
      {message ? <div className="alert alert-success">{message}</div> : null}
      

      {isResident && residentVehicleModal === 'approved' ? (
        <div className="modal-backdrop" role="presentation" onClick={() => setResidentVehicleModal('')}>
          <div className="modal-card vehicle-modal-card" role="dialog" aria-modal="true" aria-labelledby="approved-vehicles-title" onClick={(event) => event.stopPropagation()}>
            <div className="section-heading vehicle-modal-heading">
              <div>
                <span className="eyebrow">Xe đã duyệt</span>
                <h2 id="approved-vehicles-title">Xe hiện tại của tôi</h2>
                <p>Chỉ hiển thị các phương tiện đã được quản lý duyệt và đang được tính là xe hợp lệ của bạn.</p>
              </div>
              <button className="btn btn-ghost" type="button" onClick={() => setResidentVehicleModal('')}>Đóng</button>
            </div>

            {loading ? <div className="page-status">Đang tải dữ liệu phương tiện...</div> : null}

            {!loading && approvedVehicles.length === 0 ? (
              <div className="empty-state">
                <h3>Chưa có xe nào được duyệt</h3>
                <p>Khi quản lý duyệt đơn đăng ký, xe của bạn sẽ xuất hiện ở khu vực này.</p>
              </div>
            ) : null}

            {!loading && approvedVehicles.length > 0 ? (
              <div className="table-card vehicle-modal-table-card">
                <table className="data-table">
                  <thead>
                    <tr>
                      <th>Căn hộ</th>
                      <th>Biển số</th>
                      <th>Loại xe</th>
                      <th>Trạng thái</th>
                      <th>Hủy đăng ký</th>
                    </tr>
                  </thead>
                  <tbody>
                    {paginatedApprovedVehicles.map((vehicle) => (
                      <tr key={vehicle.vehicleId}>
                        <td>{renderApartmentCell(getVehicleApartmentId(vehicle))}</td>
                        <td>{vehicle.licensePlate}</td>
                        <td>{vehicleTypeLabelMap[vehicle.type] || vehicle.type}</td>
                        <td><span className={statusClassMap[vehicle.status] || 'badge'}>{statusLabelMap[vehicle.status] || vehicle.status}</span></td>
                        <td>
                          <button className="btn btn-danger" type="button" onClick={() => handleCancel(vehicle.vehicleId)}>
                            Gửi đơn hủy đăng ký
                          </button>
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
                <div className="form-actions vehicle-modal-pagination">
                  <button className="btn btn-ghost" type="button" disabled={approvedVehiclePage <= 0} onClick={() => setApprovedVehiclePage((page) => Math.max(0, page - 1))}>
                    Trang trước
                  </button>
                  <span className="table-note">Trang {approvedVehiclePage + 1}/{approvedVehicleTotalPages} · {approvedVehicles.length} bản ghi</span>
                  <button className="btn btn-ghost" type="button" disabled={approvedVehiclePage + 1 >= approvedVehicleTotalPages} onClick={() => setApprovedVehiclePage((page) => Math.min(approvedVehicleTotalPages - 1, page + 1))}>
                    Trang sau
                  </button>
                </div>
              </div>
            ) : null}
          </div>
        </div>
      ) : null}

      {isResident && residentVehicleModal === 'submitted' ? (
        <div className="modal-backdrop" role="presentation" onClick={() => setResidentVehicleModal('')}>
          <div className="modal-card vehicle-modal-card" role="dialog" aria-modal="true" aria-labelledby="submitted-requests-title" onClick={(event) => event.stopPropagation()}>
            <div className="section-heading vehicle-modal-heading">
              <div>
                <span className="eyebrow">Đơn đã gửi</span>
                <h2 id="submitted-requests-title">Các đơn đã gửi</h2>
                <p>Cư dân chỉ được chỉnh sửa hoặc hủy đơn khi đơn vẫn ở trạng thái chờ duyệt, chưa được quản lý xử lý.</p>
              </div>
              <button className="btn btn-ghost" type="button" onClick={() => setResidentVehicleModal('')}>Đóng</button>
            </div>

            {loading ? <div className="page-status">Đang tải dữ liệu phương tiện...</div> : null}

            {!loading && submittedRequests.length === 0 ? (
              <div className="empty-state">
                <h3>Chưa có đơn nào đã gửi</h3>
                <p>Các đơn đăng ký/hủy đăng ký đang chờ xử lý hoặc đã bị từ chối sẽ hiển thị tại đây.</p>
              </div>
            ) : null}

            {!loading && submittedRequests.length > 0 ? (
              <div className="table-card vehicle-modal-table-card">
                <table className="data-table">
                  <thead>
                    <tr>
                      <th>Căn hộ</th>
                      <th>Biển số</th>
                      <th>Loại xe</th>
                      <th>Trạng thái</th>
                      <th>Thao tác</th>
                    </tr>
                  </thead>
                  <tbody>
                    {paginatedSubmittedRequests.map((vehicle) => (
                      <tr key={vehicle.vehicleId}>
                        <td>{renderApartmentCell(getVehicleApartmentId(vehicle))}</td>
                        <td>{vehicle.licensePlate}</td>
                        <td>{vehicleTypeLabelMap[vehicle.type] || vehicle.type}</td>
                        <td><span className={statusClassMap[vehicle.status] || 'badge'}>{statusLabelMap[vehicle.status] || vehicle.status}</span></td>
                        <td>
                          {vehicle.status === 'PENDING' ? (
                            <div className="table-actions">
                              <button className="btn btn-ghost" type="button" onClick={() => handleEditVehicle(vehicle)}>Chỉnh sửa</button>
                              <button className="btn btn-danger" type="button" onClick={() => handleCancel(vehicle.vehicleId)}>Hủy đơn</button>
                            </div>
                          ) : (
                            <span className="table-note">Quản lý đã xử lý, không thể chỉnh sửa</span>
                          )}
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
                <div className="form-actions vehicle-modal-pagination">
                  <button className="btn btn-ghost" type="button" disabled={submittedRequestPage <= 0} onClick={() => setSubmittedRequestPage((page) => Math.max(0, page - 1))}>
                    Trang trước
                  </button>
                  <span className="table-note">Trang {submittedRequestPage + 1}/{submittedRequestTotalPages} · {submittedRequests.length} bản ghi</span>
                  <button className="btn btn-ghost" type="button" disabled={submittedRequestPage + 1 >= submittedRequestTotalPages} onClick={() => setSubmittedRequestPage((page) => Math.min(submittedRequestTotalPages - 1, page + 1))}>
                    Trang sau
                  </button>
                </div>
              </div>
            ) : null}
          </div>
        </div>
      ) : null}

      {!isResident ? (
      <section className="content-card vehicle-management-panel">
        <div className="vehicle-management-header">
          <div>
            <span className="eyebrow">Vehicle management</span>
            <h2>Quản lý phương tiện</h2>
          </div>
          {canManage ? (
            <label className="vehicle-search-box">
              <span>🔎</span>
              <input value={filters.licensePlate} onChange={(event) => updateFilter('licensePlate', event.target.value)} placeholder="Tìm biển số/căn hộ..." />
            </label>
          ) : null}
        </div>
        {/* <div className="info-grid">
          <article className="info-card">
            <strong>{canManage ? 'Đơn chờ duyệt' : 'Tổng xe của tôi'}</strong>
            <p>{canManage ? totalPending : vehicles.length}</p>
          </article>
          <article className="info-card">
            <strong>Người dùng</strong>
            <p>{auth?.email || auth?.userId || 'Tài khoản hiện tại'}</p>
          </article>
        </div> */}

        {canManage ? (
          <div className="toolbar-grid vehicle-filter-bar">
            {/* <label className="form-field">
              <span>Trạng thái</span>
              <select value={filters.status} onChange={(event) => updateFilter('status', event.target.value)}>
                <option value="">Tất cả</option>
                <option value="PENDING">Chờ duyệt</option>
                <option value="PENDING_CANCEL">Chờ duyệt hủy</option>
                <option value="APPROVED">Đã duyệt</option>
                <option value="REJECTED">Từ chối</option>
                <option value="CANCELLED">Đã hủy</option>
                <option value="INACTIVE">Không hoạt động</option>
              </select>
            </label>
            <label className="form-field">
              <span>Loại xe</span>
              <select value={filters.type} onChange={(event) => updateFilter('type', event.target.value)}>
                <option value="">Tất cả</option>
                <option value="MOTORBIKE">Xe máy</option>
                <option value="CAR">Ô tô</option>
              </select>
            </label> */}
            {/* <label className="form-field vehicle-filter-bar__hide-on-compact">
              <span>Biển số</span>
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
            </div> */}
          </div>
        ) : null}

        
        {loading ? <div className="page-status">Đang tải dữ liệu phương tiện...</div> : null}

        {!loading && vehicles.length === 0 ? (
          <div className="empty-state">
            <h3>Chưa có dữ liệu phương tiện</h3>
            <p>{canManage ? 'Không có phương tiện phù hợp với bộ lọc hiện tại.' : 'Bạn chưa có đăng ký gửi xe nào.'}</p>
          </div>
        ) : null}

        {!loading && vehicles.length > 0 ? (
          <div className="table-card vehicle-table-card">
            <table className="data-table vehicle-management-table">
              <thead>
                <tr>
                  <th>Căn hộ</th>
                  <th>Biển số</th>
                  <th>Loại xe</th>
                  <th>Trạng thái</th>
                  <th>Thao tác</th>
                </tr>
              </thead>
              <tbody>
                {vehicles.map((vehicle) => (
                  <tr key={vehicle.vehicleId}>
                    <td>{renderApartmentCell(getVehicleApartmentId(vehicle))}</td>
                    <td>{vehicle.licensePlate}</td>
                    <td>{vehicleTypeLabelMap[vehicle.type] || vehicle.type}</td>
                    <td><span className={statusClassMap[vehicle.status] || 'badge'}>{statusLabelMap[vehicle.status] || vehicle.status}</span></td>
                    <td>
                      {canManage && vehicle.status === 'PENDING' ? (
                        <div className="table-actions">
                          <button className="vehicle-icon-btn vehicle-icon-btn--approve" type="button" onClick={() => handleStatusAction(vehicle.vehicleId, 'APPROVED')} title="Duyệt">✓</button>
                          <button className="vehicle-icon-btn vehicle-icon-btn--reject" type="button" onClick={() => handleStatusAction(vehicle.vehicleId, 'REJECTED')} title="Từ chối">×</button>
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
              <span className="eyebrow">Giới hạn phương tiện</span>
              <h2>Giới hạn xe theo căn hộ</h2>
            </div>
            <button className="btn btn-ghost" type="button" onClick={loadLimits}>Làm mới giới hạn</button>
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
              <span>Loại xe *</span>
              <select value={limitForm.vehicleType} onChange={(event) => updateLimitForm('vehicleType', event.target.value)}>
                <option value="MOTORBIKE">Xe máy</option>
                <option value="CAR">Ô tô</option>
              </select>
            </label>
            <label className="form-field">
              <span>Số lượng tối đa *</span>
              <input type="number" min="0" value={limitForm.maxQuantity} onChange={(event) => updateLimitForm('maxQuantity', event.target.value)} required />
            </label>
            <div className="form-actions">
              <button className="btn btn-primary" type="submit">{limitForm.limitId ? 'Cập nhật giới hạn' : 'Tạo giới hạn'}</button>
              <button className="btn btn-ghost" type="button" onClick={() => setLimitForm(emptyLimitForm)}>Xóa form</button>
            </div>
          </form>

          {limitLoading ? <div className="page-status">Đang tải cấu hình giới hạn...</div> : null}
          {!limitLoading && limits.length > 0 ? (
            <div className="table-card">
              <table className="data-table">
                <thead>
                  <tr><th>Căn hộ</th><th>Loại xe</th><th>Đã duyệt</th><th>Tối đa</th><th>Thao tác</th></tr>
                </thead>
                <tbody>
                  {limits.map((limit) => (
                    <tr key={limit.limitId}>
                      <td>{renderApartmentCell(limit.apartmentId)}</td>
                      <td>{vehicleTypeLabelMap[limit.vehicleType] || limit.vehicleType}</td>
                      <td>{limit.approvedVehicleCount}</td>
                      <td>{limit.maxQuantity}</td>
                      <td>
                        <div className="table-actions">
                          <button className="btn btn-ghost" type="button" onClick={() => handleEditLimit(limit)}>Sửa</button>
                          <button className="btn btn-danger" type="button" onClick={() => handleDeleteLimit(limit.limitId)}>Xóa</button>
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
