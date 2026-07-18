export const NOTIFICATION_TYPES = ['ANNOUNCEMENT', 'INVOICE', 'CONTRACT', 'MAINTENANCE', 'SYSTEM']
export const NOTIFICATION_PRIORITIES = ['LOW', 'NORMAL', 'HIGH', 'URGENT']
export const NOTIFICATION_STATUSES = ['PENDING', 'SENT', 'FAILED', 'CANCELLED']
export const DELIVERY_CHANNELS = ['IN_APP', 'EMAIL']

export function canManageNotifications(role) {
  const normalized = String(role || '').replace(/^ROLE_/, '').toUpperCase()
  return ['ADMIN', 'MANAGER', 'BUILDING_MANAGER'].includes(normalized)
}

export function formatDateTime(value) {
  if (!value) return '—'
  const parsed = new Date(value)
  return Number.isNaN(parsed.getTime()) ? value : new Intl.DateTimeFormat('vi-VN', { dateStyle: 'short', timeStyle: 'medium' }).format(parsed)
}

export function apiErrorMessage(error, fallback = 'Không thể xử lý yêu cầu.') {
  const status = error?.response?.status
  const backendMessage = error?.response?.data?.message || error?.response?.data?.detail
  if (backendMessage) return backendMessage
  if (status === 400) return 'Dữ liệu không hợp lệ. Vui lòng kiểm tra lại.'
  if (status === 403) return 'Bạn không có quyền thực hiện thao tác này.'
  if (status === 404) return 'Không tìm thấy thông báo.'
  if (status >= 500) return 'Dịch vụ thông báo đang gặp sự cố. Vui lòng thử lại sau.'
  if (error?.code === 'ERR_NETWORK') return 'Không thể kết nối notification-service tại port 8084.'
  return fallback
}
