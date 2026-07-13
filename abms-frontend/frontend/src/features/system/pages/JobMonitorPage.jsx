import { FeaturePlaceholderPage } from '../../shared/FeaturePlaceholderPage.jsx'

export function JobMonitorPage() {
  return (
    <FeaturePlaceholderPage
      eyebrow="System jobs"
      title="Job Monitor"
      description="Theo dõi các tác vụ nền như auto monthly billing, overdue alerts, contract expiry monitor và notification retry jobs."
      highlights={[
        { title: 'Scheduler visibility', description: 'Tổng quan trigger time, success count, failed jobs và retry queue.' },
        { title: 'Invoice notification monitor', description: 'Bám theo use case automatic monthly invoice notification.' },
        { title: 'Admin-only console', description: 'Không gian phù hợp cho audit-sensitive, ops-level monitoring.' },
      ]}
    />
  )
}