import { formatDashboardMoney } from '../utils/aggregateDashboardOverview.js'

export function DashboardSummaryCards({ summary }) {
  const cards = [
    {
      key: 'occupancy',
      title: 'Occupancy Rate',
      value: `${summary.occupancyRate}%`,
    },
    {
      key: 'revenue',
      title: 'Revenue (This Month)',
      value: formatDashboardMoney(summary.revenueThisMonth),
    },
    {
      key: 'pending',
      title: 'Pending Invoices',
      value: String(summary.pendingInvoices),
    },
    {
      key: 'maintenance',
      title: 'Maintenance Req.',
      value: `${summary.openMaintenanceRequests} Open`,
    },
  ]

  return (
    <section className="dashboard-stats-grid" aria-label="Dashboard summary">
      {cards.map((card) => (
        <article key={card.key} className="stat-card dashboard-stat-card">
          <span className="stat-card__title">{card.title}</span>
          <strong>{card.value}</strong>
        </article>
      ))}
    </section>
  )
}
