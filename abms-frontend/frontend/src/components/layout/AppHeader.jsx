import { useEffect, useRef, useState } from 'react'
import { Link, useLocation } from 'react-router-dom'
import { APP_ROUTES } from '../../config/navigation.js'

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
      <div>
        <span className="eyebrow">Apartment Building Management</span>
        <h2>{pageTitle}</h2>
        <p className="topbar-description">{pageDescription}</p>
      </div>

      <div className="topbar-actions">
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

          {menuOpen ? (
            <div className="user-menu__dropdown" role="menu">
              <div className="user-menu__header">
                <span className="user-pill__avatar">{avatarLabel}</span>
                <div>
                  <strong>{roleLabel}</strong>
                  <small>{auth?.email || '—'}</small>
                </div>
              </div>

              <Link
                to={APP_ROUTES.notifications}
                className="user-menu__item"
                role="menuitem"
                onClick={closeMenu}
              >
                Notifications
              </Link>
              <Link
                to={APP_ROUTES.profile}
                className="user-menu__item"
                role="menuitem"
                onClick={closeMenu}
              >
                Profile
              </Link>
              <button
                type="button"
                className="user-menu__item user-menu__item--danger"
                role="menuitem"
                onClick={handleLogout}
              >
                Logout
              </button>
            </div>
          ) : null}
        </div>
      </div>
    </header>
  )
}
