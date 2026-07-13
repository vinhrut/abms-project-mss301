import { FeaturePlaceholderPage } from '../../shared/FeaturePlaceholderPage.jsx'

export function ReportListPage() {
  return (
    <FeaturePlaceholderPage
      eyebrow="Reports & analytics"
      title="Report List"
      description="Tạo và tải xuống báo cáo tài chính, occupancy, maintenance và resident operations cho từng kỳ."
      highlights={[
        { title: 'Financial report shell', description: 'Khung cho chọn kỳ, format PDF/Excel và generate report.' },
        { title: 'Dashboard export continuity', description: 'Tương thích với use case export dashboard summary report.' },
        { title: 'Audit-sensitive access', description: 'Dễ gắn audit logging và phân quyền Admin/Manager.' },
      ]}
    />
  )
}