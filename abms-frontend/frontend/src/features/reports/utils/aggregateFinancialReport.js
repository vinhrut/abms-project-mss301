/**
 * Aggregate financial report from finance invoices (no report-service / no DB save).
 */
export function aggregateFinancialReport(invoices = [], month, year) {
  const list = Array.isArray(invoices) ? invoices : []
  let totalInvoiced = 0
  let totalCollected = 0
  let totalPending = 0
  let totalOverdue = 0
  let paidInvoices = 0
  let pendingInvoices = 0
  let overdueInvoices = 0
  const feeMap = new Map()

  for (const invoice of list) {
    const total = Number(invoice.totalAmount) || 0
    const paid = Number(invoice.paidAmount) || 0
    const remaining = Number(invoice.remainingAmount != null
      ? invoice.remainingAmount
      : Math.max(total - paid, 0)) || 0
    const status = String(invoice.status || '').toUpperCase()
    const display = String(invoice.displayStatus || status).toUpperCase()

    totalInvoiced += total
    totalCollected += paid

    if (status === 'PAID') {
      paidInvoices += 1
    } else if (display === 'OVERDUE') {
      overdueInvoices += 1
      totalOverdue += remaining
    } else {
      pendingInvoices += 1
      totalPending += remaining
    }

    const details = Array.isArray(invoice.details) ? invoice.details : []
    for (const line of details) {
      const feeType = line.serviceName || `Service #${line.serviceId || '?'}`
      const amount = Number(line.amount) || 0
      feeMap.set(feeType, (feeMap.get(feeType) || 0) + amount)
    }
  }

  const feeTotal = [...feeMap.values()].reduce((sum, value) => sum + value, 0)
  const revenueBreakdown = [...feeMap.entries()]
    .map(([feeType, amount]) => ({
      feeType,
      amount,
      percentage: feeTotal > 0 ? Number(((amount / feeTotal) * 100).toFixed(2)) : 0,
    }))
    .sort((a, b) => b.amount - a.amount)

  return {
    reportType: 'MONTHLY_REVENUE',
    month: Number(month),
    year: Number(year),
    totalInvoiced,
    totalCollected,
    totalPending,
    totalOverdue,
    totalInvoices: list.length,
    paidInvoices,
    pendingInvoices,
    overdueInvoices,
    revenueBreakdown,
    generatedAt: new Date().toISOString(),
  }
}

export function formatMoneyVnd(value) {
  return new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(Number(value) || 0)
}
