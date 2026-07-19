import { useEffect, useState } from 'react'
import { PageIntro } from '../../../components/ui/PageIntro.jsx'
import { apartmentService } from '../../../services/apartmentService.js'
import { useAuth } from '../../auth/context/useAuth.js'
import { format as formatDateFns } from 'date-fns'

function fmt(dateStr) {
  if (!dateStr) return '-'
  try {
    return new Date(dateStr).toLocaleDateString('vi-VN')
  } catch {
    return dateStr
  }
}

export function ContractListPage() {
  const { auth } = useAuth()
  const buildingId = auth?.buildingId
  const [contracts, setContracts] = useState([])
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState('')
  const [selectedContract, setSelectedContract] = useState(null)
  const [showRenewForm, setShowRenewForm] = useState(false)
  const [renewPayload, setRenewPayload] = useState({ startDate: '', endDate: '', deposit: '' })

  useEffect(() => {
    loadContracts()
  }, [])

  async function loadContracts() {
    try {
      setLoading(true)
      const data = await apartmentService.getContracts(buildingId)
      setContracts(Array.isArray(data) ? data : [])
    } catch (err) {
      setError(err?.message || 'Không thể tải danh sách hợp đồng')
    } finally {
      setLoading(false)
    }
  }

  function openDetails(contract) {
    setSelectedContract(contract)
  }

  function closeDetails() {
    setSelectedContract(null)
    setShowRenewForm(false)
  }

  function openRenewForm(contract) {
    // default start date is day after current end date
    const nextDay = contract.endDate ? new Date(contract.endDate) : new Date()
    nextDay.setDate(nextDay.getDate() + 1)
    setRenewPayload({ startDate: nextDay.toISOString().slice(0, 10), endDate: '', deposit: contract.deposit || '' })
    setShowRenewForm(true)
  }

  async function submitRenew() {
    try {
      setLoading(true)
      const payload = {
        startDate: renewPayload.startDate ? renewPayload.startDate : null,
        endDate: renewPayload.endDate,
        deposit: renewPayload.deposit,
      }
      await apartmentService.renewContract(selectedContract.contractId, payload)
      await loadContracts()
      setShowRenewForm(false)
      setSelectedContract(null)
    } catch (err) {
      setError(err?.response?.data?.message || err.message || 'Không thể gia hạn hợp đồng')
    } finally {
      setLoading(false)
    }
  }

  return (
    <div>
      <PageIntro eyebrow="Contract management" title="Contract List" description="Theo dõi hợp đồng thuê/sở hữu, ngày hiệu lực, ngày hết hạn và cảnh báo sắp hết hạn." />

      <div className="card">
        <div style={{ marginBottom: 12 }}>
          <strong>Contract List</strong>
        </div>

        {loading ? (
          <div>Đang tải...</div>
        ) : (
          <div style={{ overflowX: 'auto' }}>
            <table className="data-table" style={{ minWidth: 900 }}>
              <thead>
                <tr>
                  <th>Mã hợp đồng</th>
                  <th>Tên cư dân</th>
                  <th>Căn hộ</th>
                  <th>Ngày bắt đầu</th>
                  <th>Ngày kết thúc</th>
                  <th>Số tiền cọc</th>
                  <th>Trạng thái</th>
                  <th></th>
                </tr>
              </thead>
              <tbody>
                {contracts.map((c) => (
                  <tr key={c.contractId}>
                    <td style={{ fontSize: '0.9em' }}>{c.contractId}</td>
                    <td>{c.userFullName || '-'}</td>
                    <td>{c.apartmentRoomNumber || '-'}</td>
                    <td>{fmt(c.startDate)}</td>
                    <td>{fmt(c.endDate)}</td>
                    <td>{c.deposit != null ? c.deposit : '-'}</td>
                    <td>{c.status || '-'}</td>
                    <td>
                      <button className="btn btn-ghost btn-sm" onClick={() => openDetails(c)}>Xem chi tiết</button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}

        {/* detail modal */}
        {selectedContract ? (
          <>
            <div className="modal-backdrop" style={{ position: 'fixed', inset: 0, background: 'rgba(0,0,0,0.35)', zIndex: 2000 }} onClick={closeDetails} />
            <div className="modal-content" style={{ position: 'fixed', top: '50%', left: '50%', transform: 'translate(-50%,-50%)', width: '90%', maxWidth: 800, zIndex: 2001, background: '#fff', padding: '1.5rem', borderRadius: 10 }} onClick={(e) => e.stopPropagation()}>
              <h3>Chi tiết hợp đồng</h3>
              <div style={{ marginBottom: 12 }}>
                <strong>{selectedContract.contractId}</strong>
              </div>

              <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 12 }}>
                <div>
                  <div><strong>Căn hộ:</strong> {selectedContract.apartmentRoomNumber}</div>
                  <div><strong>Người thuê:</strong> {selectedContract.userFullName}</div>
                  <div><strong>Số điện thoại:</strong> {selectedContract.userPhone}</div>
                </div>
                <div>
                  <div><strong>Ngày bắt đầu:</strong> {fmt(selectedContract.startDate)}</div>
                  <div><strong>Ngày kết thúc:</strong> {fmt(selectedContract.endDate)}</div>
                  <div><strong>Tiền đặt cọc:</strong> {selectedContract.deposit}</div>
                </div>
              </div>

              <div style={{ display: 'flex', justifyContent: 'flex-end', gap: 8, marginTop: 16 }}>
                <button className="btn btn-ghost" onClick={closeDetails}>Đóng</button>
                <button className="btn btn-primary" onClick={() => openRenewForm(selectedContract)}>Gia hạn hợp đồng</button>
              </div>

              {/* renew form modal */}
              {showRenewForm ? (
                <div style={{ marginTop: 16 }}>
                  <h4>Form gia hạn</h4>
                  <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 8 }}>
                    <label>
                      Ngày bắt đầu mới
                      <input type="date" value={renewPayload.startDate} readOnly className="input" />
                    </label>
                    <label>
                      Ngày kết thúc mới
                      <input type="date" value={renewPayload.endDate} onChange={(e) => setRenewPayload({ ...renewPayload, endDate: e.target.value })} className="input" />
                    </label>
                    <label style={{ gridColumn: '1 / -1' }}>
                      Số tiền cọc
                      <input type="number" value={renewPayload.deposit} onChange={(e) => setRenewPayload({ ...renewPayload, deposit: e.target.value })} className="input" />
                    </label>
                  </div>

                  <div style={{ display: 'flex', justifyContent: 'flex-end', gap: 8, marginTop: 12 }}>
                    <button className="btn btn-ghost" onClick={() => setShowRenewForm(false)}>Hủy</button>
                    <button className="btn btn-primary" onClick={submitRenew}>Xác nhận</button>
                  </div>
                </div>
              ) : null}
            </div>
          </>
        ) : null}

      </div>

      {error ? (<div className="alert alert-danger" style={{ marginTop: 12 }}>{error}</div>) : null}
    </div>
  )
}
