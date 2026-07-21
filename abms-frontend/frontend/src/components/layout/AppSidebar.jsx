import { Link, NavLink } from 'react-router-dom'

export function AppSidebar({ sections }) {
  return (
    <aside className="app-sidebar">
      <Link to="/app/dashboard" className="brand-mark">
        <span className="brand-mark__logo" aria-hidden="true">
          <svg viewBox="0 0 48 48" focusable="false">
            <path d="M12 42V16.5L24 7l12 9.5V42" />
            <path d="M18 42V24h12v18" />
            <path d="M17 18h2M23 18h2M29 18h2M17 24h2M29 24h2M17 30h2M29 30h2" />
          </svg>
        </span>
        <div>
          <strong>ABMS</strong>
        </div>
      </Link>

      <div className="sidebar-sections">
        {sections.map((section) => (
          <section key={section.id} className="sidebar-group">
            <p className="sidebar-group__title">{section.title}</p>
            <nav className="sidebar-nav">
              {section.items.map((item) => (
                <NavLink
                  key={item.to}
                  to={item.to}
                  className={({ isActive }) => `sidebar-link${isActive ? ' sidebar-link--active' : ''}`}
                >
                  {item.label}
                </NavLink>
              ))}
            </nav>
          </section>
        ))}
      </div>
    </aside>
  )
}