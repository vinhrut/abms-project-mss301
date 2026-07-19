import { useEffect, useMemo, useState } from 'react'
import { Link } from 'react-router-dom'
import { useAuth } from '../../auth/context/useAuth.js'
import { apartmentService } from '../../../services/apartmentService.js'
import { paymentService } from '../../../services/paymentService.js'
import { APP_ROUTES } from '../../../config/navigation.js'
import { extractApiErrorMessage } from '../../../utils/apiError.js'

const methodMap = {
  CASH: 'badge badge-success',
  VIETQR: 'badge badge-warning',
  VNPAY: 'badge badge-info',
}

const formatMoney = (value) => {
  const amount = Number(value || 0)
  return new Intl.NumberFormat('vi-VN').format(amount)
}

const formatDateTime = (value) => {
  if (!value) {
    return '-'
  }
  return new Date(value).toLocaleString('vi-VN')
}

export function PaymentListPage() {
  const { auth, isElevatedRole } = useAuth()
  const [payments, setPayments] = useState([])
  const [searchTerm, setSearchTerm] = useState('')
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState('')
  const [residentApartmentId, setResidentApartmentId] = useState('')

  const loadPayments = async () => {
    try {
      setLoading(true)
      setError('')

      let response = []

      if (isElevatedRole) {
        response = await paymentService.getPayments()
      } else if (residentApartmentId) {
        response = await paymentService.getPayments({ apartmentId: residentApartmentId })
      } else if (auth?.userId) {
        const residence = await apartmentService.getActiveResidenceByUserId(auth.userId)
        const apartmentId = residence?.apartmentId || ''
        setResidentApartmentId(apartmentId)
        if (apartmentId) {
          response = await paymentService.getPayments({ apartmentId })
        }
      }

      setPayments(Array.isArray(response) ? response : [])
    } catch (apiError) {
      setError(
        extractApiErrorMessage(
          apiError,
          'Không thể tải lịch sử thanh toán. Vui lòng thử lại.',
        ),
      )
      setPayments([])
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    if (isElevatedRole || auth?.userId) {
      loadPayments()
    }
  }, [auth?.userId, isElevatedRole])

  const filteredPayments = useMemo(() => {
    const normalized = searchTerm.trim().toLowerCase()
    if (!normalized) {
      return payments
    }

    return payments.filter((payment) => {
      const invoiceCode = payment.invoiceCode?.toLowerCase() || ''
      const method = payment.paymentMethod?.toLowerCase() || ''
      const payerId = payment.payerId?.toLowerCase() || ''
      const collectorId = payment.collectorId?.toLowerCase() || ''
      return (
        invoiceCode.includes(normalized) ||
        method.includes(normalized) ||
        payerId.includes(normalized) ||
        collectorId.includes(normalized)
      )
    })
  }, [payments, searchTerm])

  return (
    <div className="page-stack">
      <section className="page-header-card">
        <div>
          <span className="eyebrow">Payment operations</span>
          <h1>{isElevatedRole ? 'Lịch sử thanh toán' : 'Thanh toán của tôi'}</h1>
          <p>
            {isElevatedRole
              ? 'Theo dõi các giao dịch CASH / VNPAY đã ghi nhận trên hệ thống.'
              : 'Xem các khoản thanh toán liên quan đến căn hộ đang cư trú.'}
          </p>
        </div>
      </section>

      <section className="content-card">
        <div className="info-grid">
          <article className="info-card">
            <strong>{isElevatedRole ? 'Staff / Manager mode' : 'Resident mode'}</strong>
            <p>
              {isElevatedRole
                ? 'Hiển thị toàn bộ payment (CASH / VNPAY) trong hệ thống.'
                : 'Lọc theo apartmentId từ căn hộ đang active của cư dân.'}
            </p>
          </article>
          <article className="info-card">
            <strong>Người dùng hiện tại</strong>
            <p>{auth?.email || 'Unknown user'}</p>
          </article>
        </div>

        <div className="toolbar-grid">
          <label className="form-field">
            <span>Search</span>
            <input
              placeholder="Tìm theo invoice code, method, payer, collector"
              value={searchTerm}
              onChange={(event) => setSearchTerm(event.target.value)}
            />
          </label>
          <div className="toolbar-actions">
            <button type="button" className="btn btn-primary" onClick={loadPayments}>
              Làm mới danh sách
            </button>
          </div>
        </div>

        {error ? <div className="alert alert-error">{error}</div> : null}

        {loading ? <div className="page-status">Đang tải lịch sử thanh toán...</div> : null}

        {!loading && filteredPayments.length === 0 ? (
          <div className="empty-state">
            <h3>Chưa có giao dịch thanh toán</h3>
            <p>
              {isElevatedRole
                ? 'Chưa có payment nào được ghi nhận hoặc không khớp bộ lọc tìm kiếm.'
                : 'Căn hộ của bạn chưa có giao dịch thanh toán.'}
            </p>
          </div>
        ) : null}

        {!loading && filteredPayments.length > 0 ? (
          <div className="table-card">
            <table className="data-table">
              <thead>
                <tr>
                  <th>Invoice</th>
                  <th>Method</th>
                  <th>Amount</th>
                  <th>Payer</th>
                  <th>Collector</th>
                  <th>Time</th>
                </tr>
              </thead>
              <tbody>
                {filteredPayments.map((payment) => {
                  const invoiceLabel = payment.invoiceCode || payment.invoiceId || '-'
                  const invoiceTo = payment.invoiceId
                    ? `${APP_ROUTES.invoices}?invoiceId=${encodeURIComponent(payment.invoiceId)}`
                    : payment.invoiceCode
                      ? `${APP_ROUTES.invoices}?invoiceCode=${encodeURIComponent(payment.invoiceCode)}`
                      : APP_ROUTES.invoices

                  return (
                    <tr key={payment.paymentId}>
                      <td>
                        {payment.invoiceId || payment.invoiceCode ? (
                          <Link className="table-link" to={invoiceTo}>
                            {invoiceLabel}
                          </Link>
                        ) : (
                          invoiceLabel
                        )}
                      </td>
                      <td>
                        <span className={methodMap[payment.paymentMethod] || 'badge'}>
                          {payment.paymentMethod}
                        </span>
                      </td>
                      <td>{formatMoney(payment.paidAmount)}</td>
                      <td>{payment.payerId || '-'}</td>
                      <td>{payment.collectorId || '-'}</td>
                      <td>{formatDateTime(payment.paymentTime)}</td>
                    </tr>
                  )
                })}
              </tbody>
            </table>
          </div>
        ) : null}
      </section>
    </div>
  )
}
