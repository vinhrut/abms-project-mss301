import { FeaturePlaceholderPage } from '../../shared/FeaturePlaceholderPage.jsx'

export function MyTasksPage() {
  return (
    <FeaturePlaceholderPage
      eyebrow="Technician workspace"
      title="My Maintenance Tasks"
      description="Không gian làm việc riêng cho kỹ thuật viên: xem việc được giao, ưu tiên, deadline và upload bằng chứng hoàn thành."
      highlights={[
        { title: 'Assigned task queue', description: 'Danh sách ticket theo trạng thái và độ ưu tiên dành cho technician.' },
        { title: 'Completion evidence', description: 'Sẵn khung cho upload ảnh, ghi chú xử lý và đánh dấu hoàn thành.' },
        { title: 'Resident feedback handoff', description: 'Điểm nối cho bước resident verify completion ở phase sau.' },
      ]}
    />
  )
}