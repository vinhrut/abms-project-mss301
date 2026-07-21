/**
 * Aggregate dashboard overview from apartment / invoice / maintenance / contract APIs.
 * No dedicated dashboard backend — FE-only composition.
 */

const MONTH_LABELS = ['Jan', 'Feb', 'Mar', 'Apr', 'May', 'Jun', 'Jul', 'Aug', 'Sep', 'Oct', 'Nov', 'Dec']
const OPEN_MAINTENANCE = new Set(['OPEN', 'IN_PROGRESS'])
const OCCUPIED_STATUSES = new Set(['OCCUPIED', 'ACTIVE'])

function toArray(value) {
  return Array.isArray(value) ? value : []
}

function parseDate(value) {
  if (!value) return null
  const date = new Date(value)
  return Number.isNaN(date.getTime()) ? null : date
}

function startOfMonth(year, monthIndex) {
  return new Date(year, monthIndex, 1)
}

function endOfMonth(year, monthIndex) {
  return new Date(year, monthIndex + 1, 0, 23, 59, 59, 999)
}

function billingMonthKey(value) {
  if (!value) return ''
  const text = String(value)
  // "2026-07-01" or "2026-07"
  return text.slice(0, 7)
}

function lastNMonths(count, now = new Date()) {
  const months = []
  for (let i = count - 1; i >= 0; i -= 1) {
    const date = new Date(now.getFullYear(), now.getMonth() - i, 1)
    months.push({
      year: date.getFullYear(),
      monthIndex: date.getMonth(),
      key: `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}`,
      label: MONTH_LABELS[date.getMonth()],
    })
  }
  return months
}

function isOccupiedApartment(apartment) {
  return OCCUPIED_STATUSES.has(String(apartment?.status || '').toUpperCase())
}

function isPendingInvoice(invoice) {
  const status = String(invoice?.status || '').toUpperCase()
  const display = String(invoice?.displayStatus || status).toUpperCase()
  return status !== 'PAID' && display !== 'OVERDUE'
}

function isOverdueInvoice(invoice) {
  const status = String(invoice?.status || '').toUpperCase()
  const display = String(invoice?.displayStatus || status).toUpperCase()
  return status !== 'PAID' && display === 'OVERDUE'
}

function isOpenMaintenance(request) {
  return OPEN_MAINTENANCE.has(String(request?.status || '').toUpperCase())
}

function isUrgentOpenMaintenance(request) {
  if (!isOpenMaintenance(request)) return false
  const priority = String(request?.priority || '').toUpperCase()
  return priority === 'EMERGENCY' || priority === 'URGENT' || priority === 'HIGH'
}

function isActiveContract(contract) {
  const status = String(contract?.status || '').toUpperCase()
  return !status || status === 'ACTIVE'
}

function contractOverlapsMonth(contract, year, monthIndex) {
  if (!isActiveContract(contract)) return false
  const start = parseDate(contract.startDate) || parseDate(contract.endDate)
  const end = parseDate(contract.endDate)
  if (!start && !end) return false

  const monthStart = startOfMonth(year, monthIndex)
  const monthEnd = endOfMonth(year, monthIndex)
  const rangeStart = start || monthStart
  const rangeEnd = end || monthEnd
  return rangeStart <= monthEnd && rangeEnd >= monthStart
}

function isExpiringWithinDays(contract, days, now = new Date()) {
  if (!isActiveContract(contract)) return false
  const end = parseDate(contract.endDate)
  if (!end) return false
  const limit = new Date(now)
  limit.setDate(limit.getDate() + days)
  return end >= now && end <= limit
}

/**
 * @param {{
 *   apartments?: unknown[]
 *   invoices?: unknown[]
 *   maintenanceRequests?: unknown[]
 *   contracts?: unknown[]
 *   now?: Date
 * }} input
 */
