export interface RevenueBreakdown {
  feeType: string
  amount: number
  percentage: number
}

export interface FinancialReport {
  reportType: 'MONTHLY_REVENUE'
  month: number
  year: number
  totalInvoiced: number
  totalCollected: number
  totalPending: number
  totalOverdue: number
  totalInvoices: number
  paidInvoices: number
  pendingInvoices: number
  overdueInvoices: number
  revenueBreakdown: RevenueBreakdown[]
  generatedAt: string
}

export type ReportExportFormat = 'PDF' | 'EXCEL'

export interface FinancialReportRequest {
  reportType?: 'MONTHLY_REVENUE'
  month: number
  year: number
  format: ReportExportFormat
}
