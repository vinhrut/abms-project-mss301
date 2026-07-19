import { useEffect, useMemo, useState } from 'react'
import { useSearchParams } from 'react-router-dom'
import { useAuth } from '../../auth/context/useAuth.js'
import { apartmentService } from '../../../services/apartmentService.js'
import { invoiceService } from '../../../services/invoiceService.js'
import { paymentService } from '../../../services/paymentService.js'
import { vehicleService } from '../../../services/vehicleService.js'
import { extractApiErrorMessage } from '../../../utils/apiError.js'
import { validateInvoiceCreateForm } from '../utils/invoiceValidation.js'
import { validateCashPaymentForm } from '../utils/paymentValidation.js'
import {
  applyParkingSummaryToLines,
  applyPreviousMeterIndexes,
  buildDefaultServiceLines,
  buildReadingsPayload,
  formatDate,
  formatMoney,
  getInvoiceDueDate,
  isLateFeeService,
  isMeterService,
  isParkingService,
  isPastDue,
  lineAmount,
  parkingFeeBreakdown,
  quantityFromLine,
  summarizeApprovedParking,
} from '../utils/billingHelpers.js'

const statusMap = {
  UNPAID: 'badge badge-warning',
  PARTIAL: 'badge badge-warning',
  PAID: 'badge badge-success',
  OVERDUE: 'badge badge-danger',
}

function formatMonthLabel(yyyyMm) {
  if (!yyyyMm || !/^\d{4}-\d{2}$/.test(yyyyMm)) {
    return ''
  }
  const [y, m] = yyyyMm.split('-')
  return `Tháng ${Number(m)}/${y}`
}

