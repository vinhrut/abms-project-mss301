import { Navigate, Route, Routes } from 'react-router-dom'
import { ProtectedRoute } from './components/ProtectedRoute.jsx'
import { PublicRoute } from './components/PublicRoute.jsx'
import { AuthLayout } from './layouts/AuthLayout.jsx'
import { MainLayout } from './layouts/MainLayout.jsx'
import { LoginPage } from './features/auth/pages/LoginPage.jsx'
import { RegisterPage } from './features/auth/pages/RegisterPage.jsx'
import { ResidentApprovalPage } from './features/auth/pages/ResidentApprovalPage.jsx'
import { DashboardPage } from './features/dashboard/pages/DashboardPage.jsx'
import { HomePage } from './features/home/pages/HomePage.jsx'
import { VehicleListPage } from './features/vehicles/pages/VehicleListPage.jsx'
import { VehicleRegisterPage } from './features/vehicles/pages/VehicleRegisterPage.jsx'
import { NotFoundPage } from './components/NotFoundPage.jsx'

function App() {
  return (
    <Routes>
      <Route path="/" element={<HomePage />} />

      <Route element={<PublicRoute />}>
        <Route element={<AuthLayout />}>
          <Route path="/login" element={<LoginPage />} />
          <Route path="/register" element={<RegisterPage />} />
        </Route>
      </Route>

      <Route element={<ProtectedRoute />}>
        <Route element={<MainLayout />}>
          <Route path="/dashboard" element={<DashboardPage />} />
          <Route path="/residents/approvals" element={<ResidentApprovalPage />} />
          <Route path="/vehicles" element={<VehicleListPage />} />
          <Route path="/vehicles/register" element={<VehicleRegisterPage />} />
        </Route>
      </Route>

      <Route path="/home" element={<Navigate to="/" replace />} />
      <Route path="*" element={<NotFoundPage />} />
    </Routes>
  )
}

export default App