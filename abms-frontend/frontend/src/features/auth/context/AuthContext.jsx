import { useCallback, useEffect, useMemo, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { clearStoredAuth, getStoredAuth, setStoredAuth } from '../../../utils/storage.js'
import { authService } from '../../../services/authService.js'
import { setAuthToken } from '../../../services/apiClient.js'
import { AuthContext } from './authContextInstance.js'

const elevatedRoles = ['STAFF', 'MANAGER']

function getDefaultRoute(roleName) {
  if (elevatedRoles.includes(roleName)) {
    return '/vehicles'
  }

  return '/vehicles/register'
}

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
    setAuth(payload)
    setStoredAuth(payload)
    setAuthToken(payload.token)
    navigate(getDefaultRoute(payload.roleName), { replace: true })
  }

  const login = useCallback(async (formData) => {
    const response = await authService.login(formData)
    handleAuthSuccess(response)
    return response
  }, [])

  const register = useCallback(async (formData) => {
    const response = await authService.register(formData)
    return response
  }, [])

  const logout = useCallback(() => {
    setAuth(null)
    clearStoredAuth()
    setAuthToken(null)
    navigate('/login', { replace: true })
  }, [navigate])

  const isElevatedRole = elevatedRoles.includes(auth?.roleName)
  const defaultPrivateRoute = getDefaultRoute(auth?.roleName)

  const value = useMemo(
    () => ({
      auth,
      isHydrated,
      isAuthenticated: Boolean(auth?.token),
      isElevatedRole,
      defaultPrivateRoute,
      login,
      register,
      logout,
    }),
    [auth, defaultPrivateRoute, isElevatedRole, isHydrated, login, logout, register],
  )

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}