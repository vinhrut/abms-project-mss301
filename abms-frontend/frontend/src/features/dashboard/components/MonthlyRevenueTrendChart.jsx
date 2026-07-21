import {
  Bar,
  BarChart,
  CartesianGrid,
  ResponsiveContainer,
  Tooltip,
  XAxis,
  YAxis,
} from 'recharts'
import { formatDashboardMoney } from '../utils/aggregateDashboardOverview.js'

function formatAxisValue(value) {
  const amount = Number(value) || 0
  if (amount >= 1_000_000_000) return `${(amount / 1_000_000_000).toFixed(1)}B`
  if (amount >= 1_000_000) return `${Math.round(amount / 1_000_000)}M`
  if (amount >= 1_000) return `${Math.round(amount / 1_000)}K`
  return String(amount)
}

function RevenueTooltip({ active, payload, label }) {
  if (!active || !payload?.length) return null

  return (
    <div className="dashboard-chart-tooltip">
      <strong>{label}</strong>
      <span>{formatDashboardMoney(payload[0].value)}</span>
    </div>
  )
}

/**
 * Monthly revenue trend — Recharts bar chart.
 */
export function MonthlyRevenueTrendChart({ points = [] }) {
  if (!points.length) {
    return <div className="dashboard-chart-empty">No revenue trend data.</div>
  }

  const data = points.map((point) => ({
    label: point.label,
    value: Number(point.value) || 0,
  }))

  return (
    <div className="dashboard-recharts" role="img" aria-label="Monthly Revenue Trend">
      <ResponsiveContainer width="100%" height={240}>
        <BarChart data={data} margin={{ top: 8, right: 12, bottom: 4, left: 4 }}>
          <CartesianGrid stroke="var(--border)" strokeDasharray="4 4" vertical={false} />
          <XAxis
            dataKey="label"
            tick={{ fill: 'var(--text-muted)', fontSize: 12 }}
            axisLine={{ stroke: 'var(--border)' }}
            tickLine={false}
          />
          <YAxis
            tickFormatter={formatAxisValue}
            width={48}
            tick={{ fill: 'var(--text-muted)', fontSize: 12 }}
            axisLine={false}
            tickLine={false}
          />
          <Tooltip content={<RevenueTooltip />} cursor={{ fill: 'rgba(36, 87, 245, 0.06)' }} />
          <Bar
            dataKey="value"
            name="Revenue"
            fill="var(--primary)"
            radius={[8, 8, 4, 4]}
            maxBarSize={48}
          />
        </BarChart>
      </ResponsiveContainer>
    </div>
  )
}
