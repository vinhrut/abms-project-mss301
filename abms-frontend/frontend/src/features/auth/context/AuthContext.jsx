import { useCallback, useEffect, useMemo, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { clearStoredAuth, getStoredAuth, setStoredAuth } from '../../../utils/storage.js'
import { authService } from '../../../services/authService.js'
import { setAuthToken } from '../../../services/apiClient.js'
import { AuthContext } from './authContextInstance.js'
import { getDefaultPrivateRoute } from '../../../config/navigation.js'
import { getRoleLabel, isElevatedRole, normalizeRole } from '../../../config/roles.js'

export function AuthProvider({ children }) {
  const [auth, setAuth] = useState(() => getStoredAuth())
  const [isHydrated] = useState(true)
  const navigate = useNavigate()

  useEffect(() => {
    if (auth?.token) {
      setAuthToken(auth.token)
    } else {
      setAuthToken(null)
    }
  }, [auth])

  const handleAuthSuccess = (payload) => {
    const normalizedPayload = {
      ...payload,
      roleName: normalizeRole(payload.roleName),
    }

    setAuth(normalizedPayload)
    setStoredAuth(normalizedPayload)
    setAuthToken(normalizedPayload.token)
    navigate(getDefaultPrivateRoute(normalizedPayload.roleName), { replace: true })
  }

  const login = useCallback(async (formData) => {
    const response = await authService.login(formData)
    handleAuthSuccess(response)
    return response
  }, [])

  const logout = useCallback(() => {
    setAuth(null)
    clearStoredAuth()
    setAuthToken(null)
    navigate('/login', { replace: true })
  }, [navigate])

  const elevated = isElevatedRole(auth?.roleName)
  const defaultPrivateRoute = getDefaultPrivateRoute(auth?.roleName)
  const roleLabel = getRoleLabel(auth?.roleName)

  const value = useMemo(
    () => ({
      auth,
      isHydrated,
      isAuthenticated: Boolean(auth?.token),
      isElevatedRole: elevated,
      normalizedRole: normalizeRole(auth?.roleName),
      roleLabel,
      defaultPrivateRoute,
      login,
      logout,
    }),
    [auth, defaultPrivateRoute, elevated, isHydrated, login, logout, roleLabel],
  )

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}