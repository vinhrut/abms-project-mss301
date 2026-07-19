import { Navigate } from 'react-router-dom'
import { APP_ROUTES } from '../../../config/navigation.js'

export function VehicleRegisterPage() {
  return <Navigate to={APP_ROUTES.vehicles} replace />
}