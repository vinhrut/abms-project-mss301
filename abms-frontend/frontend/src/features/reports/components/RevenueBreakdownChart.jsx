export function RevenueBreakdownChart({ items = [] }) {
  const max = Math.max(...items.map((item) => Number(item.amount) || 0), 1)

  if (!items.length) {
    return <div className="report-chart-empty">Chưa có dữ liệu biểu đồ.</div>
  }

  return (
    <div className="report-bar-chart" role="img" aria-label="Revenue breakdown chart">
      {items.map((item) => {
        const height = Math.max(8, Math.round(((Number(item.amount) || 0) / max) * 100))
        return (
          <div key={item.feeType} className="report-bar-chart__item" title={`${item.feeType}: ${item.percentage}%`}>
            <div className="report-bar-chart__bar" style={{ height: `${height}%` }} />
            <span className="report-bar-chart__label">{item.feeType}</span>
          </div>
        )
      })}
    </div>
  )
}
