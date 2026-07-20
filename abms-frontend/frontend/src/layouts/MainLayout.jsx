import { Outlet, useLocation } from 'react-router-dom'
import { AppFooter } from '../components/layout/AppFooter.jsx'
import { AppHeader } from '../components/layout/AppHeader.jsx'
import { AppSidebar } from '../components/layout/AppSidebar.jsx'
import { getNavigationForRole } from '../config/navigation.js'
import { useAuth } from '../features/auth/context/useAuth.js'

export function MainLayout() {
  const { auth, logout, roleLabel } = useAuth()
  const location = useLocation()

  const sections = getNavigationForRole(auth?.roleName)
  const activeItem = sections.flatMap((section) => section.items).find((item) => location.pathname.startsWith(item.to))
  const pageTitle = activeItem?.label || 'Không gian làm việc'
  const pageDescription = 'Theo dõi và xử lý nghiệp vụ quản lý tòa nhà theo quyền truy cập hiện tại.'

  return (
    <div className="app-shell">
      <AppSidebar sections={sections} />

      <div className="main-shell">
        <AppHeader
          pageTitle={pageTitle}
          pageDescription={pageDescription}
          auth={auth}
          roleLabel={roleLabel}
          onLogout={logout}
        />

        <main className="page-content">
          <Outlet />
        </main>

        <AppFooter />
      </div>
    </div>
  )
}