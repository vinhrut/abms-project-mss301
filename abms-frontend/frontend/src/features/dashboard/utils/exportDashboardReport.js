import { formatDashboardMoney } from './aggregateDashboardOverview.js'

function downloadBlob(blob, filename) {
  const url = URL.createObjectURL(blob)
  const anchor = document.createElement('a')
  anchor.href = url
  anchor.download = filename
  anchor.click()
  URL.revokeObjectURL(url)
}

function toCsv(overview, fromDate, toDate) {
  const { summary, monthlyRevenueTrend, occupancyHistory, alerts } = overview
  const lines = [
    ['Dashboard Report'],
    ['From Date', fromDate],
    ['To Date', toDate],
    ['Generated At', new Date().toISOString()],
    [],
    ['Metric', 'Value'],
    ['Occupancy Rate', `${summary.occupancyRate}%`],
    ['Revenue (This Month)', summary.revenueThisMonth],
    ['Pending Invoices', summary.pendingInvoices],
    ['Open Maintenance Requests', summary.openMaintenanceRequests],
    [],
    ['Monthly Revenue Trend'],
    ['Period', 'Revenue (VND)'],
    ...monthlyRevenueTrend.map((row) => [row.label, row.value]),
    [],
    ['Occupancy Rate History'],
    ['Period', 'Occupancy (%)'],
    ...occupancyHistory.map((row) => [row.label, row.value]),
    [],
    ['Alerts'],
    ...alerts.map((alert) => [alert.type, alert.text]),
  ]

  return lines
    .map((row) => row.map((cell) => `"${String(cell ?? '').replaceAll('"', '""')}"`).join(','))
    .join('\n')
}

function toPrintableHtml(overview, fromDate, toDate) {
  const { summary, monthlyRevenueTrend, occupancyHistory, alerts } = overview
  const revenueRows = monthlyRevenueTrend
    .map((row) => `<tr><td>${row.label}</td><td>${formatDashboardMoney(row.value)}</td></tr>`)
    .join('')
  const occupancyRows = occupancyHistory
    .map((row) => `<tr><td>${row.label}</td><td>${row.value}%</td></tr>`)
    .join('')
  const alertRows = alerts.map((alert) => `<li>${alert.text}</li>`).join('')

  return `<!doctype html><html><head><meta charset="utf-8"><title>Dashboard Report</title>
    <style>
      body{font-family:Arial,sans-serif;padding:24px;color:#1e293b}
      h1,h2{margin:0 0 12px}p{margin:0 0 8px}
      table{border-collapse:collapse;width:100%;margin:12px 0 20px}
      th,td{border:1px solid #cbd5e1;padding:8px;text-align:left}
      .cards{display:grid;grid-template-columns:repeat(2,1fr);gap:12px;margin:16px 0}
      .card{border:1px solid #cbd5e1;border-radius:8px;padding:12px}
      .card strong{display:block;font-size:18px;margin-top:6px}
    </style></head><body>
    <h1>Dashboard Report</h1>
    <p>From: ${fromDate} &nbsp;|&nbsp; To: ${toDate}</p>
    <div class="cards">
      <div class="card">Occupancy Rate<strong>${summary.occupancyRate}%</strong></div>
      <div class="card">Revenue (This Month)<strong>${formatDashboardMoney(summary.revenueThisMonth)}</strong></div>
      <div class="card">Pending Invoices<strong>${summary.pendingInvoices}</strong></div>
      <div class="card">Maintenance Req.<strong>${summary.openMaintenanceRequests} Open</strong></div>
    </div>
    <h2>Monthly Revenue Trend</h2>
    <table><thead><tr><th>Period</th><th>Revenue</th></tr></thead><tbody>${revenueRows}</tbody></table>
    <h2>Occupancy Rate History</h2>
    <table><thead><tr><th>Period</th><th>Occupancy</th></tr></thead><tbody>${occupancyRows}</tbody></table>
    <h2>Alerts</h2>
    <ul>${alertRows}</ul>
    </body></html>`
}

/**
 * UC-DASH-02 FE export (mock overview). Backend PDF/XLSX can replace this later.
 */
export async function exportDashboardReport({ overview, format, fromDate, toDate }) {
  const stamp = `${fromDate}_to_${toDate}`

  if (format === 'EXCEL') {
    const csv = `\uFEFF${toCsv(overview, fromDate, toDate)}`
    downloadBlob(
      new Blob([csv], { type: 'text/csv;charset=utf-8' }),
      `dashboard-report-${stamp}.csv`,
    )
    return { delivered: 'download' }
  }

  const html = toPrintableHtml(overview, fromDate, toDate)
  const popup = window.open('', '_blank', 'noopener,noreferrer,width=960,height=720')
  if (!popup) {
    downloadBlob(new Blob([html], { type: 'text/html;charset=utf-8' }), `dashboard-report-${stamp}.html`)
    return { delivered: 'download' }
  }
  popup.document.write(html)
  popup.document.close()
  popup.focus()
  popup.print()
  return { delivered: 'print' }
}
