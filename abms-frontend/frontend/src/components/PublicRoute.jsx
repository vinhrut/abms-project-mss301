import { Navigate, Outlet } from 'react-router-dom'
import { useAuth } from '../features/auth/context/useAuth.js'

export function PublicRoute() {
  const { isAuthenticated, isHydrated, defaultPrivateRoute } = useAuth()

  if (!isHydrated) {
    return <div className="page-status">Đang kiểm tra phiên đăng nhập...</div>
  }

  if (isAuthenticated) {
    return <Navigate to={defaultPrivateRoute} replace />
  }

  return <Outlet />
}