export type NotificationType = 'ANNOUNCEMENT' | 'INVOICE' | 'CONTRACT' | 'MAINTENANCE' | 'SYSTEM'
export type NotificationPriority = 'LOW' | 'NORMAL' | 'HIGH' | 'URGENT'
export type DeliveryChannel = 'IN_APP' | 'EMAIL'
export type NotificationStatus = 'PENDING' | 'SENT' | 'FAILED' | 'CANCELLED'

export interface AnnouncementDTO {
  title: string
  content: string
  priority: NotificationPriority
  recipientGroup: string
  channels: DeliveryChannel[]
  recipientIds: string[]
  scheduledAt?: string | null
}

export interface NotificationDTO {
  id: string
  title: string
  content: string
  type: NotificationType
  priority: NotificationPriority
  recipientGroup: string
  channels: DeliveryChannel[]
  status: NotificationStatus
  createdAt: string
  scheduledAt: string | null
  sentAt: string | null
  read: boolean
  failureReason: string | null
}

export interface PageResponse<T> {
  content: T[]
  page: number
  size: number
  totalElements: number
  totalPages: number
}
