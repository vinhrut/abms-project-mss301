import { NotificationStatusBadge } from './NotificationStatusBadge.jsx'
import { formatDateTime } from '../utils/notificationUtils.js'

export function NotificationList({ items, loading, onSelect }) {
  if (loading) {
    return (
      <div className="empty-state">
        <p>Đang tải thông báo...</p>
      </div>
    )
  }

  if (!items.length) {
    return (
      <div className="empty-state">
        <h3>Không có thông báo nào.</h3>
      </div>
    )
  }

  return (
    <div className="table-card notification-table-card">
      <table className="data-table notification-history-table">
        <thead>
          <tr>
            <th>Title</th>
            <th>Type</th>
            <th>Date Sent</th>
            <th>Recipient</th>
            <th>Status</th>
            <th>Read</th>
          </tr>
        </thead>
        <tbody>
          {items.map((item) => (
            <tr
              key={item.id}
              className={`notification-row ${item.read ? 'is-read' : 'is-unread'}`}
              onClick={() => onSelect(item.id)}
              tabIndex={0}
              onKeyDown={(event) => event.key === 'Enter' && onSelect(item.id)}
            >
              <td>
                <span className="notification-cell-ellipsis" title={item.title}>
                  {item.title}
                </span>
              </td>
              <td>{item.type}</td>
              <td>{formatDateTime(item.sentAt)}</td>
              <td>{item.recipientGroup || '-'}</td>
              <td>
                <NotificationStatusBadge status={item.status} />
              </td>
              <td>
                <span className={`notification-read-badge ${item.read ? 'is-read' : 'is-unread'}`}>
                  {item.read ? 'Đã đọc' : 'Chưa đọc'}
                </span>
              </td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  )
}
