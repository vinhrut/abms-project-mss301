/** Utility helpers for multi-service invoice create & display. */

export const METER_UNITS = new Set(['kwh', 'm3'])
export const CAR_EQUIVALENT_FOR_MOTORBIKE = 0.1

export function formatMoney(value) {
  const amount = Number(value || 0)
  return new Intl.NumberFormat('vi-VN').format(amount)
}

export function isMeterService(service) {
  return METER_UNITS.has(String(service?.unit || '').toLowerCase())
}

export function isLateFeeService(service) {
  const name = String(service?.name || '').toLowerCase()
  return name.includes('late') || name.includes('trễ') || name.includes('tre')
}

export function isParkingService(service) {
  const name = String(service?.name || '').toLowerCase()
  const unit = String(service?.unit || '').toLowerCase()
  return name.includes('parking') || name.includes('giữ xe') || unit === 'slot'
}

export function normalizeVehicleType(type) {
  return String(type || '').trim().toUpperCase()
}

export function isCarType(type) {
  const normalized = normalizeVehicleType(type)
  return normalized === 'CAR' || normalized === 'OTO' || normalized === 'ÔTÔ'
}

export function isMotorbikeType(type) {
  const normalized = normalizeVehicleType(type)
  return (
    normalized === 'MOTORBIKE' ||
    normalized === 'MOTORCYCLE' ||
    normalized === 'XEMAY' ||
    normalized === 'XE_MAY'
  )
}

/**
 * Billing qty in car-equivalent slots:
 * 1 CAR = 1.0, 1 MOTORBIKE = 0.1 (motorcycle fee = car unit_price / 10)
 */
export function summarizeApprovedParking(vehicles = []) {
  const approved = vehicles.filter(
    (vehicle) => String(vehicle.status || '').toUpperCase() === 'APPROVED',
  )

  let carCount = 0
  let motorbikeCount = 0
  const ignored = []

  for (const vehicle of approved) {
    if (isCarType(vehicle.type)) {
      carCount += 1
    } else if (isMotorbikeType(vehicle.type)) {
      motorbikeCount += 1
    } else {
      ignored.push(vehicle)
    }
  }

  const quantity = Number(
    (carCount + motorbikeCount * CAR_EQUIVALENT_FOR_MOTORBIKE).toFixed(2),
  )

  return {
    carCount,
    motorbikeCount,
    quantity,
    approvedCount: approved.length,
    vehicles: approved,
    ignored,
  }
}

export function parkingFeeBreakdown(unitPrice, summary) {
  const carUnit = Number(unitPrice || 0)
  const motoUnit = carUnit * CAR_EQUIVALENT_FOR_MOTORBIKE
  return {
    carUnit,
    motoUnit,
    carAmount: carUnit * Number(summary.carCount || 0),
    motoAmount: motoUnit * Number(summary.motorbikeCount || 0),
    total:
      carUnit * Number(summary.carCount || 0) +
      motoUnit * Number(summary.motorbikeCount || 0),
  }
}

/**
 * Payment due on the 5th of the month after billingMonth.
 * Example: billingMonth 2026-07-01 → due 2026-08-05
 */
export function getInvoiceDueDate(billingMonth) {
  if (!billingMonth) {
    return null
  }
  const base = new Date(`${billingMonth}T00:00:00`)
  if (Number.isNaN(base.getTime())) {
    return null
  }
  return new Date(base.getFullYear(), base.getMonth() + 1, 5)
}

export function formatDate(date) {
  if (!date) {
    return '-'
  }
  return new Intl.DateTimeFormat('vi-VN').format(date)
}

export function isPastDue(billingMonth, status, now = new Date()) {
  if (!billingMonth || status === 'PAID') {
    return false
  }
  const due = getInvoiceDueDate(billingMonth)
  if (!due) {
    return false
  }
  const today = new Date(now.getFullYear(), now.getMonth(), now.getDate())
  return today > due
}

export function lineAmount(unitPrice, quantity) {
  return Number(unitPrice || 0) * Number(quantity || 0)
}

/**
 * Build default line-state map from services list + selected apartment.
 */
