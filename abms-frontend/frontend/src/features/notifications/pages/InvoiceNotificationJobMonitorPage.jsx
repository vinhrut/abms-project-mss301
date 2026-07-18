import { useCallback, useEffect, useState } from "react";
import { invoiceNotificationJobService } from "../api/invoiceNotificationJobService";

export default function InvoiceNotificationJobMonitorPage() {
  const [data, setData] = useState({ content: [], totalPages: 0, page: 0 });
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");
  const current = new Date();
  const [period, setPeriod] = useState(`${current.getFullYear()}-${String(current.getMonth() + 1).padStart(2, "0")}`);

  const load = useCallback(async (page = 0) => {
    setLoading(true); setError("");
    try { setData(await invoiceNotificationJobService.getHistory(page, 10)); }
    catch (e) { setError(e?.response?.data?.message || e.message || "Cannot load job history"); }
    finally { setLoading(false); }
  }, []);

  useEffect(() => { load(0); }, [load]);

  async function runNow() {
    setLoading(true); setError("");
    try { await invoiceNotificationJobService.runNow(period); await load(0); }
    catch (e) { setError(e?.response?.data?.message || e.message || "Cannot run job"); setLoading(false); }
  }

  async function retry(id) {
    setLoading(true); setError("");
    try { await invoiceNotificationJobService.retry(id); await load(data.page || 0); }
    catch (e) { setError(e?.response?.data?.message || e.message || "Cannot retry job"); setLoading(false); }
  }

  return <main style={{ padding: 24 }}>
    <h1>Monthly Invoice Notification Jobs</h1>
    <p>Automatic schedule: 08:00 on the first day of every month.</p>
    <div style={{ display: "flex", gap: 12, margin: "16px 0" }}>
      <input type="month" value={period} onChange={(e) => setPeriod(e.target.value)} />
      <button disabled={loading} onClick={runNow}>Run now</button>
      <button disabled={loading} onClick={() => load(data.page || 0)}>Refresh</button>
    </div>
    {error && <div style={{ color: "#b91c1c", marginBottom: 12 }}>{error}</div>}
    <div style={{ overflowX: "auto" }}><table style={{ width: "100%", borderCollapse: "collapse" }}>
      <thead><tr>{["Period","Status","Invoices","Recipients","Sent","Failed","Attempt","Started","Action"].map(h => <th key={h} style={{ textAlign:"left", padding:8, borderBottom:"1px solid #ddd" }}>{h}</th>)}</tr></thead>
      <tbody>{(data.content || []).map(row => <tr key={row.id}>
        <td style={{padding:8}}>{row.billingPeriod}</td><td>{row.status}</td><td>{row.invoiceCount}</td>
        <td>{row.recipientCount}</td><td>{row.sentCount}</td><td>{row.failedCount}</td><td>{row.attemptNumber}</td>
        <td>{row.startedAt ? new Date(row.startedAt).toLocaleString("vi-VN") : "-"}</td>
        <td>{["FAILED","SKIPPED"].includes(row.status) && <button disabled={loading} onClick={() => retry(row.id)}>Retry</button>}</td>
      </tr>)}</tbody>
    </table></div>
    {!loading && !(data.content || []).length && <p>No job runs.</p>}
  </main>;
}
