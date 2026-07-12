import { Navigate, Outlet, useLocation } from 'react-router-dom'
import { useAuth } from '../features/auth/context/useAuth.js'

export function ProtectedRoute() {
  const { isAuthenticated, isHydrated } = useAuth()
  const location = useLocation()

  if (!isHydrated) {
    return <div className="page-status">Đang khởi tạo phiên đăng nhập...</div>
  }

  if (!isAuthenticated) {
    return <Navigate to="/login" replace state={{ from: location }} />
  }

  return <Outlet />
}