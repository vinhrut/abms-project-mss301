/* eslint-disable react-hooks/set-state-in-effect */
import { useEffect, useMemo, useState } from 'react'
import { PageIntro } from '../../../components/ui/PageIntro.jsx'
import { ROLE_KEYS } from '../../../config/roles.js'
import { apartmentService } from '../../../services/apartmentService.js'
import { userService } from '../../../services/userService.js'
import { extractApiErrorMessage } from '../../../utils/apiError.js'
import { useAuth } from '../../auth/context/useAuth.js'

function normalizeCollection(data) {
  if (Array.isArray(data)) {
    return data
  }

  if (Array.isArray(data?.content)) {
    return data.content
  }

  if (Array.isArray(data?.items)) {
    return data.items
  }

  return []
}

function getBuildingLabel(building) {
  const name = building?.name || building?.buildingName || building?.code || 'Tòa nhà'
  const code = building?.code ? ` (${building.code})` : ''
  return name === building?.code ? name : `${name}${code}`
}

function getApartmentLabel(apartment) {
  return apartment?.roomNumber || apartment?.apartmentNumber || apartment?.code || apartment?.name || 'Căn hộ'
}

const roleLabelMap = {
  [ROLE_KEYS.ADMIN]: 'Quản trị viên',
  [ROLE_KEYS.MANAGER]: 'Quản lý',
  [ROLE_KEYS.RESIDENT]: 'Cư dân',
  [ROLE_KEYS.STAFF]: 'Nhân viên',
  [ROLE_KEYS.TECHNICIAN]: 'Kỹ thuật viên',
}

const statusLabelMap = {
  ACTIVE: 'Đang hoạt động',
  LOCKED: 'Đã khóa',
  PENDING: 'Chờ xử lý',
}

const initialCreateState = {
  roleName: ROLE_KEYS.MANAGER,
  email: '',
  password: '11111111',
  fullName: '',
  phone: '',
  idCard: '',
  buildingId: '',
  apartmentId: '',
  relationship: 'OWNER',
  residenceType: 'PERMANENT',
}

const roleOptionsForManager = [ROLE_KEYS.RESIDENT, ROLE_KEYS.STAFF, ROLE_KEYS.TECHNICIAN]

function getStatusClass(status) {
  if (status === 'ACTIVE') return 'badge badge-success'
  if (status === 'LOCKED') return 'badge badge-danger'
  return 'badge badge-warning'
}

