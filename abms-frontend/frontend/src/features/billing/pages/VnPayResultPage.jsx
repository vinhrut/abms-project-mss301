import { Link, useSearchParams } from 'react-router-dom'
import { APP_ROUTES } from '../../../config/navigation.js'

export function VnPayResultPage() {
  const [searchParams] = useSearchParams()
  const successParam = searchParams.get('success')
  const success = successParam === 'true' || successParam === '1'
  const invoiceCode = searchParams.get('invoiceCode') || ''
  const txnRef = searchParams.get('txnRef') || ''
  const message = searchParams.get('message') || (success ? 'Thanh toán thành công.' : 'Thanh toán thất bại.')

  return (
    <div className="page-stack">
      <section className="page-header-card">
        <div>
          <span className="eyebrow">VNPay sandbox</span>
          <h1>{success ? 'Thanh toán thành công' : 'Thanh toán không thành công'}</h1>
          <p>{message}</p>
        </div>
      </section>

      <section className="content-card">
        <div className={`alert ${success ? 'alert-success' : 'alert-error'}`}>
          {success
            ? 'Giao dịch VNPay đã được xác nhận. Hóa đơn sẽ cập nhật trạng thái PAID nếu đã trả đủ.'
            : 'VNPay không ghi nhận thanh toán thành công. Bạn có thể thử lại từ danh sách hóa đơn.'}
        </div>

        <div className="info-grid">
          {invoiceCode ? (
            <article className="info-card">
              <strong>Hóa đơn</strong>
              <p>{invoiceCode}</p>
            </article>
          ) : null}
          {txnRef ? (
            <article className="info-card">
              <strong>Mã giao dịch (TxnRef)</strong>
              <p>{txnRef}</p>
            </article>
          ) : null}
        </div>

        <div className="toolbar-actions" style={{ marginTop: '1.25rem' }}>
          <Link className="btn btn-primary" to={APP_ROUTES.invoices}>
            Về danh sách hóa đơn
          </Link>
          <Link className="btn btn-secondary" to={APP_ROUTES.payments}>
            Xem lịch sử thanh toán
          </Link>
        </div>
      </section>
    </div>
  )
}
