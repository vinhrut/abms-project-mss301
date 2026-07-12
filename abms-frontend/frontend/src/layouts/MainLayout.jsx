import { Link, NavLink, Outlet } from 'react-router-dom'
import { useAuth } from '../features/auth/context/useAuth.js'

const elevatedRoles = ['STAFF', 'MANAGER']

export function MainLayout() {
  const { auth, logout, isElevatedRole } = useAuth()

  const navigation = [
    { to: '/dashboard', label: 'Dashboard' },
    ...(isElevatedRole ? [{ to: '/residents/approvals', label: 'Duyệt cư dân' }] : []),
    { to: '/vehicles/register', label: 'Đăng ký xe' },
    {
      to: '/vehicles',
      label: isElevatedRole ? 'Duyệt xe' : 'Xe của tôi',
    },
  ]

  return (
    <div className="app-shell">
      <aside className="sidebar">
        <Link to="/dashboard" className="brand-mark">
          <span>AB</span>
          <div>
            <strong>ABMS</strong>
            <small>Apartment Management</small>
          </div>
        </Link>

        <nav className="sidebar-nav">
          {navigation.map((item) => (
            <NavLink
              key={item.to}
              to={item.to}
              className={({ isActive }) =>
                `sidebar-link${isActive ? ' sidebar-link--active' : ''}`
              }
            >
              {item.label}
            </NavLink>
          ))}
        </nav>

        <div className="sidebar-card">
          <span className="eyebrow">Current role</span>
          <strong>{auth?.roleName || 'Unknown'}</strong>
          <p>
            {elevatedRoles.includes(auth?.roleName)
              ? 'Bạn đang ở khu vực quản lý, có thể xử lý các yêu cầu đăng ký xe.'
              : 'Bạn đang ở khu vực cư dân, có thể gửi yêu cầu đăng ký xe mới và xem danh sách xe của mình.'}
          </p>
        </div>
      </aside>

      <div className="main-shell">
        <header className="topbar">
          <div>
            <span className="eyebrow">Welcome back</span>
            <h2>{auth?.email}</h2>
          </div>

          <div className="topbar-actions">
            <div className="user-pill">
              <span className="user-pill__avatar">{auth?.email?.[0]?.toUpperCase() || 'U'}</span>
              <div>
                <strong>{auth?.roleName || 'RESIDENT'}</strong>
                <small>Phiên đăng nhập đang hoạt động</small>
              </div>
            </div>
            <button type="button" className="btn btn-secondary" onClick={logout}>
              Logout
            </button>
          </div>
        </header>

        <main className="page-content">
          <Outlet />
        </main>
      </div>
    </div>
  )
}