export function aggregateDashboardOverview({
  apartments = [],
  invoices = [],
  maintenanceRequests = [],
  contracts = [],
  now = new Date(),
} = {}) {
  const apartmentList = toArray(apartments)
  const invoiceList = toArray(invoices)
  const maintenanceList = toArray(maintenanceRequests)
  const contractList = toArray(contracts)
  const months = lastNMonths(6, now)
  const currentKey = months[months.length - 1]?.key

  const totalApartments = apartmentList.length
  const occupiedApartments = apartmentList.filter(isOccupiedApartment).length
  const occupancyRate = totalApartments
    ? Math.round((occupiedApartments / totalApartments) * 100)
    : 0

  const revenueByMonth = new Map(months.map((month) => [month.key, 0]))
  for (const invoice of invoiceList) {
    const key = billingMonthKey(invoice.billingMonth)
    if (!revenueByMonth.has(key)) continue
    revenueByMonth.set(key, (revenueByMonth.get(key) || 0) + (Number(invoice.paidAmount) || 0))
  }

  const monthlyRevenueTrend = months.map((month) => ({
    label: month.label,
    value: revenueByMonth.get(month.key) || 0,
  }))

  const revenueThisMonth = revenueByMonth.get(currentKey) || 0

  const pendingInvoices = invoiceList.filter(isPendingInvoice).length
  const overdueInvoices = invoiceList.filter(isOverdueInvoice).length

  const openMaintenance = maintenanceList.filter(isOpenMaintenance)
  const urgentMaintenance = maintenanceList.filter(isUrgentOpenMaintenance)
  const expiringContracts = contractList.filter((contract) => isExpiringWithinDays(contract, 30, now))

  const apartmentIds = new Set(apartmentList.map((item) => String(item.apartmentId || '')))
  const occupancyHistory = months.map((month) => {
    const occupiedIds = new Set()
    for (const contract of contractList) {
      if (!contractOverlapsMonth(contract, month.year, month.monthIndex)) continue
      const apartmentId = String(contract.apartmentId || '')
      if (apartmentIds.size && apartmentId && !apartmentIds.has(apartmentId)) continue
      if (apartmentId) occupiedIds.add(apartmentId)
    }
    const total = totalApartments || occupiedIds.size
    const rate = total ? Math.round((occupiedIds.size / total) * 100) : 0
    return { label: month.label, value: rate }
  })

  // Prefer live apartment status for the current month point
  if (occupancyHistory.length) {
    occupancyHistory[occupancyHistory.length - 1] = {
      label: months[months.length - 1].label,
      value: occupancyRate,
    }
  }

  const alerts = []
  if (overdueInvoices > 0) {
    alerts.push({
      id: 'overdue-invoices',
      type: 'OVERDUE_INVOICE',
      text: `${overdueInvoices} Overdue Invoice${overdueInvoices === 1 ? '' : 's'} require follow-up`,
    })
  }
  if (expiringContracts.length > 0) {
    alerts.push({
      id: 'expiring-contracts',
      type: 'EXPIRING_CONTRACT',
      text: `${expiringContracts.length} Contract${expiringContracts.length === 1 ? '' : 's'} expiring within 30 days`,
    })
  }
  if (urgentMaintenance.length > 0) {
    alerts.push({
      id: 'urgent-maintenance',
      type: 'URGENT_MAINTENANCE',
      text: `${urgentMaintenance.length} Urgent Maintenance Request${urgentMaintenance.length === 1 ? '' : 's'} pending`,
    })
  }

  return {
    summary: {
      occupancyRate,
      revenueThisMonth,
      pendingInvoices,
      openMaintenanceRequests: openMaintenance.length,
    },
    monthlyRevenueTrend,
    occupancyHistory,
    alerts,
  }
}

export function formatDashboardMoney(amount) {
  return `${Number(amount || 0).toLocaleString('en-US')} VND`
}

export function emptyDashboardOverview() {
  return {
    summary: {
      occupancyRate: 0,
      revenueThisMonth: 0,
      pendingInvoices: 0,
      openMaintenanceRequests: 0,
    },
    monthlyRevenueTrend: [],
    occupancyHistory: [],
    alerts: [],
  }
}
