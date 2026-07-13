import { Link, NavLink } from 'react-router-dom'

export function AppSidebar({ sections }) {
  return (
    <aside className="app-sidebar">
      <Link to="/app/dashboard" className="brand-mark">
        <span>AB</span>
        <div>
          <strong>ABMS</strong>
          <small>Building Operations Platform</small>
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