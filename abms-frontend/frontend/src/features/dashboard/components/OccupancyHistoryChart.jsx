export function OccupancyHistoryChart({ points = [] }) {
  if (!points.length) {
    return <div className="dashboard-chart-empty">No occupancy history data.</div>
  }

  const max = Math.max(...points.map((p) => Number(p.value) || 0), 100)

  return (
    <div className="dashboard-bar-chart" role="img" aria-label="Occupancy Rate History">
      {points.map((point) => {
        const height = Math.max(8, Math.round(((Number(point.value) || 0) / max) * 100))
        return (
          <div
            key={point.label}
            className="dashboard-bar-chart__item"
            title={`${point.label}: ${point.value}%`}
          >
            <span className="dashboard-bar-chart__value">{point.value}%</span>
            <div className="dashboard-bar-chart__bar" style={{ height: `${height}%` }} />
            <span className="dashboard-bar-chart__label">{point.label}</span>
          </div>
        )
      })}
    </div>
  )
}
