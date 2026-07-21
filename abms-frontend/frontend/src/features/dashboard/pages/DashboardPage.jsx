/* eslint-disable react-hooks/set-state-in-effect */
import { useCallback, useEffect, useState } from 'react'
import { Navigate } from 'react-router-dom'
import { PageIntro } from '../../../components/ui/PageIntro.jsx'
import { getDefaultPrivateRoute } from '../../../config/navigation.js'
import { isRoleIn, ROLE_KEYS } from '../../../config/roles.js'
import { extractApiErrorMessage } from '../../../utils/apiError.js'
import { useAuth } from '../../auth/context/useAuth.js'
import { dashboardOverviewService } from '../api/dashboardOverviewService.js'
import { DashboardAlertsPanel } from '../components/DashboardAlertsPanel.jsx'
import { DashboardSummaryCards } from '../components/DashboardSummaryCards.jsx'
import { ExportDashboardReportModal } from '../components/ExportDashboardReportModal.jsx'
import { MonthlyRevenueTrendChart } from '../components/MonthlyRevenueTrendChart.jsx'
import { OccupancyHistoryChart } from '../components/OccupancyHistoryChart.jsx'
import { emptyDashboardOverview } from '../utils/aggregateDashboardOverview.js'

function getBuildingIdFromToken(token) {
  try {
    const [, payload] = token.split('.')
    return JSON.parse(window.atob(payload.replace(/-/g, '+').replace(/_/g, '/'))).buildingId || ''
  } catch {
    return ''
  }
}

/**
 * UC-DASH-01 View Dashboard + UC-DASH-02 Export Dashboard Report
 * Data: compose existing apartment / finance / maintenance / contract APIs on FE.
 */
export function DashboardPage() {
  const { auth } = useAuth()
  const [exportOpen, setExportOpen] = useState(false)
  const [overview, setOverview] = useState(emptyDashboardOverview)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')

  const canViewDashboard = isRoleIn(auth?.roleName, [ROLE_KEYS.ADMIN, ROLE_KEYS.MANAGER])
  const isManager = isRoleIn(auth?.roleName, [ROLE_KEYS.MANAGER])
  const buildingId = isManager
    ? auth?.buildingId || getBuildingIdFromToken(auth?.token || '')
    : ''

  const load = useCallback(async () => {
    setLoading(true)
    setError('')
    try {
      const data = await dashboardOverviewService.loadOverview({
        buildingId: buildingId || undefined,
      })
      setOverview(data)
    } catch (apiError) {
      setOverview(emptyDashboardOverview())
      setError(extractApiErrorMessage(apiError, 'Không thể tải dữ liệu dashboard.'))
    } finally {
      setLoading(false)
    }
  }, [buildingId])

  useEffect(() => {
    if (!canViewDashboard) return
    load()
  }, [canViewDashboard, load])

  if (!canViewDashboard) {
    return <Navigate to={getDefaultPrivateRoute(auth?.roleName)} replace />
  }

  const { summary, monthlyRevenueTrend, occupancyHistory, alerts } = overview

  return (
    <div className="page-stack dashboard-page">
      <PageIntro
        title="Dashboard"
        actions={
          <button type="button" className="btn btn-primary" onClick={() => setExportOpen(true)} disabled={loading}>
            Export Report
          </button>
        }
      />

      {error && <div className="alert alert-error">{error}</div>}
      {loading && <div className="page-status">Đang tải dashboard...</div>}

      {!loading && (
        <>
          <DashboardSummaryCards summary={summary} />

          <section className="dashboard-charts-grid" aria-label="Dashboard charts">
            <article className="content-card dashboard-chart-card">
              <div className="section-heading">
                <h3>Monthly Revenue Trend</h3>
              </div>
              <MonthlyRevenueTrendChart points={monthlyRevenueTrend} />
            </article>

            <article className="content-card dashboard-chart-card">
              <div className="section-heading">
                <h3>Occupancy Rate History</h3>
              </div>
              <OccupancyHistoryChart points={occupancyHistory} />
            </article>
          </section>

          <DashboardAlertsPanel alerts={alerts} />
        </>
      )}

      <ExportDashboardReportModal
        open={exportOpen}
        overview={overview}
        onClose={() => setExportOpen(false)}
      />
    </div>
  )
}
