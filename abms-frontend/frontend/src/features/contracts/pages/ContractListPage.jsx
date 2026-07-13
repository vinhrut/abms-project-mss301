import { FeaturePlaceholderPage } from '../../shared/FeaturePlaceholderPage.jsx'

export function ContractListPage() {
  return (
    <FeaturePlaceholderPage
      eyebrow="Contract management"
      title="Contract List"
      description="Theo dõi hợp đồng thuê/sở hữu, ngày hiệu lực, ngày hết hạn và cảnh báo sắp hết hạn."
      highlights={[
        { title: 'Contract overview', description: 'List hợp đồng theo trạng thái Active / Expiring / Expired / Terminated.' },
        { title: 'Document integration', description: 'Sẵn khung cho upload PDF, hợp đồng đính kèm và thao tác gia hạn.' },
        { title: 'Finance coordination', description: 'Làm đầu vào cho dashboard alerts và contract expiry monitoring.' },
      ]}
    />
  )
}