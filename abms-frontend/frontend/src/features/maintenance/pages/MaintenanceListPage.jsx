import { FeaturePlaceholderPage } from '../../shared/FeaturePlaceholderPage.jsx'

export function MaintenanceListPage() {
  return (
    <FeaturePlaceholderPage
      eyebrow="Maintenance management"
      title="Maintenance Requests"
      description="Theo dõi yêu cầu bảo trì, mức độ ưu tiên, trạng thái xử lý, kỹ thuật viên được gán và lịch sử cập nhật."
      highlights={[
        { title: 'Role-aware workspace', description: 'Manager/Staff theo dõi toàn bộ yêu cầu, Resident gửi ticket, Technician xem task được giao.' },
        { title: 'Status timeline', description: 'Sẵn giao diện cho OPEN / IN_PROGRESS / RESOLVED / CANCELLED và history audit.' },
        { title: 'Assignment flow', description: 'Khung cho Assign Staff, Confirm Complete và completion evidence.' },
      ]}
    />
  )
}