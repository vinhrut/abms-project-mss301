import { Navigate, Route, Routes } from 'react-router-dom'
import { ProtectedRoute } from './components/ProtectedRoute.jsx'
import { PublicRoute } from './components/PublicRoute.jsx'
import { APP_ROUTES } from './config/navigation.js'
import { ApartmentListPage } from './features/apartments/pages/ApartmentListPage.jsx'
import { AuthLayout } from './layouts/AuthLayout.jsx'
import { MainLayout } from './layouts/MainLayout.jsx'
import { LoginPage } from './features/auth/pages/LoginPage.jsx'
import { RegisterPage } from './features/auth/pages/RegisterPage.jsx'
import { ResidentApprovalPage } from './features/auth/pages/ResidentApprovalPage.jsx'
import { InvoiceListPage } from './features/billing/pages/InvoiceListPage.jsx'
import { PaymentListPage } from './features/billing/pages/PaymentListPage.jsx'
import { VnPayResultPage } from './features/billing/pages/VnPayResultPage.jsx'
import { BuildingListPage } from './features/buildings/pages/BuildingListPage.jsx'
import { ContractListPage } from './features/contracts/pages/ContractListPage.jsx'
import { DashboardPage } from './features/dashboard/pages/DashboardPage.jsx'
import { ChangePasswordPage } from './features/profile/pages/ChangePasswordPage.jsx'
import { ProfilePage } from './features/profile/pages/ProfilePage.jsx'
import { HomePage } from './features/home/pages/HomePage.jsx'
import { MaintenanceListPage } from './features/maintenance/pages/MaintenanceListPage.jsx'
import { MaintenanceDetailPage } from './features/maintenance/pages/MaintenanceDetailPage.jsx'
import { MaintenanceSubmitPage } from './features/maintenance/pages/MaintenanceSubmitPage.jsx'
import { MyTasksPage } from './features/maintenance/pages/MyTasksPage.jsx'
import { NotificationListPage } from './features/notifications/pages/NotificationListPage.jsx'
import { ReportListPage } from './features/reports/pages/ReportListPage.jsx'
import { ResidentListPage } from './features/residents/pages/ResidentListPage.jsx'
import { JobMonitorPage } from './features/system/pages/JobMonitorPage.jsx'
import { UserManagementPage } from './features/users/pages/UserManagementPage.jsx'
import { VehicleListPage } from './features/vehicles/pages/VehicleListPage.jsx'
import { VehicleRegisterPage } from './features/vehicles/pages/VehicleRegisterPage.jsx'
import { NotFoundPage } from './components/NotFoundPage.jsx'

function App() {
  return (
    <Routes>
      <Route path={APP_ROUTES.home} element={<HomePage />} />

      <Route element={<PublicRoute />}>
        <Route element={<AuthLayout />}>
          <Route path={APP_ROUTES.login} element={<LoginPage />} />
          <Route path={APP_ROUTES.register} element={<RegisterPage />} />
        </Route>
      </Route>

      <Route element={<ProtectedRoute />}>
        <Route element={<MainLayout />}>
          <Route path={APP_ROUTES.dashboard} element={<DashboardPage />} />
          <Route path={APP_ROUTES.buildings} element={<BuildingListPage />} />
          <Route path={APP_ROUTES.users} element={<UserManagementPage />} />
          <Route path={APP_ROUTES.apartments} element={<ApartmentListPage />} />
          <Route path={APP_ROUTES.residents} element={<ResidentListPage />} />
          <Route path={APP_ROUTES.residentApprovals} element={<ResidentApprovalPage />} />
          <Route path={APP_ROUTES.contracts} element={<ContractListPage />} />
          <Route path={APP_ROUTES.invoices} element={<InvoiceListPage />} />
          <Route path={APP_ROUTES.payments} element={<PaymentListPage />} />
          <Route path={APP_ROUTES.vnpayResult} element={<VnPayResultPage />} />
          <Route path={APP_ROUTES.maintenance} element={<MaintenanceListPage />} />
          <Route path="/app/maintenance/:requestId" element={<MaintenanceDetailPage />} />
          <Route path={APP_ROUTES.maintenanceSubmit} element={<MaintenanceSubmitPage />} />
          <Route path={APP_ROUTES.maintenanceTasks} element={<MyTasksPage />} />
          <Route path={APP_ROUTES.vehicles} element={<VehicleListPage />} />
          <Route path={APP_ROUTES.vehicleRegister} element={<VehicleRegisterPage />} />
          <Route path={APP_ROUTES.notifications} element={<NotificationListPage />} />
          <Route path={APP_ROUTES.reports} element={<ReportListPage />} />
          <Route path={APP_ROUTES.jobs} element={<JobMonitorPage />} />
          <Route path={APP_ROUTES.profile} element={<ProfilePage />} />
          <Route path={APP_ROUTES.changePassword} element={<ChangePasswordPage />} />
        </Route>
      </Route>

      <Route path="/dashboard" element={<Navigate to={APP_ROUTES.dashboard} replace />} />
      <Route path="/residents/approvals" element={<Navigate to={APP_ROUTES.residentApprovals} replace />} />
      <Route path="/vehicles" element={<Navigate to={APP_ROUTES.vehicles} replace />} />
      <Route path="/vehicles/register" element={<Navigate to={APP_ROUTES.vehicleRegister} replace />} />
      <Route path="/home" element={<Navigate to="/" replace />} />
      <Route path="*" element={<NotFoundPage />} />
    </Routes>
  )
}

export default App