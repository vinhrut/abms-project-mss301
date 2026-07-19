import { useEffect, useState } from 'react'
import { PageIntro } from '../../../components/ui/PageIntro.jsx'
import { ROLE_KEYS } from '../../../config/roles.js'
import { apartmentService } from '../../../services/apartmentService.js'
import { extractApiErrorMessage } from '../../../utils/apiError.js'
import { useAuth } from '../../auth/context/useAuth.js'
import { FeaturePlaceholderPage } from '../../shared/FeaturePlaceholderPage.jsx'

function getBuildingIdFromToken(token) {
  try {
    const [, payload] = token.split('.')
    return JSON.parse(window.atob(payload.replace(/-/g, '+').replace(/_/g, '/'))).buildingId || ''
  } catch {
    return ''
  }
}

function getStatusClass(status) {
  if (status === 'OCCUPIED' || status === 'ACTIVE') return 'badge badge-success'
  if (status === 'VACANT' || status === 'AVAILABLE') return 'badge badge-warning'
  return 'badge'
}

export function ApartmentListPage() {
  const { auth, normalizedRole } = useAuth()
  const [apartments, setApartments] = useState([])
  const [selectedApartment, setSelectedApartment] = useState(null)
  const [selectedApartmentResidents, setSelectedApartmentResidents] = useState([])
  const [loading, setLoading] = useState(false)
  const [loadingResidents, setLoadingResidents] = useState(false)
  const [error, setError] = useState('')
  const managerBuildingId = auth?.buildingId || getBuildingIdFromToken(auth?.token || '')
  const isManager = normalizedRole === ROLE_KEYS.MANAGER

  useEffect(() => {
    const loadManagerData = async () => {
      if (!isManager || !managerBuildingId) {
        setApartments([])
        return
      }

      try {
        setLoading(true)
        setError('')
        const apartmentData = await apartmentService.getApartmentsByBuildingId(managerBuildingId)

        setApartments(Array.isArray(apartmentData) ? apartmentData : [])
      } catch (apiError) {
        setApartments([])
        setError(extractApiErrorMessage(apiError, 'Không thể tải danh sách căn hộ của tòa nhà đang quản lý.'))
      } finally {
        setLoading(false)
      }
    }

    loadManagerData()
  }, [isManager, managerBuildingId])

  const handleViewResidents = async (apartment) => {
    setSelectedApartment(apartment)
    setSelectedApartmentResidents([])
    setLoadingResidents(true)
    setError('')

    try {
      const apartmentResidents = await apartmentService.getApartmentResidentsByApartmentId(apartment.apartmentId)
      setSelectedApartmentResidents(Array.isArray(apartmentResidents) ? apartmentResidents : [])
    } catch (apiError) {
      setSelectedApartmentResidents([])
      setError(extractApiErrorMessage(apiError, 'Không thể tải danh sách cư dân của căn hộ này.'))
    } finally {
      setLoadingResidents(false)
    }
  }

  return (
    <div className="page-stack">
      <PageIntro
        eyebrow="Apartment management"
        title="Danh sách căn hộ"
        description="Manager chỉ xem các căn hộ thuộc tòa nhà được phân công quản lý."
      />

      {isManager ? (
        <section className="content-card">
          <div className="section-heading">
            <div>
              <span className="eyebrow">My building</span>
              <h3>Căn hộ thuộc tòa nhà đang quản lý</h3>
            </div>
            <span className="table-note">Building ID: {managerBuildingId || '-'}</span>
          </div>

          {error ? <div className="alert alert-error">{error}</div> : null}
          {loading ? <div className="page-status">Đang tải danh sách căn hộ...</div> : null}

          {!loading && managerBuildingId && apartments.length === 0 ? (
            <div className="empty-state">
              <h3>Chưa có căn hộ</h3>
              <p>Tòa nhà bạn quản lý hiện chưa có căn hộ nào.</p>
            </div>
          ) : null}

          {!loading && apartments.length > 0 ? (
            <>
              <div className="table-card">
                <table className="data-table">
                  <thead>
                    <tr>
                      <th>Căn hộ</th>
                      <th>Tầng</th>
                      <th>Diện tích</th>
                      <th>Trạng thái</th>
                      <th></th>
                    </tr>
                  </thead>
                  <tbody>
                    {apartments.map((apartment) => (
                      <tr key={apartment.apartmentId}>
                        <td>{apartment.roomNumber || '-'}</td>
                        <td>{apartment.floor ?? '-'}</td>
                        <td>{apartment.area ? `${apartment.area} m²` : '-'}</td>
                        <td><span className={getStatusClass(apartment.status)}>{apartment.status || '-'}</span></td>
                        <td>
                          <button
                            type="button"
                            className="btn btn-secondary btn-sm"
                            onClick={() => handleViewResidents(apartment)}
                          >
                            Xem cư dân
                          </button>
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>

              {/* Modal: show residents in a popup to avoid layout shifts */}
              {selectedApartment ? (
                <>
                  <div
                    className="modal-backdrop"
                    style={{ position: 'fixed', inset: 0, background: 'rgba(0,0,0,0.35)', zIndex: 1000 }}
                    onClick={() => {
                      setSelectedApartment(null)
                      setSelectedApartmentResidents([])
                    }}
                  />

                  <div
                    className="modal-content"
                    role="dialog"
                    aria-modal="true"
                    style={{
                      position: 'fixed',
                      top: '50%',
                      left: '50%',
                      transform: 'translate(-50%, -50%)',
                      width: '85%',
                      maxWidth: '1100px',
                      maxHeight: '80vh',
                      overflowY: 'auto',
                      zIndex: 1001,
                      background: '#fff',
                      borderRadius: 12,
                      padding: '1rem 1.25rem',
                      boxShadow: '0 10px 30px rgba(0,0,0,0.2)',
                    }}
                    onClick={(e) => e.stopPropagation()}
                  >
                    <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '0.5rem' }}>
                      <div>
                        <span className="eyebrow">Apartment residents</span>
                        <h3 style={{ margin: 0 }}>Danh sách cư dân căn hộ {selectedApartment.roomNumber}</h3>
                      </div>
                      <div>
                        <button type="button" className="btn btn-ghost" onClick={() => { setSelectedApartment(null); setSelectedApartmentResidents([]) }}>
                          Đóng
                        </button>
                      </div>
                    </div>

                    {loadingResidents ? (
                      <div className="page-status">Đang tải danh sách cư dân...</div>
                    ) : selectedApartmentResidents.length === 0 ? (
                      <div className="empty-state">
                        <h3>Chưa có cư dân</h3>
                        <p>Căn hộ này hiện chưa có cư dân hoặc dữ liệu chưa được cập nhật.</p>
                      </div>
                    ) : (
                      <div style={{ overflowX: 'auto' }}>
                        <table className="data-table" style={{ minWidth: 700 }}>
                          <thead>
                            <tr>
                              <th>Họ tên</th>
                              <th>Email</th>
                              <th>Điện thoại</th>
                              <th>Vai trò</th>
                              <th>Loại cư trú</th>
                              <th>Trạng thái</th>
                            </tr>
                          </thead>
                          <tbody>
                            {selectedApartmentResidents.map((resident) => (
                              <tr key={resident.residentId}>
                                <td>{resident.userFullName || resident.userId || '-'}</td>
                                <td>{resident.userEmail || '-'}</td>
                                <td>{resident.userPhone || '-'}</td>
                                <td>{resident.relationship || '-'}</td>
                                <td>{resident.residenceType || '-'}</td>
                                <td><span className={getStatusClass(resident.status)}>{resident.status || '-'}</span></td>
                              </tr>
                            ))}
                          </tbody>
                        </table>
                      </div>
                    )}
                  </div>
                </>
              ) : null}
            </>
          ) : null}
        </section>
      ) : (
        <FeaturePlaceholderPage
          eyebrow="Apartment management"
          title="Apartment List"
          description="Theo dõi căn hộ theo tòa, tầng, diện tích, trạng thái sử dụng và cư dân đang ở."
          highlights={[
            { title: 'Apartment inventory', description: 'Danh sách căn hộ với filter theo building, floor, room number và status.' },
            { title: 'Detail handoff', description: 'Điểm vào để mở Apartment Detail, lịch sử bảo trì và thông tin cư dân.' },
            { title: 'Resident assignment', description: 'Sẵn sàng tích hợp flow assign/remove resident from apartment.' },
          ]}
        />
      )}
    </div>
  )
}
