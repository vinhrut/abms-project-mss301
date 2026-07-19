export function NotificationStatusBadge({ status }) {
  const className = { PENDING: 'badge-warning', SENT: 'badge-success', FAILED: 'badge-danger', CANCELLED: 'badge-muted' }[status] || ''
  return <span className={`badge ${className}`}>{status}</span>
}
