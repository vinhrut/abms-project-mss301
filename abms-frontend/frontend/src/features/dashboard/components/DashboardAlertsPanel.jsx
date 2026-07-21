export function DashboardAlertsPanel({ alerts = [] }) {
  return (
    <section className="content-card dashboard-alerts-card" aria-label="Alerts">
      <div className="section-heading">
        <h3>Alerts</h3>
      </div>

      {!alerts.length ? (
        <p className="dashboard-alerts-empty">No urgent items at the moment.</p>
      ) : (
        <ul className="dashboard-alerts-list">
          {alerts.map((alert) => (
            <li key={alert.id} data-alert-type={alert.type}>
              {alert.text}
            </li>
          ))}
        </ul>
      )}
    </section>
  )
}
