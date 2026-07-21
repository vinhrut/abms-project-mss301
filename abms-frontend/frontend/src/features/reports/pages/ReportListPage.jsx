/* eslint-disable react-hooks/set-state-in-effect */
import { useState } from 'react'
import { PageIntro } from '../../../components/ui/PageIntro.jsx'
import { FinancialSummaryCards } from '../components/FinancialSummaryCards.jsx'
import { RevenueBreakdownChart } from '../components/RevenueBreakdownChart.jsx'
import { financialReportService } from '../api/financialReportService.js'
import { extractApiErrorMessage } from '../../../utils/apiError.js'
import { formatMoneyVnd } from '../utils/aggregateFinancialReport.js'

const REPORT_TYPES = [{ value: 'MONTHLY_REVENUE', label: 'Monthly Revenue' }]

export function ReportListPage() {
  const now = new Date()
  const [reportType, setReportType] = useState('MONTHLY_REVENUE')
  const [period, setPeriod] = useState(
    `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, '0')}`,
  )
  const [format, setFormat] = useState('PDF')
  const [report, setReport] = useState(null)
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState('')

  const generate = async () => {
    const [year, month] = period.split('-').map(Number)
    if (!year || !month) {
      setError('Chọn Period (Month/Year) hợp lệ.')
      return
    }
    setLoading(true)
    setError('')
    try {
      const data = await financialReportService.preview(month, year)
      setReport(data)
    } catch (e) {
      setReport(null)
      setError(extractApiErrorMessage(e, 'Không thể tổng hợp báo cáo từ hóa đơn.'))
    } finally {
      setLoading(false)
    }
  }

  const download = async () => {
    if (!report) return
    setLoading(true)
    setError('')
    try {
      await financialReportService.exportReport(report, format)
    } catch (e) {
      setError(extractApiErrorMessage(e, 'Không thể tải báo cáo.'))
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="page-stack report-page">
      <PageIntro
        eyebrow="Report > Financial Reports"
        title="Generate Financial Report"
        description="Tổng hợp doanh thu theo tháng từ hóa đơn (finance-service), không lưu DB."
      />

      <section className="content-card">
        <div className="report-filter-bar">
          <label className="form-field">
            <span>Report Type</span>
            <select
              aria-label="Report Type"
              value={reportType}
              onChange={(e) => setReportType(e.target.value)}
            >
              {REPORT_TYPES.map((item) => (
                <option key={item.value} value={item.value}>
                  {item.label}
                </option>
              ))}
            </select>
          </label>

          <label className="form-field">
            <span>Period (Month/Year)</span>
            <input
              type="month"
              aria-label="Period"
              value={period}
              onChange={(e) => setPeriod(e.target.value)}
            />
          </label>

          <label className="form-field">
            <span>Format</span>
            <select
              aria-label="Format"
              value={format}
              onChange={(e) => setFormat(e.target.value)}
            >
              <option value="PDF">PDF</option>
              <option value="EXCEL">Excel</option>
            </select>
          </label>

          <div className="report-filter-bar__action">
            <button className="btn btn-primary" type="button" disabled={loading} onClick={generate}>
              Generate
            </button>
          </div>
        </div>

        {error && <div className="alert alert-error">{error}</div>}
        {loading && <div className="page-status">Đang tổng hợp...</div>}

        {!loading && !report && (
          <div className="empty-state">
            <h3>Chưa có báo cáo</h3>
            <p>Chọn kỳ và bấm Generate để tổng hợp từ danh sách hóa đơn.</p>
          </div>
        )}

        {report && (
          <>
            <div className="report-split">
              <section className="report-panel">
                <h3>Revenue Breakdown by Fee Type (Chart)</h3>
                <RevenueBreakdownChart items={report.revenueBreakdown} />
              </section>

              <section className="report-panel">
                <h3>Fee Type</h3>
                <div className="table-card notification-table-card">
                  <table className="data-table report-fee-table">
                    <thead>
                      <tr>
                        <th>Fee Type</th>
                        <th>Amount</th>
                        <th>% of Total</th>
                      </tr>
                    </thead>
                    <tbody>
                      {report.revenueBreakdown.length === 0 && (
                        <tr>
                          <td colSpan={3} className="job-monitor-empty-cell">
                            Không có chi tiết phí trong kỳ này.
                          </td>
                        </tr>
                      )}
                      {report.revenueBreakdown.map((row) => (
                        <tr key={row.feeType}>
                          <td>{row.feeType}</td>
                          <td>{formatMoneyVnd(row.amount)}</td>
                          <td>{row.percentage}%</td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
              </section>
            </div>

            <FinancialSummaryCards report={report} />

            <div className="report-download-row">
              <button className="btn btn-primary" type="button" disabled={loading} onClick={download}>
                Download
              </button>
            </div>
          </>
        )}
      </section>
    </div>
  )
}
