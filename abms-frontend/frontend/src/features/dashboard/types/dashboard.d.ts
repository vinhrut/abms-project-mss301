export type DashboardAlertType = 'OVERDUE_INVOICE' | 'EXPIRING_CONTRACT' | 'URGENT_MAINTENANCE'

export interface DashboardSummary {
  occupancyRate: number
  revenueThisMonth: number
  pendingInvoices: number
  openMaintenanceRequests: number
}

export interface DashboardSeriesPoint {
  label: string
  value: number
}

export interface DashboardAlert {
  id: string
  text: string
  type: DashboardAlertType
}

export interface DashboardOverview {
  summary: DashboardSummary
  monthlyRevenueTrend: DashboardSeriesPoint[]
  occupancyHistory: DashboardSeriesPoint[]
  alerts: DashboardAlert[]
}

export type DashboardExportFormat = 'PDF' | 'EXCEL'

export interface DashboardExportRequest {
  format: DashboardExportFormat
  fromDate: string
  toDate: string
}
