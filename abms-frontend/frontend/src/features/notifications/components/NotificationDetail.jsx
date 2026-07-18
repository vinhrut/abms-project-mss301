import { NotificationStatusBadge } from './NotificationStatusBadge.jsx'
import { formatDateTime } from '../utils/notificationUtils.js'

export function NotificationDetail({ item, loading, canManage, actionLoading, onClose, onApprove, onRead }) {
  return <div className="notification-modal-backdrop" role="presentation" onMouseDown={onClose}>
    <section className="notification-modal" role="dialog" aria-modal="true" aria-label="Chi tiết thông báo" onMouseDown={(event) => event.stopPropagation()}>
      <div className="section-heading"><div><span className="eyebrow">Chi tiết thông báo</span><h2>{loading ? 'Đang tải...' : item?.title}</h2></div><button className="btn btn-ghost" onClick={onClose}>Đóng</button></div>
      {loading ? <div className="empty-state">Đang tải chi tiết...</div> : item && <>
        <div className="notification-detail-grid">
          <div><small>Loại</small><strong>{item.type}</strong></div><div><small>Trạng thái</small><NotificationStatusBadge status={item.status} /></div>
          <div><small>Ưu tiên</small><strong>{item.priority}</strong></div><div><small>Nhóm nhận</small><strong>{item.recipientGroup}</strong></div>
          <div><small>Kênh</small><strong>{item.channels?.join(', ')}</strong></div><div><small>Đã đọc</small><strong>{item.read ? 'Có' : 'Không'}</strong></div>
          <div><small>Ngày tạo</small><strong>{formatDateTime(item.createdAt)}</strong></div><div><small>Hẹn gửi</small><strong>{formatDateTime(item.scheduledAt)}</strong></div>
          <div><small>Đã gửi</small><strong>{formatDateTime(item.sentAt)}</strong></div><div><small>ID</small><strong className="break-word">{item.id}</strong></div>
        </div>
        <div className="notification-detail-content"><small>Nội dung</small><p>{item.content}</p></div>
        {item.failureReason && <div className="alert alert-error">Lý do lỗi: {item.failureReason}</div>}
        <div className="form-actions">{!item.read && <button className="btn btn-primary" onClick={onRead} disabled={actionLoading}>Đánh dấu đã đọc</button>}{canManage && item.status === 'PENDING' && <button className="btn btn-success" onClick={onApprove} disabled={actionLoading}>Approve</button>}</div>
      </>}
    </section>
  </div>
}
