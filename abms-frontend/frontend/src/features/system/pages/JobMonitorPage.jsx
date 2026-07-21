/* eslint-disable react-hooks/set-state-in-effect */
import { useCallback, useEffect, useMemo, useRef, useState } from 'react'
import { PageIntro } from '../../../components/ui/PageIntro.jsx'
import { invoiceNotificationJobService } from '../../notifications/api/invoiceNotificationJobService.js'
import { extractApiErrorMessage } from '../../../utils/apiError.js'

const SCHEDULED_TIME_LABEL = '01st, 08:00 AM'
const POLL_MS = 15000

const currentPeriod = () => {
  const now = new Date()
  return `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, '0')}`
}

const statusBadge = (status) => (
  <span className={`badge badge-${String(status || '').toLowerCase()}`}>{status || '-'}</span>
)

export function JobMonitorPage() {
  const period = useMemo(() => currentPeriod(), [])
  const [jobs, setJobs] = useState([])
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState('')
  const [message, setMessage] = useState('')
  const autoRunTried = useRef(false)

  const load = useCallback(async ({ silent = false } = {}) => {
    if (!silent) setLoading(true)
    setError('')
    try {
      const response = await invoiceNotificationJobService.getHistory(0, 20)
      setJobs(response.content || [])
      return response.content || []
    } catch (e) {
      setError(extractApiErrorMessage(e, 'Không thể tải lịch sử job.'))
      return []
    } finally {
      if (!silent) setLoading(false)
    }
  }, [])

  const ensureCurrentMonthJob = useCallback(async () => {
    const list = await load()
    const hasCurrent = list.some((job) => job.billingPeriod === period)
    if (hasCurrent || autoRunTried.current) return

    autoRunTried.current = true
    try {
      await invoiceNotificationJobService.runNow(period)
      setMessage(`Đã đồng bộ job tháng ${period}.`)
      await load({ silent: true })
    } catch (e) {
      // Job may already be SUCCESS / RUNNING — keep monitor read-only.
      setError(extractApiErrorMessage(e, 'Không thể đồng bộ job tháng hiện tại.'))
    }
  }, [load, period])

  useEffect(() => {
    ensureCurrentMonthJob()
  }, [ensureCurrentMonthJob])

  useEffect(() => {
    const timer = window.setInterval(() => {
      load({ silent: true })
    }, POLL_MS)
    return () => window.clearInterval(timer)
  }, [load])

  const latestJob = useMemo(
    () => jobs.find((job) => job.billingPeriod === period) || jobs[0] || null,
    [jobs, period],
  )

  const deliveries = latestJob?.deliveries || latestJob?.items || []

  const canRetry =
    latestJob &&
    (['FAILED', 'SKIPPED', 'PARTIAL_SUCCESS'].includes(latestJob.status)
      || Number(latestJob.failedCount) > 0)

  const retry = async () => {
    if (!latestJob?.id) return
    if (!window.confirm('Retry các notification thất bại của job tháng này?')) return
    setLoading(true)
    setMessage('')
    try {
      await invoiceNotificationJobService.retry(latestJob.id)
      setMessage('Đã yêu cầu retry job.')
      await load()
    } catch (e) {
      setError(extractApiErrorMessage(e, 'Không thể retry job.'))
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="page-stack job-monitor-page">
      <PageIntro
        eyebrow="System Job Monitor > Monthly Invoice Notification"
        title="Automatic Invoice Notification"
        description={`Theo dõi job gửi thông báo hóa đơn tháng ${period} (tự chạy / cập nhật định kỳ).`}
      />

      <section className="content-card">
        {error && <div className="alert alert-error">{error}</div>}
        {message && <div className="alert alert-success">{message}</div>}
        {loading && <div className="page-status">Đang tải...</div>}

        <div className="job-monitor-summary">
          <article className="info-card">
            <strong>Scheduled Time</strong>
            <p>{SCHEDULED_TIME_LABEL}</p>
          </article>
          <article className="info-card">
            <strong>Total Recipients</strong>
            <p>{latestJob ? `${latestJob.recipientCount ?? 0} Residents` : '—'}</p>
          </article>
          <article className="info-card">
            <strong>Sent Successfully</strong>
            <p className="job-monitor-metric job-monitor-metric--success">
              {latestJob ? latestJob.sentCount ?? 0 : '—'}
            </p>
          </article>
          <article className="info-card">
            <strong>Failed (Retry)</strong>
            <p className="job-monitor-metric job-monitor-metric--danger">
              {latestJob ? latestJob.failedCount ?? 0 : '—'}
            </p>
            {canRetry && (
              <button
                className="btn btn-danger job-monitor-retry-btn"
                type="button"
                disabled={loading}
                onClick={retry}
              >
                Retry failed
              </button>
            )}
          </article>
        </div>

        <div className="table-card notification-table-card">
          <table className="data-table job-monitor-table">
            <thead>
              <tr>
                <th>Resident</th>
                <th>Invoice ID</th>
                <th>Channel</th>
                <th>Status</th>
                <th>Attempt</th>
              </tr>
            </thead>
            <tbody>
              {!loading && deliveries.length === 0 && (
                <tr>
                  <td colSpan={5} className="job-monitor-empty-cell">
                    {latestJob
                      ? 'Chưa có chi tiết giao nhận cho kỳ này.'
                      : `Đang chờ job tháng ${period}...`}
                  </td>
                </tr>
              )}
              {deliveries.map((item, index) => (
                <tr key={item.id || `${item.invoiceId}-${item.channel}-${index}`}>
                  <td>{item.residentName || item.residentId || item.userId || '-'}</td>
                  <td>{item.invoiceId || '-'}</td>
                  <td>{item.channel || '-'}</td>
                  <td>{statusBadge(item.status)}</td>
                  <td>{item.attempt ?? item.attemptNumber ?? '-'}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </section>
    </div>
  )
}