export function InvoiceListPage() {
  const { auth, isElevatedRole } = useAuth()
  const [searchParams] = useSearchParams()
  const [invoices, setInvoices] = useState([])
  const [apartments, setApartments] = useState([])
  const [services, setServices] = useState([])
  const [parkingVehicles, setParkingVehicles] = useState([])
  const [parkingLoading, setParkingLoading] = useState(false)
  const [parkingError, setParkingError] = useState('')
  const [residentApartmentId, setResidentApartmentId] = useState('')
  const [filterApartmentId, setFilterApartmentId] = useState('')
  const [filterMonth, setFilterMonth] = useState('')
  const [filterStatus, setFilterStatus] = useState('')
  const [searchTerm, setSearchTerm] = useState('')
  const [apartmentId, setApartmentId] = useState('')
  const [billingMonth, setBillingMonth] = useState('')
  const [serviceLines, setServiceLines] = useState({})
  const [formErrors, setFormErrors] = useState({})
  const [loading, setLoading] = useState(false)
  const [submitting, setSubmitting] = useState(false)
  const [listError, setListError] = useState('')
  const [createError, setCreateError] = useState('')
  const [createMessage, setCreateMessage] = useState('')
  const [actionMessage, setActionMessage] = useState('')
  const [cashModal, setCashModal] = useState(null)
  const [cashErrors, setCashErrors] = useState({})
  const [expandedId, setExpandedId] = useState(null)
  const [focusInvoiceId, setFocusInvoiceId] = useState('')

  const selectedApartment = useMemo(
    () => apartments.find((item) => item.apartmentId === apartmentId) || null,
    [apartments, apartmentId],
  )

  const meterServices = useMemo(
    () => services.filter((service) => isMeterService(service)),
    [services],
  )
  const parkingService = useMemo(
    () => services.find((service) => isParkingService(service)) || null,
    [services],
  )
  const fixedServices = useMemo(
    () =>
      services.filter(
        (service) =>
          !isMeterService(service) &&
          !isLateFeeService(service) &&
          !isParkingService(service),
      ),
    [services],
  )
  const lateFeeService = useMemo(
    () => services.find((service) => isLateFeeService(service)) || null,
    [services],
  )

  const parkingSummary = useMemo(
    () => summarizeApprovedParking(parkingVehicles),
    [parkingVehicles],
  )
  const parkingBreakdown = useMemo(
    () =>
      parkingService
        ? parkingFeeBreakdown(parkingService.unitPrice, parkingSummary)
        : null,
    [parkingService, parkingSummary],
  )

  const dueDate = useMemo(() => getInvoiceDueDate(`${billingMonth}-01`), [billingMonth])
  const shouldSuggestLateFee = useMemo(() => {
    if (!billingMonth || !dueDate) {
      return false
    }
    return isPastDue(`${billingMonth}-01`, 'UNPAID')
  }, [billingMonth, dueDate])

  const previewTotal = useMemo(() => {
    return services.reduce((sum, service) => {
      const line = serviceLines[String(service.serviceId)]
      if (!line?.enabled) {
        return sum
      }
      return sum + lineAmount(service.unitPrice, quantityFromLine(line))
    }, 0)
  }, [services, serviceLines])

  const syncParkingForApartment = async (nextApartmentId, nextServices = services) => {
    const parking = nextServices.find((service) => isParkingService(service)) || null
    if (!nextApartmentId || !parking) {
      setParkingVehicles([])
      setParkingError('')
      setServiceLines((prev) =>
        applyParkingSummaryToLines(prev, parking, summarizeApprovedParking([])),
      )
      return
    }

    try {
      setParkingLoading(true)
      setParkingError('')
      const vehicles = await vehicleService.getVehiclesByApartmentId(nextApartmentId)
      const list = Array.isArray(vehicles) ? vehicles : []
      setParkingVehicles(list)
      const summary = summarizeApprovedParking(list)
      setServiceLines((prev) => applyParkingSummaryToLines(prev, parking, summary))
    } catch (apiError) {
      setParkingVehicles([])
      setParkingError(
        extractApiErrorMessage(
          apiError,
          'Không tải được danh sách xe của căn hộ. Phí gửi xe sẽ để 0.',
        ),
      )
      setServiceLines((prev) =>
        applyParkingSummaryToLines(prev, parking, summarizeApprovedParking([])),
      )
    } finally {
      setParkingLoading(false)
    }
  }

  const resetCreateForm = (nextApartmentId = '', nextServices = services) => {
    const apartment =
      apartments.find((item) => item.apartmentId === nextApartmentId) || null
    setApartmentId(nextApartmentId)
    setBillingMonth('')
    setServiceLines(buildDefaultServiceLines(nextServices, apartment, null))
    setFormErrors({})
    syncParkingForApartment(nextApartmentId, nextServices)
  }

  const loadInvoices = async () => {
    try {
      setLoading(true)
      setListError('')

      let response = []

      if (isElevatedRole) {
        const params = {}
        if (filterApartmentId.trim()) {
          params.apartmentId = filterApartmentId.trim()
        }
        if (filterMonth) {
          params.billingMonth = `${filterMonth}-01`
        }
        if (filterStatus) {
          params.status = filterStatus
        }
        response = await invoiceService.getInvoices(params)
      } else if (residentApartmentId) {
        response = await invoiceService.getInvoicesByApartmentId(residentApartmentId)
      } else if (auth?.userId) {
        const residence = await apartmentService.getActiveResidenceByUserId(auth.userId)
        const nextApartmentId = residence?.apartmentId || ''
        setResidentApartmentId(nextApartmentId)
        if (nextApartmentId) {
          response = await invoiceService.getInvoicesByApartmentId(nextApartmentId)
        }
      }

      setInvoices(Array.isArray(response) ? response : [])
    } catch (apiError) {
      setListError(
        extractApiErrorMessage(
          apiError,
          'Không thể tải danh sách hóa đơn. Vui lòng thử lại.',
        ),
      )
      setInvoices([])
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    if (!isElevatedRole) {
      return
    }

    Promise.all([
      apartmentService.getAllApartments().catch(() => []),
      invoiceService.getServices().catch(() => []),
    ]).then(([apartmentData, serviceData]) => {
      const nextApartments = Array.isArray(apartmentData) ? apartmentData : []
      const nextServices = Array.isArray(serviceData) ? serviceData : []
      setApartments(nextApartments)
      setServices(nextServices)
      setServiceLines(buildDefaultServiceLines(nextServices, null))
    })
  }, [isElevatedRole])

  useEffect(() => {
    if (isElevatedRole || auth?.userId) {
      loadInvoices()
    }
  }, [auth?.userId, isElevatedRole])

  useEffect(() => {
    if (!lateFeeService) {
      return
    }
    const key = String(lateFeeService.serviceId)
    setServiceLines((prev) => {
      if (!prev[key]) {
        return prev
      }
      if (prev[key].enabled === shouldSuggestLateFee) {
        return prev
      }
      return {
        ...prev,
        [key]: {
          ...prev[key],
          enabled: shouldSuggestLateFee,
          quantity: '1',
          newIndex: '1',
        },
      }
    })
  }, [shouldSuggestLateFee, lateFeeService])

  const filteredInvoices = useMemo(() => {
    const normalized = searchTerm.trim().toLowerCase()
    if (!normalized) {
      return invoices
    }

    return invoices.filter((invoice) => {
      const code = invoice.invoiceCode?.toLowerCase() || ''
      const itemApartmentId = invoice.apartmentId?.toLowerCase() || ''
      const status = (invoice.displayStatus || invoice.status || '').toLowerCase()
      return (
        code.includes(normalized) ||
        itemApartmentId.includes(normalized) ||
        status.includes(normalized)
      )
    })
  }, [invoices, searchTerm])

  const handleApartmentChange = async (event) => {
    const nextId = event.target.value
    setApartmentId(nextId)
    setCreateMessage('')
    setCreateError('')
    setFormErrors((prev) => ({ ...prev, apartmentId: '' }))
    const apartment = apartments.find((item) => item.apartmentId === nextId) || null

    let previousInvoices = []
    if (nextId) {
      try {
        const response = await invoiceService.getInvoicesByApartmentId(nextId)
        previousInvoices = Array.isArray(response) ? response : []
      } catch {
        previousInvoices = []
      }
    }

    setServiceLines((prev) => {
      let next = { ...prev }
      for (const service of services) {
        const key = String(service.serviceId)
        if (!next[key] || isParkingService(service)) {
          continue
        }
        if (String(service.unit || '').toLowerCase() === 'm2') {
          const area = Number(apartment?.area || 0)
          const qty = area > 0 ? String(area) : next[key].quantity || '1'
          next[key] = { ...next[key], quantity: qty, newIndex: qty }
        }
        if (isMeterService(service)) {
          next[key] = {
            ...next[key],
            oldIndex: '0',
            newIndex: '0',
          }
        }
      }
      return applyPreviousMeterIndexes(next, services, previousInvoices)
    })
    await syncParkingForApartment(nextId, services)
  }

  const updateServiceLine = (serviceId, patch) => {
    const key = String(serviceId)
    setServiceLines((prev) => ({
      ...prev,
      [key]: {
        ...prev[key],
        ...patch,
      },
    }))
    setFormErrors((prev) => ({ ...prev, [`service_${key}`]: '', services: '' }))
    setCreateMessage('')
    setCreateError('')
  }

  const handleCreateInvoice = async (event) => {
    event.preventDefault()
    const validationErrors = validateInvoiceCreateForm({
      apartmentId,
      billingMonth,
      services,
      lines: serviceLines,
    })
    setFormErrors(validationErrors)
    if (Object.keys(validationErrors).length > 0) {
      return
    }

    const readings = buildReadingsPayload(services, serviceLines)
    if (readings.length === 0) {
      setFormErrors({ services: 'Chọn ít nhất một dịch vụ với số lượng > 0.' })
      return
    }

    const payload = {
      apartmentId: apartmentId.trim(),
      billingMonth: `${billingMonth}-01`,
      readings,
    }

    try {
      setSubmitting(true)
      setCreateError('')
      const created = await invoiceService.createFromMeterReadings(payload)
      setCreateMessage(`Đã tạo hóa đơn ${created.invoiceCode}.`)
      resetCreateForm('', services)
      await loadInvoices()
      setExpandedId(created.invoiceId)
    } catch (apiError) {
      setCreateError(
        extractApiErrorMessage(apiError, 'Không thể tạo hóa đơn. Vui lòng thử lại.'),
      )
    } finally {
      setSubmitting(false)
    }
  }

  const payWithVnPay = async (invoice) => {
    if (!auth?.userId) {
      setListError('Không xác định được người thanh toán. Vui lòng đăng nhập lại.')
      return
    }

    try {
      setSubmitting(true)
      setListError('')
      const data = await paymentService.createVnPayPayment({
        invoiceId: invoice.invoiceId,
        payerId: auth.userId,
      })
      if (!data?.paymentUrl) {
        setListError('Không nhận được URL thanh toán VNPay.')
        return
      }
      window.location.href = data.paymentUrl
    } catch (apiError) {
      setListError(extractApiErrorMessage(apiError, 'Không thể khởi tạo thanh toán VNPay.'))
      setSubmitting(false)
    }
  }

  const openCashModal = (invoice) => {
    setCashErrors({})
    setCashModal({
      invoiceId: invoice.invoiceId,
      invoiceCode: invoice.invoiceCode,
      remainingAmount: invoice.remainingAmount,
      payerId: '',
      collectorId: auth?.userId || '',
      paidAmount: String(invoice.remainingAmount ?? ''),
    })
  }

  const handleCashSubmit = async (event) => {
    event.preventDefault()
    const validationErrors = validateCashPaymentForm(cashModal)
    setCashErrors(validationErrors)
    if (Object.keys(validationErrors).length > 0) {
      return
    }

    try {
      setSubmitting(true)
      await paymentService.recordCash({
        invoiceId: cashModal.invoiceId,
        payerId: cashModal.payerId.trim(),
        collectorId: cashModal.collectorId.trim(),
        paidAmount: Number(cashModal.paidAmount),
      })
      setCashModal(null)
      setActionMessage(`Đã ghi nhận thanh toán tiền mặt cho ${cashModal.invoiceCode}.`)
      await loadInvoices()
    } catch (apiError) {
      setListError(extractApiErrorMessage(apiError, 'Không thể ghi nhận thanh toán tiền mặt.'))
    } finally {
      setSubmitting(false)
    }
  }

  const roomLabel = (id) => {
    const found = apartments.find((item) => item.apartmentId === id)
    return found?.roomNumber || id
  }

  useEffect(() => {
    const invoiceId = searchParams.get('invoiceId') || ''
    const invoiceCode = searchParams.get('invoiceCode') || ''
    if (!invoiceId && !invoiceCode) {
      return
    }

    let targetId = invoiceId
    if (!targetId && invoiceCode) {
      const matched = invoices.find((item) => item.invoiceCode === invoiceCode)
      targetId = matched?.invoiceId || ''
    }
    if (!targetId) {
      return
    }

    setFocusInvoiceId(targetId)
    setExpandedId(targetId)
    window.requestAnimationFrame(() => {
      document.getElementById(`invoice-card-${targetId}`)?.scrollIntoView({
        behavior: 'smooth',
        block: 'center',
      })
    })
  }, [searchParams, invoices])

  return (
    <div className="page-stack">
      <section className="page-header-card">
        <div>
          <span className="eyebrow">Financial management</span>
          <h1>{isElevatedRole ? 'Quản lý hóa đơn' : 'Hóa đơn của tôi'}</h1>
          <p>
            {isElevatedRole
              ? 'Lập hóa đơn nhiều dịch vụ (điện, nước, quản lý, internet, rác…) và theo dõi hạn thanh toán ngày 5.'
              : 'Xem chi tiết từng khoản phí và hạn thanh toán trước ngày 5 hàng tháng.'}
          </p>
        </div>
      </section>

      {isElevatedRole ? (
        <section className="content-card">
          <div className="section-heading">
            <div>
              <h2>Tạo hóa đơn</h2>
              <p>
                Chọn căn hộ, kỳ thanh toán và các dịch vụ. Hạn nộp: ngày 5 tháng kế tiếp — trễ hạn
                sẽ áp dụng Late Fee.
              </p>
            </div>
          </div>

          {createError ? <div className="alert alert-error">{createError}</div> : null}
          {createMessage ? <div className="alert alert-success">{createMessage}</div> : null}
          {formErrors.services ? (
            <div className="alert alert-error">{formErrors.services}</div>
          ) : null}

          <form onSubmit={handleCreateInvoice}>
            <div className="toolbar-grid billing-create-grid">
              <label className="form-field">
                <span>Apartment</span>
                <select value={apartmentId} onChange={handleApartmentChange}>
                  <option value="">Chọn căn hộ</option>
                  {apartments.map((apartment) => (
                    <option key={apartment.apartmentId} value={apartment.apartmentId}>
                      {apartment.roomNumber || apartment.apartmentId}
                      {apartment.area ? ` (${apartment.area} m²)` : ''}
                    </option>
                  ))}
                </select>
                {formErrors.apartmentId ? (
                  <span className="field-error">{formErrors.apartmentId}</span>
                ) : null}
              </label>

              <label className="form-field form-field--month">
                <span>Tháng thanh toán</span>
                <input
                  type="month"
                  value={billingMonth}
                  onChange={(event) => {
                    setBillingMonth(event.target.value)
                    setFormErrors((prev) => ({ ...prev, billingMonth: '' }))
                    setCreateMessage('')
                    setCreateError('')
                  }}
                />
                {formErrors.billingMonth ? (
                  <span className="field-error">{formErrors.billingMonth}</span>
                ) : null}
              </label>

              <div className="bill-due-chip">
                <strong>Hạn thanh toán</strong>
                <span>{dueDate ? formatDate(dueDate) : 'Chọn tháng để xem hạn (ngày 5)'}</span>
              </div>
            </div>

            <div className="billing-subsection">
              <h3>1. Chỉ số tiêu thụ</h3>
              <div className="service-line-list">
                {meterServices.map((service) => {
                  const key = String(service.serviceId)
                  const line = serviceLines[key] || {}
                  const qty = quantityFromLine(line)
                  return (
                    <article key={key} className="service-line-card">
                      <label className="service-line-check">
                        <input
                          type="checkbox"
                          checked={Boolean(line.enabled)}
                          onChange={(event) =>
                            updateServiceLine(service.serviceId, {
                              enabled: event.target.checked,
                            })
                          }
                        />
                        <span>
                          {service.name}
                          <small>
                            {formatMoney(service.unitPrice)} VND / {service.unit}
                          </small>
                        </span>
                      </label>
                      <div className="service-line-fields">
                        <label className="form-field">
                          <span>Chỉ số cũ</span>
                          <input
                            type="number"
                            min="0"
                            step="0.01"
                            disabled={!line.enabled}
                            value={line.oldIndex ?? '0'}
                            onChange={(event) =>
                              updateServiceLine(service.serviceId, {
                                oldIndex: event.target.value,
                              })
                            }
                          />
                        </label>
                        <label className="form-field">
                          <span>Chỉ số mới</span>
                          <input
                            type="number"
                            min="0"
                            step="0.01"
                            disabled={!line.enabled}
                            value={line.newIndex ?? '0'}
                            onChange={(event) =>
                              updateServiceLine(service.serviceId, {
                                newIndex: event.target.value,
                              })
                            }
                          />
                        </label>
                        <div className="service-line-amount">
                          <span>Tiêu thụ</span>
                          <strong>
                            {qty} {service.unit}
                          </strong>
                          <span>{formatMoney(lineAmount(service.unitPrice, qty))} VND</span>
                        </div>
                      </div>
                      {formErrors[`service_${key}`] ? (
                        <span className="field-error">{formErrors[`service_${key}`]}</span>
                      ) : null}
                    </article>
                  )
                })}
              </div>
            </div>

            <div className="billing-subsection">
              <h3>2. Phí gửi xe (theo xe APPROVED của căn hộ)</h3>
              {parkingService ? (
                <article className="service-line-card">
                  <label className="service-line-check">
                    <input
                      type="checkbox"
                      checked={Boolean(
                        serviceLines[String(parkingService.serviceId)]?.enabled,
                      )}
                      disabled={parkingSummary.quantity <= 0}
                      onChange={(event) =>
                        updateServiceLine(parkingService.serviceId, {
                          enabled: event.target.checked && parkingSummary.quantity > 0,
                        })
                      }
                    />
                    <span>
                      {parkingService.name}
                      <small>
                        Ô tô: {formatMoney(parkingService.unitPrice)} VND/tháng · Xe máy:{' '}
                        {formatMoney(Number(parkingService.unitPrice) / 10)} VND/tháng
                      </small>
                    </span>
                  </label>

                  {!apartmentId ? (
                    <p className="late-fee-hint" style={{ color: '#475569' }}>
                      Chọn căn hộ để tải danh sách xe từ vehicle-service.
                    </p>
                  ) : null}

                  {parkingLoading ? (
                    <p className="late-fee-hint" style={{ color: '#475569' }}>
                      Đang tải xe của căn hộ...
                    </p>
                  ) : null}

                  {parkingError ? <div className="alert alert-error">{parkingError}</div> : null}

                  {apartmentId && !parkingLoading ? (
                    <>
                      <div className="parking-summary-grid">
                        <div>
                          <span>Ô tô APPROVED</span>
                          <strong>{parkingSummary.carCount}</strong>
                          <small>
                            {formatMoney(parkingBreakdown?.carAmount || 0)} VND
                          </small>
                        </div>
                        <div>
                          <span>Xe máy APPROVED</span>
                          <strong>{parkingSummary.motorbikeCount}</strong>
                          <small>
                            {formatMoney(parkingBreakdown?.motoAmount || 0)} VND
                          </small>
                        </div>
                        <div>
                          <span>Tổng phí gửi xe</span>
                          <strong>{formatMoney(parkingBreakdown?.total || 0)} VND</strong>
                          <small>
                            SL quy đổi: {parkingSummary.quantity} slot-xe-hơi
                          </small>
                        </div>
                      </div>

                      {parkingSummary.vehicles.length > 0 ? (
                        <ul className="parking-vehicle-list">
                          {parkingSummary.vehicles.map((vehicle) => (
                            <li key={vehicle.vehicleId}>
                              <strong>{vehicle.type}</strong> · {vehicle.licensePlate}
                              {vehicle.brand ? ` · ${vehicle.brand}` : ''}
                            </li>
                          ))}
                        </ul>
                      ) : (
                        <p className="late-fee-hint" style={{ color: '#475569' }}>
                          Căn hộ chưa có xe APPROVED — phí gửi xe = 0.
                        </p>
                      )}
                    </>
                  ) : null}

                  {formErrors[`service_${parkingService.serviceId}`] ? (
                    <span className="field-error">
                      {formErrors[`service_${parkingService.serviceId}`]}
                    </span>
                  ) : null}
                </article>
              ) : (
                <p className="late-fee-hint" style={{ color: '#475569' }}>
                  Chưa cấu hình dịch vụ Parking Fee trong hệ thống.
                </p>
              )}
            </div>

            <div className="billing-subsection">
              <h3>3. Phí theo kỳ / cố định</h3>
              <div className="service-line-list">
                {fixedServices.map((service) => {
                  const key = String(service.serviceId)
                  const line = serviceLines[key] || {}
                  const qty = quantityFromLine(line)
                  return (
                    <article key={key} className="service-line-card">
                      <label className="service-line-check">
                        <input
                          type="checkbox"
                          checked={Boolean(line.enabled)}
                          onChange={(event) =>
                            updateServiceLine(service.serviceId, {
                              enabled: event.target.checked,
                            })
                          }
                        />
                        <span>
                          {service.name}
                          <small>
                            {formatMoney(service.unitPrice)} VND / {service.unit}
                          </small>
                        </span>
                      </label>
                      <div className="service-line-fields">
                        <label className="form-field">
                          <span>Số lượng ({service.unit})</span>
                          <input
                            type="number"
                            min="0"
                            step="0.01"
                            disabled={!line.enabled}
                            value={line.quantity ?? '1'}
                            onChange={(event) =>
                              updateServiceLine(service.serviceId, {
                                quantity: event.target.value,
                                newIndex: event.target.value,
                              })
                            }
                          />
                        </label>
                        <div className="service-line-amount">
                          <span>Thành tiền</span>
                          <strong>
                            {qty} × {formatMoney(service.unitPrice)}
                          </strong>
                          <span>{formatMoney(lineAmount(service.unitPrice, qty))} VND</span>
                        </div>
                      </div>
                      {formErrors[`service_${key}`] ? (
                        <span className="field-error">{formErrors[`service_${key}`]}</span>
                      ) : null}
                    </article>
                  )
                })}
              </div>
            </div>

            {lateFeeService ? (
              <div className="billing-subsection">
                <h3>4. Phí trễ hạn</h3>
                <article className="service-line-card late-fee-card">
                  <label className="service-line-check">
                    <input
                      type="checkbox"
                      checked={Boolean(serviceLines[String(lateFeeService.serviceId)]?.enabled)}
                      onChange={(event) =>
                        updateServiceLine(lateFeeService.serviceId, {
                          enabled: event.target.checked,
                          quantity: '1',
                          newIndex: '1',
                        })
                      }
                    />
                    <span>
                      {lateFeeService.name}
                      <small>
                        {formatMoney(lateFeeService.unitPrice)} VND (cố định) — áp dụng nếu nộp
                        sau ngày 5
                      </small>
                    </span>
                  </label>
                  {shouldSuggestLateFee ? (
                    <p className="late-fee-hint">
                      Đã quá hạn thanh toán dự kiến ({formatDate(dueDate)}). Hệ thống gợi ý bật
                      Late Fee.
                    </p>
                  ) : (
                    <p className="late-fee-hint">
                      Hạn nộp mặc định: ngày 5 của tháng liền kề sau tháng hóa đơn.
                    </p>
                  )}
                </article>
              </div>
            ) : null}

            <div className="bill-preview-bar">
              <div>
                <span>Tạm tính</span>
                <strong>{formatMoney(previewTotal)} VND</strong>
                {selectedApartment?.roomNumber ? (
                  <small>
                    {selectedApartment.roomNumber}
                    {selectedApartment.area ? ` · ${selectedApartment.area} m²` : ''}
                  </small>
                ) : null}
              </div>
              <button type="submit" className="btn btn-primary" disabled={submitting}>
                {submitting ? 'Đang tạo...' : 'Tạo hóa đơn'}
              </button>
            </div>
          </form>
        </section>
      ) : null}

      <section className="content-card">
        <div className="section-heading">
          <div>
            <h2>Tra cứu &amp; danh sách hóa đơn</h2>
            <p>
              Mỗi hóa đơn hiển thị đầy đủ từng khoản phí, đơn giá, thành tiền và tổng cần thanh
              toán.
            </p>
          </div>
        </div>

        <div className="toolbar-grid">
          {isElevatedRole ? (
            <>
              <label className="form-field">
                <span>Apartment filter</span>
                <select
                  value={filterApartmentId}
                  onChange={(event) => setFilterApartmentId(event.target.value)}
                >
                  <option value="">Tất cả căn hộ</option>
                  {apartments.map((apartment) => (
                    <option key={apartment.apartmentId} value={apartment.apartmentId}>
                      {apartment.roomNumber || apartment.apartmentId}
                    </option>
                  ))}
                </select>
              </label>
              <label className="form-field form-field--month">
                <span>Kỳ thanh toán</span>
                <input
                  type="month"
                  value={filterMonth}
                  onChange={(event) => setFilterMonth(event.target.value)}
                />
              </label>
              <label className="form-field">
                <span>Status</span>
                <select
                  value={filterStatus}
                  onChange={(event) => setFilterStatus(event.target.value)}
                >
                  <option value="">Tất cả</option>
                  <option value="UNPAID">UNPAID</option>
                  <option value="PARTIAL">PARTIAL</option>
                  <option value="PAID">PAID</option>
                </select>
              </label>
            </>
          ) : null}

          <label className="form-field">
            <span>Search</span>
            <input
              placeholder="Tìm theo mã hóa đơn, apartment, status"
              value={searchTerm}
              onChange={(event) => setSearchTerm(event.target.value)}
            />
          </label>

          <div className="toolbar-actions">
            <button type="button" className="btn btn-primary" onClick={loadInvoices}>
              Làm mới danh sách
            </button>
          </div>
        </div>

        {listError ? <div className="alert alert-error">{listError}</div> : null}
        {actionMessage ? <div className="alert alert-success">{actionMessage}</div> : null}

        {loading ? <div className="page-status">Đang tải danh sách hóa đơn...</div> : null}

        {!loading && filteredInvoices.length === 0 ? (
          <div className="empty-state">
            <h3>Chưa có hóa đơn</h3>
            <p>
              {isElevatedRole
                ? 'Chưa có hóa đơn phù hợp bộ lọc, hoặc chưa tạo hóa đơn nào.'
                : 'Căn hộ của bạn chưa có hóa đơn hoặc chưa xác định được căn hộ đang cư trú.'}
            </p>
          </div>
        ) : null}

        {!loading && filteredInvoices.length > 0 ? (
          <div className="invoice-bill-list">
            {filteredInvoices.map((invoice) => {
              const displayStatus = invoice.displayStatus || invoice.status
              const payable = invoice.status !== 'PAID'
              const due = getInvoiceDueDate(invoice.billingMonth)
              const overdue = isPastDue(invoice.billingMonth, invoice.status)
              const expanded = expandedId === invoice.invoiceId
              const focused = focusInvoiceId === invoice.invoiceId
              return (
                <article
                  key={invoice.invoiceId}
                  id={`invoice-card-${invoice.invoiceId}`}
                  className={`invoice-bill-card${focused ? ' invoice-bill-card--focus' : ''}`}
                >
                  <header className="invoice-bill-header">
                    <div>
                      <h3>{invoice.invoiceCode}</h3>
                      <p>
                        {isElevatedRole ? roomLabel(invoice.apartmentId) : 'Căn hộ của bạn'} · Kỳ{' '}
                        {invoice.billingMonth}
                      </p>
                    </div>
                    <div className="invoice-bill-meta">
                      <span className={statusMap[displayStatus] || 'badge'}>{displayStatus}</span>
                      <span className={overdue ? 'due-date due-date--late' : 'due-date'}>
                        Hạn nộp: {formatDate(due)}
                        {overdue ? ' · Quá hạn (có thể tính Late Fee)' : ''}
                      </span>
                    </div>
                  </header>

                  <div className="invoice-bill-summary">
                    <div>
                      <span>Tổng cộng</span>
                      <strong>{formatMoney(invoice.totalAmount)} VND</strong>
                    </div>
                    <div>
                      <span>Đã trả</span>
                      <strong>{formatMoney(invoice.paidAmount)} VND</strong>
                    </div>
                    <div>
                      <span>Còn lại</span>
                      <strong>{formatMoney(invoice.remainingAmount)} VND</strong>
                    </div>
                  </div>

                  <div className="invoice-bill-actions">
                    <button
                      type="button"
                      className="btn btn-secondary"
                      onClick={() =>
                        setExpandedId(expanded ? null : invoice.invoiceId)
                      }
                    >
                      {expanded ? 'Ẩn chi tiết' : 'Xem chi tiết khoản phí'}
                    </button>
                    {payable ? (
                      <>
                        <button
                          type="button"
                          className="btn btn-primary"
                          disabled={submitting}
                          onClick={() => payWithVnPay(invoice)}
                        >
                          VNPay
                        </button>
                        {isElevatedRole ? (
                          <button
                            type="button"
                            className="btn btn-success"
                            onClick={() => openCashModal(invoice)}
                          >
                            Cash
                          </button>
                        ) : null}
                      </>
                    ) : (
                      <span className="table-note">Đã thanh toán đủ</span>
                    )}
                  </div>

                  {expanded ? (
                    <div className="invoice-bill-details">
                      <table className="bill-lines-table">
                        <thead>
                          <tr>
                            <th>Dịch vụ</th>
                            <th>Đơn vị</th>
                            <th>SL</th>
                            <th>Đơn giá</th>
                            <th>Thành tiền</th>
                          </tr>
                        </thead>
                        <tbody>
                          {(invoice.details || []).map((detail) => (
                            <tr key={detail.detailId}>
                              <td>{detail.serviceName}</td>
                              <td>{detail.unit}</td>
                              <td>{detail.quantity}</td>
                              <td>{formatMoney(detail.unitPrice)} VND</td>
                              <td>{formatMoney(detail.amount)} VND</td>
                            </tr>
                          ))}
                        </tbody>
                        <tfoot>
                          <tr>
                            <td colSpan={4}>Tổng hóa đơn</td>
                            <td>{formatMoney(invoice.totalAmount)} VND</td>
                          </tr>
                        </tfoot>
                      </table>
                    </div>
                  ) : null}
                </article>
              )
            })}
          </div>
        ) : null}
      </section>

      {cashModal ? (
        <div className="modal-backdrop" onClick={() => setCashModal(null)}>
          <div className="modal-card" onClick={(event) => event.stopPropagation()}>
            <h3>Thu tiền mặt — {cashModal.invoiceCode}</h3>
            <p>Còn nợ: {formatMoney(cashModal.remainingAmount)} VND</p>
            <form onSubmit={handleCashSubmit}>
              <label className="form-field">
                <span>Payer ID (cư dân)</span>
                <input
                  value={cashModal.payerId}
                  onChange={(event) =>
                    setCashModal((prev) => ({ ...prev, payerId: event.target.value }))
                  }
                  placeholder="UUID người trả tiền"
                />
                {cashErrors.payerId ? <span className="field-error">{cashErrors.payerId}</span> : null}
              </label>
              <label className="form-field">
                <span>Collector ID (nhân viên thu)</span>
                <input
                  value={cashModal.collectorId}
                  onChange={(event) =>
                    setCashModal((prev) => ({ ...prev, collectorId: event.target.value }))
                  }
                />
                {cashErrors.collectorId ? (
                  <span className="field-error">{cashErrors.collectorId}</span>
                ) : null}
              </label>
              <label className="form-field">
                <span>Paid amount</span>
                <input
                  type="number"
                  min="0.01"
                  step="0.01"
                  value={cashModal.paidAmount}
                  onChange={(event) =>
                    setCashModal((prev) => ({ ...prev, paidAmount: event.target.value }))
                  }
                />
                {cashErrors.paidAmount ? (
                  <span className="field-error">{cashErrors.paidAmount}</span>
                ) : null}
              </label>
              <div className="modal-actions">
                <button type="button" className="btn btn-secondary" onClick={() => setCashModal(null)}>
                  Hủy
                </button>
                <button type="submit" className="btn btn-primary" disabled={submitting}>
                  {submitting ? 'Đang lưu...' : 'Ghi nhận'}
                </button>
              </div>
            </form>
          </div>
        </div>
      ) : null}
    </div>
  )
}
