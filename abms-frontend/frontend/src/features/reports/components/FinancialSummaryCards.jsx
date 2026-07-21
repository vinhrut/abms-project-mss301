import { formatMoneyVnd } from '../utils/aggregateFinancialReport.js'

export function FinancialSummaryCards({ report }) {
  if (!report) return null
  return (
    <section className="report-summary-box">
      <h3>Summary</h3>
      <p>
        <strong>Total Collected:</strong> {formatMoneyVnd(report.totalCollected)}
      </p>
      <p>
        <strong>Pending:</strong> {formatMoneyVnd(report.totalPending)}
      </p>
      <p>
        <strong>Overdue:</strong> {formatMoneyVnd(report.totalOverdue)}
      </p>
    </section>
  )
}
