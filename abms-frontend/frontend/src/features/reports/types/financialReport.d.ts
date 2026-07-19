export interface RevenueBreakdown { feeType: string; amount: number; percentage: number }
export interface FinancialReport { month:number; year:number; totalInvoiced:number; totalCollected:number; totalPending:number; totalOverdue:number; totalInvoices:number; paidInvoices:number; pendingInvoices:number; overdueInvoices:number; revenueBreakdown:RevenueBreakdown[]; generatedAt:string }
export type ReportExportFormat = 'PDF' | 'EXCEL' | 'XLSX'
export interface FinancialReportRequest { month:number; year:number; format:ReportExportFormat }
export interface ApiError { timestamp:string; status:number; error:string; message:string; validationErrors?:Record<string,string> }
