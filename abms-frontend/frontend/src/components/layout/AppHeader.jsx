import { useEffect, useRef, useState } from 'react'
import { Link, useLocation } from 'react-router-dom'
// Bạn có thể xóa import APP_ROUTES nếu không dùng đến
// import { APP_ROUTES } from '../../config/navigation.js'

export function AppHeader({ pageTitle, pageDescription, auth, roleLabel, onLogout }) {
  const location = useLocation()
  const menuRef = useRef(null)
  const [menuOpen, setMenuOpen] = useState(false)

  useEffect(() => {
    setMenuOpen(false)
  }, [location.pathname])

  useEffect(() => {
    const handleClickOutside = (event) => {
      if (menuRef.current && !menuRef.current.contains(event.target)) {
        setMenuOpen(false)
      }
    }

    const handleEscape = (event) => {
      if (event.key === 'Escape') {
        setMenuOpen(false)
      }
    }

    document.addEventListener('mousedown', handleClickOutside)
    document.addEventListener('keydown', handleEscape)

    return () => {
      document.removeEventListener('mousedown', handleClickOutside)
      document.removeEventListener('keydown', handleEscape)
    }
  }, [])

  const closeMenu = () => setMenuOpen(false)

  const handleLogout = () => {
    closeMenu()
    onLogout()
  }

  const avatarLabel = auth?.email?.[0]?.toUpperCase() || 'U'

  return (
    <header className="topbar app-topbar">
      <div className="topbar-title">
        <span className="eyebrow app-topbar__eyebrow">ABMS Workspace</span>
        <div className="app-topbar__title-row">
          <h2>{pageTitle}</h2>
        </div>
        <p className="topbar-description">{pageDescription}</p>
      </div>

      <div className="topbar-actions">
        <div className="topbar-quick-links">
          <Link to="/app/notifications" className="btn btn-ghost">🔔 Thông báo</Link>
          <Link to="/app/profile" className="btn btn-ghost">👤 Hồ sơ</Link>
        </div>
        <div className="user-menu" ref={menuRef}>
          <button
            type="button"
            className="user-pill user-menu__trigger"
            aria-haspopup="menu"
            aria-expanded={menuOpen}
            onClick={() => setMenuOpen((open) => !open)}
          >
            <span className="user-pill__avatar">{avatarLabel}</span>
            <div className="user-menu__summary">
              <strong>{roleLabel}</strong>
              <small>{auth?.email || 'Tài khoản'}</small>
            </div>
            <span className="user-menu__chevron" aria-hidden="true">
              ▾
            </span>
          </button>

          {/* SỬA LỖI Ở ĐÂY: Dùng && thay vì ? và thêm các thẻ đóng div */}
          {menuOpen && (
            <div className="user-menu__dropdown" role="menu">
              <div className="user-menu__header">
                <span className="user-pill__avatar">{avatarLabel}</span>
                <div>
                  <strong>{roleLabel}</strong>
                  <small>{auth?.email || '—'}</small>
                </div>
              </div>

              {/* SỬA LỖI LOGIC: Gọi handleLogout thay vì onLogout */}
              <button 
                type="button" 
                className="btn btn-secondary app-topbar__logout" 
                onClick={handleLogout}
              >
                Đăng xuất
              </button>
            </div>
          )}
        </div>
      </div>
    </header>
  )
}