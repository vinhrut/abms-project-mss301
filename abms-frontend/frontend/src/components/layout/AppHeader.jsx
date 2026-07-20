import { Link, useLocation } from 'react-router-dom'

export function AppHeader({ pageTitle, pageDescription, auth, roleLabel, onLogout }) {
  const location = useLocation()

  return (
    <header className="topbar app-topbar">
      <div className="topbar-title">
        <span className="eyebrow">Hệ thống quản lý tòa nhà</span>
        <h2>{pageTitle}</h2>
        <p className="topbar-description">{pageDescription}</p>
      </div>

      <div className="topbar-actions">
        <div className="topbar-quick-links">
          <Link to="/app/notifications" className="btn btn-ghost">Thông báo</Link>
          <Link to="/app/profile" className="btn btn-ghost">Hồ sơ</Link>
        </div>

        <div className="user-pill">
          <span className="user-pill__avatar">{auth?.email?.[0]?.toUpperCase() || 'U'}</span>
          <div>
            <strong>{roleLabel}</strong>
            <small>{auth?.email || location.pathname}</small>
          </div>
        </div>

        <button type="button" className="btn btn-secondary" onClick={onLogout}>Đăng xuất</button>
      </div>
    </header>
  )
}