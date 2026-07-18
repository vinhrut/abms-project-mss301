import { NotificationStatusBadge } from './NotificationStatusBadge.jsx'
import { formatDateTime } from '../utils/notificationUtils.js'

export function NotificationList({ items, loading, onSelect }) {
  if (loading) return <div className="empty-state"><p>Đang tải thông báo...</p></div>
  if (!items.length) return <div className="empty-state"><h3>Không có thông báo</h3><p>Thử thay đổi bộ lọc hoặc tạo thông báo mới.</p></div>
  return <div className="notification-table-wrap"><table className="data-table"><thead><tr><th>Thông báo</th><th>Ưu tiên / Nhóm</th><th>Kênh</th><th>Trạng thái</th><th>Hẹn gửi</th><th>Đã đọc</th></tr></thead><tbody>
    {items.map((item) => <tr key={item.id} className="notification-row" onClick={() => onSelect(item.id)} tabIndex="0" onKeyDown={(event) => event.key === 'Enter' && onSelect(item.id)}>
      <td><strong>{item.title}</strong><p className="notification-content-preview">{item.content}</p></td>
      <td><span className={`priority priority--${item.priority?.toLowerCase()}`}>{item.priority}</span><small>{item.recipientGroup}</small></td>
      <td>{item.channels?.join(', ')}</td><td><NotificationStatusBadge status={item.status} /></td><td>{formatDateTime(item.scheduledAt)}</td><td>{item.read ? 'Đã đọc' : 'Chưa đọc'}</td>
    </tr>)}
  </tbody></table></div>
}
