import { FeaturePlaceholderPage } from '../../shared/FeaturePlaceholderPage.jsx'

export function BuildingListPage() {
  return (
    <FeaturePlaceholderPage
      eyebrow="Building management"
      title="Building List"
      description="Quản lý danh sách tòa nhà, trạng thái vận hành, tỷ lệ lấp đầy và điều hướng sang chi tiết từng tòa."
      highlights={[
        { title: 'Building directory', description: 'Bảng danh sách tòa nhà với filter theo mã, tên, địa chỉ và occupancy.' },
        { title: 'Summary KPIs', description: 'Hiển thị total buildings, active apartments, resident count và operational status.' },
        { title: 'API-ready shell', description: 'Giữ chỗ cho GET building list, create/update/delete trong phase backend tiếp theo.' },
      ]}
    />
  )
}