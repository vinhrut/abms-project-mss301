import { FeaturePlaceholderPage } from '../../shared/FeaturePlaceholderPage.jsx'

export function ApartmentListPage() {
  return (
    <FeaturePlaceholderPage
      eyebrow="Apartment management"
      title="Apartment List"
      description="Theo dõi căn hộ theo tòa, tầng, diện tích, trạng thái sử dụng và cư dân đang ở."
      highlights={[
        { title: 'Apartment inventory', description: 'Danh sách căn hộ với filter theo building, floor, room number và status.' },
        { title: 'Detail handoff', description: 'Điểm vào để mở Apartment Detail, lịch sử bảo trì và thông tin cư dân.' },
        { title: 'Resident assignment', description: 'Sẵn sàng tích hợp flow assign/remove resident from apartment.' },
      ]}
    />
  )
}