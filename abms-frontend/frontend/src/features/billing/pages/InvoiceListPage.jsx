import { FeaturePlaceholderPage } from '../../shared/FeaturePlaceholderPage.jsx'

export function InvoiceListPage() {
  return (
    <FeaturePlaceholderPage
      eyebrow="Financial management"
      title="Invoice List"
      description="Quản lý danh sách hóa đơn theo căn hộ, kỳ thanh toán và trạng thái Paid / Unpaid / Overdue."
      highlights={[
        { title: 'Invoice filters', description: 'Filter theo apartment, month/year, payment status và overdue state.' },
        { title: 'Record payment entry', description: 'Điểm vào cho payment modal, VietQR flow và invoice detail.' },
        { title: 'Meter-reading ready', description: 'Sẵn chỗ cho luồng nhập chỉ số điện nước và tạo hóa đơn hàng loạt.' },
      ]}
    />
  )
}