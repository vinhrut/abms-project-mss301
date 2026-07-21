/* eslint-disable react-hooks/set-state-in-effect */
import { useCallback, useEffect, useMemo, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { PageIntro } from '../../../components/ui/PageIntro.jsx'
import { APP_ROUTES } from '../../../config/navigation.js'
import { isRoleIn, ROLE_KEYS } from '../../../config/roles.js'
import { apartmentService } from '../../../services/apartmentService.js'
import { userService } from '../../../services/userService.js'
import { extractApiErrorMessage } from '../../../utils/apiError.js'
import { useAuth } from '../../auth/context/useAuth.js'

const emptyForm = { name: '', code: '', address: '', floors: 1 }

function getBuildingIdFromToken(token) {
  try {
    const [, payload] = token.split('.')
    return JSON.parse(window.atob(payload.replace(/-/g, '+').replace(/_/g, '/'))).buildingId || ''
  } catch {
    return ''
  }
}

function normalizeRoleName(user) {
  return String(user?.roleName || user?.role?.roleName || user?.role || '')
    .replace(/^ROLE_/, '')
    .toUpperCase()
}

function buildOccupancyLabel(apartments = []) {
  const total = apartments.length
  if (!total) return 'No apartments'
  const occupied = apartments.filter((item) => String(item.status || '').toUpperCase() === 'OCCUPIED').length
  const rate = Math.round((occupied / total) * 100)
  return `${occupied}/${total} occupied (${rate}%)`
}

function deriveFloors(apartments = [], fallback = 0) {
  if (!apartments.length) return Number(fallback) || 0
  return Math.max(Number(fallback) || 0, apartments.reduce((max, item) => Math.max(max, Number(item.floor) || 0), 0))
}

export function BuildingListPage() {
  const { auth } = useAuth()
  const navigate = useNavigate()
  const isManager = isRoleIn(auth?.roleName, [ROLE_KEYS.MANAGER])
  const isAdmin = isRoleIn(auth?.roleName, [ROLE_KEYS.ADMIN])
  const managerBuildingId = auth?.buildingId || getBuildingIdFromToken(auth?.token || '')

  const [rows, setRows] = useState([])
  const [search, setSearch] = useState('')
  const [loading, setLoading] = useState(false)
  const [saving, setSaving] = useState(false)
  const [error, setError] = useState('')
  const [message, setMessage] = useState('')
  const [modal, setModal] = useState(null)
  const [form, setForm] = useState(emptyForm)
  const [formErrors, setFormErrors] = useState({})

  const load = useCallback(async () => {
    setLoading(true)
    setError('')
    try {
      const [buildings, users] = await Promise.all([
        apartmentService.getBuildings(),
        userService.getUsers().catch(() => []),
      ])

      let buildingList = Array.isArray(buildings) ? buildings : []
      if (isManager && managerBuildingId) {
        buildingList = buildingList.filter((item) => item.buildingId === managerBuildingId)
      }

      const managers = (Array.isArray(users) ? users : []).filter(
        (user) => normalizeRoleName(user) === ROLE_KEYS.MANAGER,
      )

      const enriched = await Promise.all(
        buildingList.map(async (building) => {
          const [apartments, residents] = await Promise.all([
            apartmentService.getApartmentsByBuildingId(building.buildingId).catch(() => []),
            apartmentService.getBuildingResidentsByBuildingId(building.buildingId).catch(() => []),
          ])
          const apartmentList = Array.isArray(apartments) ? apartments : []
          const residentList = Array.isArray(residents) ? residents : []
          const activeApartments = apartmentList.filter((item) =>
            ['OCCUPIED', 'ACTIVE'].includes(String(item.status || '').toUpperCase()),
          ).length
          const manager = managers.find((user) => user.buildingId === building.buildingId)

          return {
            buildingId: building.buildingId,
            name: building.name || '-',
            code: building.code || '',
            address: building.address || '-',
            floors: deriveFloors(apartmentList, building.floors),
            apartmentCount: apartmentList.length,
            activeApartments,
            occupancyStatus: buildOccupancyLabel(apartmentList),
            residentCount: residentList.filter((item) => String(item.status || '').toUpperCase() === 'ACTIVE').length
              || residentList.length,
            managerName: manager?.fullName || manager?.email || 'Unassigned',
          }
        }),
      )

      setRows(enriched)
    } catch (apiError) {
      setRows([])
      setError(extractApiErrorMessage(apiError, 'Không thể tải danh sách tòa nhà.'))
    } finally {
      setLoading(false)
    }
  }, [isManager, managerBuildingId])

  useEffect(() => {
    load()
  }, [load])

  const filtered = useMemo(() => {
    const keyword = search.trim().toLowerCase()
    if (!keyword) return rows
    return rows.filter((row) =>
      [row.name, row.code, row.address]
        .join(' ')
        .toLowerCase()
        .includes(keyword),
    )
  }, [rows, search])

  const openEdit = (row) => {
    setForm({
      name: row.name === '-' ? '' : row.name,
      code: row.code || '',
      address: row.address === '-' ? '' : row.address,
      floors: row.floors || 0,
    })
    setFormErrors({})
    setModal({ buildingId: row.buildingId })
    setMessage('')
  }

  const closeModal = () => {
    if (saving) return
    setModal(null)
    setFormErrors({})
  }

  const updateForm = (name, value) => {
    setForm((old) => ({ ...old, [name]: value }))
  }

  const validateForm = () => {
    const next = {}
    if (!form.name.trim()) next.name = 'Nhập tên tòa nhà'
    if (!form.code.trim()) next.code = 'Nhập mã tòa nhà'
    if (!form.address.trim()) next.address = 'Nhập địa chỉ'
    if (form.floors === '' || Number(form.floors) < 0 || Number.isNaN(Number(form.floors))) {
      next.floors = 'Số tầng không hợp lệ'
    }
    setFormErrors(next)
    return Object.keys(next).length === 0
  }

  const submitForm = async (event) => {
    event.preventDefault()
    if (!modal?.buildingId || !validateForm()) return

    const payload = {
      name: form.name.trim(),
      code: form.code.trim(),
      address: form.address.trim(),
      floors: Number(form.floors) || 0,
    }

    setSaving(true)
    setError('')
    try {
      await apartmentService.updateBuilding(modal.buildingId, payload)
      setMessage('Đã cập nhật tòa nhà.')
      setModal(null)
      await load()
    } catch (apiError) {
      setError(extractApiErrorMessage(apiError, 'Không thể lưu tòa nhà.'))
    } finally {
      setSaving(false)
    }
  }

  const removeBuilding = async (row) => {
    if (!isAdmin) {
      setError('Chỉ Admin được xóa tòa nhà.')
      return
    }
    if (!window.confirm(`Xóa tòa nhà "${row.name}"? Chỉ xóa được khi chưa có căn hộ.`)) return

    setSaving(true)
    setError('')
    setMessage('')
    try {
      await apartmentService.deleteBuilding(row.buildingId)
      setMessage('Đã xóa tòa nhà.')
      await load()
    } catch (apiError) {
      setError(extractApiErrorMessage(apiError, 'Không thể xóa tòa nhà.'))
    } finally {
      setSaving(false)
    }
  }

  const viewRooms = (buildingId) => {
    navigate(`${APP_ROUTES.apartments}?buildingId=${buildingId}`)
  }

  const canEditRow = (row) => isAdmin || (isManager && row.buildingId === managerBuildingId)

  return (
    <div className="page-stack building-list-page">
      <PageIntro
        eyebrow="Building Management"
        title="QUẢN LÝ TÒA NHÀ"
        description="Danh sách tòa nhà trong hệ thống — xem, sửa, xóa và mở danh sách phòng."
      />

      <section className="content-card">
        <div className="building-toolbar">
          <label className="building-search">
            <span className="visually-hidden">Search</span>
            <input
              type="search"
              placeholder="Search..."
              value={search}
              onChange={(e) => setSearch(e.target.value)}
              aria-label="Search buildings"
            />
          </label>
          {(isAdmin || isManager) && (
            <button
              className="btn btn-primary"
              type="button"
              title="Chức năng thêm tòa nhà sẽ do module khác phụ trách"
              onClick={() => setMessage('Chức năng thêm tòa nhà mới sẽ được tích hợp sau.')}
            >
              + ADD TÒA NHÀ MỚI
            </button>
          )}
        </div>

        {error && <div className="alert alert-error">{error}</div>}
        {message && <div className="alert alert-success">{message}</div>}
        {loading && <div className="page-status">Đang tải danh sách tòa nhà...</div>}

        {!loading && filtered.length === 0 && (
          <div className="empty-state">
            <h3>No buildings available.</h3>
            <p>{search.trim() ? 'Không có tòa nhà khớp từ khóa tìm kiếm.' : 'Chưa có dữ liệu tòa nhà trong hệ thống.'}</p>
          </div>
        )}

        {!loading && filtered.length > 0 && (
          <div className="table-card notification-table-card">
            <table className="data-table building-table">
              <thead>
                <tr>
                  <th>STT</th>
                  <th>Tên Tòa Nhà</th>
                  <th>Địa Chỉ</th>
                  <th>Số Tầng</th>
                  <th>Hành động</th>
                </tr>
              </thead>
              <tbody>
                {filtered.map((row, index) => (
                  <tr key={row.buildingId}>
                    <td className="building-col-stt">{index + 1}</td>
                    <td className="building-col-name">{row.name}</td>
                    <td className="building-col-address" title={row.address}>{row.address}</td>
                    <td className="building-col-floors">{row.floors || '—'}</td>
                    <td>
                      <div className="building-actions">
                        {canEditRow(row) && (
                          <button type="button" className="link-btn" disabled={saving} onClick={() => openEdit(row)}>
                            [Sửa]
                          </button>
                        )}
                        {isAdmin && (
                          <button
                            type="button"
                            className="link-btn link-btn--danger"
                            disabled={saving}
                            onClick={() => removeBuilding(row)}
                          >
                            [Xóa]
                          </button>
                        )}
                        <button type="button" className="link-btn" onClick={() => viewRooms(row.buildingId)}>
                          [Xem phòng]
                        </button>
                      </div>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </section>

      {modal && (
        <div className="modal-backdrop" role="presentation" onClick={closeModal}>
          <section className="modal-card" role="dialog" onClick={(e) => e.stopPropagation()}>
            <h2>Sửa tòa nhà</h2>
            <form className="building-form" onSubmit={submitForm}>
              <label className="form-field">
                <span>Tên tòa nhà</span>
                <input value={form.name} onChange={(e) => updateForm('name', e.target.value)} maxLength={150} />
                {formErrors.name && <small className="field-error">{formErrors.name}</small>}
              </label>
              <label className="form-field">
                <span>Mã tòa nhà</span>
                <input value={form.code} onChange={(e) => updateForm('code', e.target.value)} maxLength={50} />
                {formErrors.code && <small className="field-error">{formErrors.code}</small>}
              </label>
              <label className="form-field form-field--full">
                <span>Địa chỉ</span>
                <input value={form.address} onChange={(e) => updateForm('address', e.target.value)} maxLength={255} />
                {formErrors.address && <small className="field-error">{formErrors.address}</small>}
              </label>
              <label className="form-field">
                <span>Số tầng</span>
                <input
                  type="number"
                  min="0"
                  value={form.floors}
                  onChange={(e) => updateForm('floors', e.target.value)}
                />
                {formErrors.floors && <small className="field-error">{formErrors.floors}</small>}
              </label>
              <div className="form-actions form-field--full">
                <button className="btn btn-ghost" type="button" onClick={closeModal} disabled={saving}>
                  Hủy
                </button>
                <button className="btn btn-primary" type="submit" disabled={saving}>
                  {saving ? 'Đang lưu...' : 'Lưu'}
                </button>
              </div>
            </form>
          </section>
        </div>
      )}
    </div>
  )
}
