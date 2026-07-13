import { FeaturePlaceholderPage } from '../../shared/FeaturePlaceholderPage.jsx'

export function ResidentListPage() {
  return (
    <FeaturePlaceholderPage
      eyebrow="Resident management"
      title="Resident List"
      description="Quản lý cư dân, hồ sơ, trạng thái tài khoản, căn hộ liên kết và lịch sử cư trú."
      highlights={[
        { title: 'Search & filter', description: 'Filter theo tên, email, căn hộ, trạng thái tài khoản và contract status.' },
        { title: 'Resident detail flow', description: 'Liên kết sang Resident Detail, vehicle registrations và activity history.' },
        { title: 'Approval coexistence', description: 'Tương thích với màn hình Resident Approval đã có API thật.' },
      ]}
    />
  )
}