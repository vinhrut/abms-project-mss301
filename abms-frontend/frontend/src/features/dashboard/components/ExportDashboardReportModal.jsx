import { useState } from 'react'
import { exportDashboardReport } from '../utils/exportDashboardReport.js'

function todayIsoDate() {
  return new Date().toISOString().slice(0, 10)
}

function startOfMonthIsoDate() {
  const now = new Date()
  return `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, '0')}-01`
}

/**
 * UC-DASH-02 Export Dashboard Report modal
 */
export function ExportDashboardReportModal({ open, overview, onClose }) {
  const [format, setFormat] = useState('PDF')
  const [fromDate, setFromDate] = useState(startOfMonthIsoDate)
  const [toDate, setToDate] = useState(todayIsoDate)
  const [errors, setErrors] = useState({})
  const [exporting, setExporting] = useState(false)
  const [message, setMessage] = useState('')

  if (!open) return null

  const validate = () => {
    const next = {}
    if (!fromDate) next.fromDate = 'From Date is required.'
    if (!toDate) next.toDate = 'To Date is required.'
    if (fromDate && toDate && fromDate > toDate) {
      next.toDate = 'To Date must be on or after From Date.'
    }
    if (!format) next.format = 'Select a format.'
    return next
  }

  const handleExport = async (event) => {
    event.preventDefault()
    const next = validate()
    setErrors(next)
    setMessage('')
    if (Object.keys(next).length) return

    try {
      setExporting(true)
      await exportDashboardReport({
        overview,
        format,
        fromDate,
        toDate,
      })
      setMessage(
        format === 'EXCEL'
          ? 'Report exported as Excel-compatible CSV.'
          : 'Report opened for PDF print / save.',
      )
    } catch {
      setErrors({ form: 'Unable to export the dashboard report. Please try again.' })
    } finally {
      setExporting(false)
    }
  }

  return (
    <div className="modal-backdrop" role="presentation" onClick={onClose}>
      <section
        className="modal-card export-dashboard-modal"
        role="dialog"
        aria-modal="true"
        aria-labelledby="export-dashboard-title"
        onClick={(event) => event.stopPropagation()}
      >
        <h2 id="export-dashboard-title">Export Dashboard Report</h2>

        <form className="export-dashboard-form" onSubmit={handleExport} noValidate>
          <fieldset className="form-field announcement-fieldset">
            <legend>Format</legend>
            <div className="choice-row" role="radiogroup" aria-label="Format">
              <label className="choice-option">
                <input
                  type="radio"
                  name="format"
                  value="PDF"
                  checked={format === 'PDF'}
                  onChange={() => setFormat('PDF')}
                />
                PDF
              </label>
              <label className="choice-option">
                <input
                  type="radio"
                  name="format"
                  value="EXCEL"
                  checked={format === 'EXCEL'}
                  onChange={() => setFormat('EXCEL')}
                />
                Excel (.xlsx)
              </label>
            </div>
            {errors.format ? <small className="field-error">{errors.format}</small> : null}
          </fieldset>

          <div className="export-dashboard-dates">
            <label className="form-field">
              <span>From Date</span>
              <input
                type="date"
                value={fromDate}
                onChange={(e) => {
                  setFromDate(e.target.value)
                  setErrors((old) => ({ ...old, fromDate: '', toDate: '' }))
                }}
                aria-invalid={Boolean(errors.fromDate)}
              />
              {errors.fromDate ? <small className="field-error">{errors.fromDate}</small> : null}
            </label>

            <label className="form-field">
              <span>To Date</span>
              <input
                type="date"
                value={toDate}
                onChange={(e) => {
                  setToDate(e.target.value)
                  setErrors((old) => ({ ...old, fromDate: '', toDate: '' }))
                }}
                aria-invalid={Boolean(errors.toDate)}
              />
              {errors.toDate ? <small className="field-error">{errors.toDate}</small> : null}
            </label>
          </div>

          <p className="export-dashboard-note">
            Note: If report generation exceeds 10 seconds, the report will be emailed to you upon
            completion.
          </p>

          {errors.form ? <div className="alert alert-error">{errors.form}</div> : null}
          {message ? <div className="alert alert-success">{message}</div> : null}

          <div className="modal-actions export-dashboard-actions">
            <button type="button" className="btn btn-secondary" onClick={onClose} disabled={exporting}>
              Cancel
            </button>
            <button type="submit" className="btn btn-primary" disabled={exporting}>
              {exporting ? 'Exporting...' : 'Export'}
            </button>
          </div>
        </form>
      </section>
    </div>
  )
}
