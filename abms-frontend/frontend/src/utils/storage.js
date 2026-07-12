const AUTH_STORAGE_KEY = 'abms-auth'

export function getStoredAuth() {
  try {
    const rawValue = window.localStorage.getItem(AUTH_STORAGE_KEY)
    return rawValue ? JSON.parse(rawValue) : null
  } catch {
    return null
  }
}

export function setStoredAuth(value) {
  window.localStorage.setItem(AUTH_STORAGE_KEY, JSON.stringify(value))
}

export function clearStoredAuth() {
  window.localStorage.removeItem(AUTH_STORAGE_KEY)
}