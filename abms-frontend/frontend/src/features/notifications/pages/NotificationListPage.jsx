import { FeaturePlaceholderPage } from '../../shared/FeaturePlaceholderPage.jsx'

export function NotificationListPage() {
  return (
    <FeaturePlaceholderPage
      eyebrow="Notifications"
      title="Notification Center"
      description="Xem lịch sử thông báo, trạng thái đã đọc/chưa đọc, nội dung chi tiết và lọc theo loại thông báo."
      highlights={[
        { title: 'Unified inbox', description: 'In-app notification center cho invoice, maintenance, contract và announcement.' },
        { title: 'Filter & history', description: 'Khung cho filter theo type, date range, status và recipient.' },
        { title: 'Announcement integration', description: 'Sẵn để nối create announcement và detail modal ở phase tiếp theo.' },
      ]}
    />
  )
}