export function UserManagementPage() {
  const { auth, normalizedRole } = useAuth()
  const [users, setUsers] = useState([])
  const [buildings, setBuildings] = useState([])
  const [apartments, setApartments] = useState([])
  const [formData, setFormData] = useState(initialCreateState)
  const [loading, setLoading] = useState(false)
  const [loadingBuildings, setLoadingBuildings] = useState(false)
  const [loadingApartments, setLoadingApartments] = useState(false)
  const [submitting, setSubmitting] = useState(false)
  const [error, setError] = useState('')
  const [message, setMessage] = useState('')
  const [buildingLoadError, setBuildingLoadError] = useState('')
  const [apartmentLoadError, setApartmentLoadError] = useState('')

  const isAdmin = normalizedRole === ROLE_KEYS.ADMIN
  const isManager = normalizedRole === ROLE_KEYS.MANAGER
  const managerBuildingId = useMemo(() => {
    if (auth?.buildingId) {
      return auth.buildingId
    }

    const currentUser = users.find((user) => user.userId === auth?.userId)
    return currentUser?.buildingId || ''
  }, [auth?.buildingId, auth?.userId, users])

  const visibleUsers = useMemo(() => users, [users])

  const buildingById = useMemo(
    () => new Map(buildings.map((building) => [building.buildingId, building]).filter(([buildingId]) => buildingId)),
    [buildings],
  )

  const managerBuildingLabel = useMemo(() => {
    const found = buildingById.get(managerBuildingId)
    if (found) return getBuildingLabel(found)
    return managerBuildingId ? 'Tòa nhà được phân quyền' : ''
  }, [buildingById, managerBuildingId])

  const getUserBuildingDisplay = (user) => {
    if (!user?.buildingId) return '-'
    const found = buildingById.get(user.buildingId)
    return found ? getBuildingLabel(found) : 'Tòa nhà được phân quyền'
  }

  useEffect(() => {
    const loadUsers = async () => {
      try {
        setLoading(true)
        setError('')
        const usersData = await userService.getUsers()
        setUsers(normalizeCollection(usersData))
      } catch (apiError) {
        setError(extractApiErrorMessage(apiError, 'Không thể tải danh sách tài khoản.'))
      } finally {
        setLoading(false)
      }
    }

    loadUsers()
  }, [])

  useEffect(() => {
    const loadBuildings = async () => {
      if (!isAdmin) {
        setBuildings([])
        setBuildingLoadError('')
        return
      }

      try {
        setLoadingBuildings(true)
        setBuildingLoadError('')
        const buildingData = await apartmentService.getBuildings()
        setBuildings(normalizeCollection(buildingData))
      } catch (apiError) {
        setBuildings([])
        setBuildingLoadError(extractApiErrorMessage(apiError, 'Không thể tải danh sách tòa nhà.'))
      } finally {
        setLoadingBuildings(false)
      }
    }

    loadBuildings()
  }, [isAdmin])

  useEffect(() => {
    const loadApartmentsForManager = async () => {
      if (!isManager || !managerBuildingId) {
        setApartments([])
        setApartmentLoadError(
          isManager && !managerBuildingId ? 'Không xác định được tòa nhà của manager để tải danh sách căn hộ.' : '',
        )
        return
      }

      try {
        setLoadingApartments(true)
        setApartmentLoadError('')
        const apartmentData = await apartmentService.getApartmentsByBuildingId(managerBuildingId)
        setApartments(normalizeCollection(apartmentData))
      } catch (apiError) {
        setApartments([])
        setApartmentLoadError(extractApiErrorMessage(apiError, 'Không thể tải danh sách căn hộ.'))
      } finally {
        setLoadingApartments(false)
      }
    }

    loadApartmentsForManager()
  }, [isManager, managerBuildingId])

  useEffect(() => {
    setFormData((prev) => ({
      ...prev,
      roleName: isAdmin ? ROLE_KEYS.MANAGER : prev.roleName === ROLE_KEYS.MANAGER ? ROLE_KEYS.RESIDENT : prev.roleName,
    }))
  }, [isAdmin])

  useEffect(() => {
    if (isManager && formData.roleName !== ROLE_KEYS.RESIDENT && formData.apartmentId) {
      setFormData((prev) => ({ ...prev, apartmentId: '' }))
    }
  }, [formData.apartmentId, formData.roleName, isManager])

  const handleChange = (event) => {
    const { name, value } = event.target
    setFormData((prev) => ({ ...prev, [name]: value }))
    setError('')
    setMessage('')
  }

  const reloadUsers = async () => {
    const usersData = await userService.getUsers()
    setUsers(Array.isArray(usersData) ? usersData : [])
  }

  const handleSubmit = async (event) => {
    event.preventDefault()

    try {
      setSubmitting(true)
      setError('')
      setMessage('')

      if (isAdmin && !formData.buildingId) {
        setError('Vui lòng chọn tòa nhà cho manager.')
        return
      }

      if (isManager && formData.roleName === ROLE_KEYS.RESIDENT) {
        if (!managerBuildingId) {
          setError('Không xác định được tòa nhà của manager nên chưa thể cấp tài khoản resident.')
          return
        }

        if (!formData.apartmentId) {
          setError('Vui lòng chọn căn hộ cho resident.')
          return
        }
      }

      if (isAdmin) {
        await userService.createManager({
          email: formData.email,
          password: formData.password,
          fullName: formData.fullName,
          phone: formData.phone,
          idCard: formData.idCard,
          buildingId: formData.buildingId,
        })
        setMessage('Đã cấp tài khoản manager thành công.')
      } else if (isManager) {
        await userService.createUser({
          email: formData.email,
          password: formData.password,
          fullName: formData.fullName,
          phone: formData.phone,
          idCard: formData.idCard,
          roleName: formData.roleName,
          apartmentId: formData.roleName === ROLE_KEYS.RESIDENT ? formData.apartmentId : null,
          relationship: formData.roleName === ROLE_KEYS.RESIDENT ? formData.relationship : null,
          residenceType: formData.roleName === ROLE_KEYS.RESIDENT ? formData.residenceType : null,
        })
        setMessage(`Đã cấp tài khoản ${roleLabelMap[formData.roleName] || formData.roleName} thành công.`)
      }

      setFormData({
        ...initialCreateState,
        roleName: isAdmin ? ROLE_KEYS.MANAGER : ROLE_KEYS.RESIDENT,
      })
      await reloadUsers()
    } catch (apiError) {
      setError(extractApiErrorMessage(apiError, 'Không thể cấp tài khoản mới.'))
    } finally {
      setSubmitting(false)
    }
  }

  const buildingPlaceholder = loadingBuildings
    ? 'Đang tải tòa nhà...'
    : buildingLoadError
      ? 'Tòa nhà'
      : buildings.length === 0
        ? 'Không có tòa nhà'
        : 'Chọn tòa nhà'

  const apartmentPlaceholder = loadingApartments
    ? 'Đang tải căn hộ...'
    : apartmentLoadError
      ? 'Căn hộ'
      : apartments.length === 0
        ? 'Không có căn hộ trong tòa nhà'
        : 'Chọn căn hộ'

  const handleToggleLock = async (user) => {
    try {
      setError('')
      setMessage('')
      if (user.status === 'LOCKED') {
        await userService.unlockUser(user.userId)
        setMessage(`Đã mở khóa tài khoản ${user.email}.`)
      } else {
        await userService.lockUser(user.userId)
        setMessage(`Đã khóa tài khoản ${user.email}.`)
      }
      await reloadUsers()
    } catch (apiError) {
      setError(extractApiErrorMessage(apiError, 'Không thể cập nhật trạng thái khóa tài khoản.'))
    }
  }

  return (
    <div className="page-stack">
      <PageIntro
        eyebrow="Quản lý tài khoản"
        title={isAdmin ? 'Cấp tài khoản manager' : 'Cấp và quản lý tài khoản nội bộ'}
        description={
          isAdmin
            ? 'Admin cấp tài khoản quản lý theo từng tòa nhà và có quyền khóa/mở khóa tài khoản quản lý.'
            : 'Manager cấp tài khoản cư dân, nhân viên, kỹ thuật viên trong phạm vi tòa nhà của mình và quản lý trạng thái khóa tài khoản.'
        }
      />

      <section className="content-card">
        <div className="section-heading">
          <div>
            <span className="eyebrow">Cấp tài khoản</span>
            <h3>{isAdmin ? 'Tạo quản lý mới' : 'Tạo cư dân / nhân viên / kỹ thuật viên'}</h3>
          </div>
        </div>

        <form className="form-grid form-grid--two-columns" onSubmit={handleSubmit}>
          {!isAdmin ? (
            <label className="form-field">
              <span>Vai trò *</span>
              <select name="roleName" value={formData.roleName} onChange={handleChange}>
                {roleOptionsForManager.map((role) => (
                  <option key={role} value={role}>{roleLabelMap[role] || role}</option>
                ))}
              </select>
            </label>
          ) : null}

          {isAdmin ? (
            <label className="form-field form-field--full">
              <span>Tòa nhà *</span>
              <select name="buildingId" value={formData.buildingId} onChange={handleChange}>
                <option value="">{buildingPlaceholder}</option>
                {buildings.map((building) => (
                  <option key={building.buildingId} value={building.buildingId}>
                    {getBuildingLabel(building)}
                  </option>
                ))}
              </select>
              {buildingLoadError ? <small className="field-error">{buildingLoadError}</small> : null}
            </label>
          ) : null}

          <label className="form-field">
            <span>Họ tên *</span>
            <input name="fullName" value={formData.fullName} onChange={handleChange} />
          </label>

          <label className="form-field">
            <span>Email *</span>
            <input type="email" name="email" value={formData.email} onChange={handleChange} />
          </label>

          <label className="form-field">
            <span>Mật khẩu *</span>
            <input type="password" name="password" value={formData.password} onChange={handleChange} />
          </label>

          <label className="form-field">
            <span>Số điện thoại</span>
            <input name="phone" value={formData.phone} onChange={handleChange} />
          </label>

          <label className="form-field form-field--full">
            <span>CCCD/CMND</span>
            <input name="idCard" value={formData.idCard} onChange={handleChange} />
          </label>

          {isManager && formData.roleName === ROLE_KEYS.RESIDENT ? (
            <>
              <label className="form-field form-field--full">
                <span>Căn hộ *</span>
                <select
                  name="apartmentId"
                  value={formData.apartmentId}
                  onChange={handleChange}
                  disabled={loadingApartments || !managerBuildingId || apartments.length === 0}
                >
                  <option value="">{apartmentPlaceholder}</option>
                  {apartments.map((apartment) => (
                    <option key={apartment.apartmentId} value={apartment.apartmentId}>
                      {getApartmentLabel(apartment)}
                    </option>
                  ))}
                </select>
                {managerBuildingLabel ? <small>Tòa nhà quản lý: {managerBuildingLabel}</small> : null}
                {apartmentLoadError ? <small className="field-error">{apartmentLoadError}</small> : null}
              </label>

              <label className="form-field">
                <span>Quan hệ với căn hộ</span>
                <select name="relationship" value={formData.relationship} onChange={handleChange}>
                  <option value="OWNER">Chủ hộ</option>
                  <option value="TENANT">Người thuê</option>
                  <option value="FAMILY">Người thân</option>
                </select>
              </label>

              <label className="form-field">
                <span>Loại cư trú</span>
                <select name="residenceType" value={formData.residenceType} onChange={handleChange}>
                  <option value="PERMANENT">Thường trú</option>
                  <option value="TEMPORARY">Tạm trú</option>
                </select>
              </label>
            </>
          ) : null}

          {error ? <div className="alert alert-error form-field--full">{error}</div> : null}
          {message ? <div className="alert alert-success form-field--full">{message}</div> : null}

          <div className="form-actions form-field--full">
            <button type="submit" className="btn btn-primary" disabled={submitting}>
              {submitting ? 'Đang xử lý...' : isAdmin ? 'Cấp tài khoản manager' : 'Cấp tài khoản'}
            </button>
          </div>
        </form>
      </section>

      <section className="content-card">
        <div className="section-heading">
          <div>
            <span className="eyebrow">Danh bạ tài khoản</span>
            <h3>Danh sách tài khoản có thể quản lý</h3>
          </div>
        </div>

        {loading ? <div className="page-status">Đang tải danh sách tài khoản...</div> : null}

        {!loading && visibleUsers.length > 0 ? (
          <div className="table-card">
            <table className="data-table">
              <thead>
                <tr>
                  <th>Họ tên</th>
                  <th>Email</th>
                  <th>Vai trò</th>
                  <th>Trạng thái</th>
                  <th>Tòa nhà</th>
                  <th>Số điện thoại</th>
                  <th>Thời điểm khóa</th>
                  <th>Thao tác</th>
                </tr>
              </thead>
              <tbody>
                {visibleUsers.map((user) => (
                  <tr key={user.userId}>
                    <td>{user.fullName}</td>
                    <td>{user.email}</td>
                    <td>{roleLabelMap[user.roleName] || user.roleName}</td>
                    <td><span className={getStatusClass(user.status)}>{statusLabelMap[user.status] || user.status}</span></td>
                    <td>{getUserBuildingDisplay(user)}</td>
                    <td>{user.phone || '-'}</td>
                    <td>{user.lockedAt || '-'}</td>
                    <td>
                      {user.userId !== auth?.userId ? (
                        <button type="button" className={user.status === 'LOCKED' ? 'btn btn-success' : 'btn btn-danger'} onClick={() => handleToggleLock(user)}>
                          {user.status === 'LOCKED' ? 'Mở khóa' : 'Khóa'}
                        </button>
                      ) : (
                        <span className="table-note">Tài khoản hiện tại</span>
                      )}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        ) : null}

        {!loading && visibleUsers.length === 0 ? (
          <div className="empty-state">
            <h3>Chưa có dữ liệu tài khoản</h3>
            <p>Danh sách tài khoản sẽ xuất hiện ở đây sau khi hệ thống trả dữ liệu.</p>
          </div>
        ) : null}
      </section>
    </div>
  )
}
