import { FeaturePlaceholderPage } from '../../shared/FeaturePlaceholderPage.jsx'

export function PaymentListPage() {
  return (
    <FeaturePlaceholderPage
      eyebrow="Payment operations"
      title="Payment List"
      description="Theo dõi lịch sử thanh toán theo căn hộ, phương thức, thời gian và người thu tiền/xác nhận giao dịch."
      highlights={[
        { title: 'Transaction history', description: 'Hiển thị payment method, amount, invoice reference, payer và time.' },
        { title: 'Cash & VietQR support', description: 'Khung giao diện cho offline collection và QR-assisted confirmation.' },
        { title: 'Debt clearing workflow', description: 'Sẵn để kết nối invoice status update sang PAID / PARTIAL.' },
      ]}
    />
  )
}