export function buildDefaultServiceLines(services, apartment, parkingSummary = null) {
  const area = Number(apartment?.area || 0)
  const lines = {}

  for (const service of services) {
    const id = String(service.serviceId)
    if (isMeterService(service)) {
      lines[id] = {
        serviceId: service.serviceId,
        enabled: true,
        mode: 'meter',
        oldIndex: '0',
        newIndex: '0',
        quantity: '0',
      }
      continue
    }

    if (isLateFeeService(service)) {
      lines[id] = {
        serviceId: service.serviceId,
        enabled: false,
        mode: 'fixed',
        oldIndex: '0',
        newIndex: '1',
        quantity: '1',
      }
      continue
    }

    if (isParkingService(service)) {
      const quantity = parkingSummary?.quantity || 0
      lines[id] = {
        serviceId: service.serviceId,
        enabled: quantity > 0,
        mode: 'parking',
        oldIndex: '0',
        newIndex: String(quantity),
        quantity: String(quantity),
        carCount: parkingSummary?.carCount || 0,
        motorbikeCount: parkingSummary?.motorbikeCount || 0,
      }
      continue
    }

    const unit = String(service.unit || '').toLowerCase()
    let quantity = '1'
    if (unit === 'm2') {
      quantity = area > 0 ? String(area) : '1'
    }

    lines[id] = {
      serviceId: service.serviceId,
      enabled: true,
      mode: 'quantity',
      oldIndex: '0',
      newIndex: quantity,
      quantity,
    }
  }

  return lines
}

export function applyParkingSummaryToLines(lines, parkingService, parkingSummary) {
  if (!parkingService) {
    return lines
  }

  const key = String(parkingService.serviceId)
  const quantity = parkingSummary?.quantity || 0
  return {
    ...lines,
    [key]: {
      ...(lines[key] || {}),
      serviceId: parkingService.serviceId,
      enabled: quantity > 0,
      mode: 'parking',
      oldIndex: '0',
      newIndex: String(quantity),
      quantity: String(quantity),
      carCount: parkingSummary?.carCount || 0,
      motorbikeCount: parkingSummary?.motorbikeCount || 0,
    },
  }
}

/**
 * Prefill meter oldIndex from the latest invoice that has a saved newIndex
 * for that apartment + service (electricity/water).
 */
export function applyPreviousMeterIndexes(lines, services, apartmentInvoices) {
  if (!lines || !Array.isArray(services) || !Array.isArray(apartmentInvoices)) {
    return lines
  }

  const sorted = [...apartmentInvoices].sort((a, b) => {
    const ma = String(a.billingMonth || '')
    const mb = String(b.billingMonth || '')
    return mb.localeCompare(ma)
  })

  const next = { ...lines }
  for (const service of services) {
    if (!isMeterService(service)) {
      continue
    }
    const key = String(service.serviceId)
    if (!next[key]) {
      continue
    }

    let lastNewIndex = null
    for (const invoice of sorted) {
      const detail = (invoice.details || []).find(
        (item) =>
          Number(item.serviceId) === Number(service.serviceId) &&
          item.newIndex != null &&
          item.newIndex !== '',
      )
      if (detail) {
        lastNewIndex = detail.newIndex
        break
      }
    }

    if (lastNewIndex == null) {
      continue
    }

    const oldIndex = String(lastNewIndex)
    next[key] = {
      ...next[key],
      oldIndex,
      // Keep form valid until staff enters the new reading for this month.
      newIndex: oldIndex,
    }
  }

  return next
}

export function quantityFromLine(line) {
  if (!line?.enabled) {
    return 0
  }
  if (line.mode === 'meter') {
    return Math.max(0, Number(line.newIndex || 0) - Number(line.oldIndex || 0))
  }
  return Math.max(0, Number(line.quantity || 0))
}

export function buildReadingsPayload(services, lines) {
  return services
    .filter((service) => lines[String(service.serviceId)]?.enabled)
    .map((service) => {
      const line = lines[String(service.serviceId)]
      const qty = quantityFromLine(line)
      if (line.mode === 'meter') {
        return {
          serviceId: service.serviceId,
          oldIndex: Number(line.oldIndex || 0),
          newIndex: Number(line.newIndex || 0),
        }
      }
      return {
        serviceId: service.serviceId,
        oldIndex: 0,
        newIndex: qty,
      }
    })
    .filter((reading) => reading.newIndex - reading.oldIndex > 0)
}
