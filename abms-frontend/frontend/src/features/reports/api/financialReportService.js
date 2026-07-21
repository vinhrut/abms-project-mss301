import { invoiceService } from '../../../services/invoiceService.js'
import { aggregateFinancialReport } from '../utils/aggregateFinancialReport.js'

function billingMonthParam(month, year) {
  const m = String(month).padStart(2, '0')
  return `${year}-${m}-01`
}

function downloadBlob(blob, filename) {
  const url = URL.createObjectURL(blob)
  const anchor = document.createElement('a')
  anchor.href = url
  anchor.download = filename
  anchor.click()
  URL.revokeObjectURL(url)
}

function toCsv(report) {
  const lines = [
    ['Financial Report', 'Monthly Revenue'],
    ['Period', `${String(report.month).padStart(2, '0')}/${report.year}`],
    ['Generated At', report.generatedAt],
    [],
    ['Metric', 'Amount (VND)'],
    ['Total Collected', report.totalCollected],
    ['Pending', report.totalPending],
    ['Overdue', report.totalOverdue],
    ['Total Invoiced', report.totalInvoiced],
    [],
    ['Fee Type', 'Amount', '% of Total'],
    ...report.revenueBreakdown.map((row) => [row.feeType, row.amount, row.percentage]),
  ]
  return lines
    .map((row) => row.map((cell) => `"${String(cell ?? '').replaceAll('"', '""')}"`).join(','))
    .join('\n')
}

function toPrintableHtml(report) {
  const money = (value) => new Intl.NumberFormat('vi-VN').format(Number(value) || 0)
  const rows = report.revenueBreakdown
    .map((row) => `<tr><td>${row.feeType}</td><td>${money(row.amount)}</td><td>${row.percentage}%</td></tr>`)
    .join('')
  return `<!doctype html><html><head><meta charset="utf-8"><title>Financial Report</title>
    <style>body{font-family:Arial,sans-serif;padding:24px}table{border-collapse:collapse;width:100%;margin-top:12px}
    th,td{border:1px solid #ccc;padding:8px;text-align:left}h1,h2{margin:0 0 8px}</style></head><body>
    <h1>Financial Report — Monthly Revenue</h1>
    <p>Period: ${String(report.month).padStart(2, '0')}/${report.year}</p>
    <p>Total Collected: ${money(report.totalCollected)} VND</p>
    <p>Pending: ${money(report.totalPending)} VND</p>
    <p>Overdue: ${money(report.totalOverdue)} VND</p>
    <h2>Fee Type</h2>
    <table><thead><tr><th>Fee Type</th><th>Amount</th><th>% of Total</th></tr></thead><tbody>${rows}</tbody></table>
    </body></html>`
}

export const financialReportService = {
  async preview(month, year) {
    const invoices = await invoiceService.getInvoices({
      billingMonth: billingMonthParam(month, year),
    })
    return aggregateFinancialReport(invoices, month, year)
  },

  async exportReport(report, format = 'PDF') {
    const period = `${report.year}-${String(report.month).padStart(2, '0')}`
    if (format === 'EXCEL') {
      const csv = `\uFEFF${toCsv(report)}`
      downloadBlob(new Blob([csv], { type: 'text/csv;charset=utf-8' }), `financial-report-${period}.csv`)
      return
    }

    const html = toPrintableHtml(report)
    const popup = window.open('', '_blank', 'noopener,noreferrer,width=900,height=700')
    if (!popup) {
      downloadBlob(new Blob([html], { type: 'text/html;charset=utf-8' }), `financial-report-${period}.html`)
      return
    }
    popup.document.write(html)
    popup.document.close()
    popup.focus()
    popup.print()
  },
}
