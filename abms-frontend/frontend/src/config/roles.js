export const ROLE_KEYS = {
  ADMIN: 'ADMIN',
  MANAGER: 'MANAGER',
  STAFF: 'STAFF',
  TECHNICIAN: 'TECHNICIAN',
  RESIDENT: 'RESIDENT',
}

const roleAliasMap = {
  ROLE_ADMIN: ROLE_KEYS.ADMIN,
  ADMIN: ROLE_KEYS.ADMIN,
  ROLE_MANAGER: ROLE_KEYS.MANAGER,
  MANAGER: ROLE_KEYS.MANAGER,
  BUILDING_MANAGER: ROLE_KEYS.MANAGER,
  ROLE_STAFF: ROLE_KEYS.STAFF,
  STAFF: ROLE_KEYS.STAFF,
  RECEPTIONIST: ROLE_KEYS.STAFF,
  ROLE_TECHNICIAN: ROLE_KEYS.TECHNICIAN,
  TECHNICIAN: ROLE_KEYS.TECHNICIAN,
  TECHNICAL: ROLE_KEYS.TECHNICIAN,
  ROLE_RESIDENT: ROLE_KEYS.RESIDENT,
  RESIDENT: ROLE_KEYS.RESIDENT,
  ROLE_USER: ROLE_KEYS.RESIDENT,
  USER: ROLE_KEYS.RESIDENT,
}

export function normalizeRole(roleName) {
  if (!roleName) {
    return ROLE_KEYS.RESIDENT
  }

  return roleAliasMap[roleName] || roleName.replace(/^ROLE_/, '') || ROLE_KEYS.RESIDENT
}

export function isRoleIn(roleName, roles = []) {
  const normalizedRole = normalizeRole(roleName)
  return roles.includes(normalizedRole)
}

export function isElevatedRole(roleName) {
  return isRoleIn(roleName, [ROLE_KEYS.ADMIN, ROLE_KEYS.MANAGER, ROLE_KEYS.STAFF])
}

export function getRoleLabel(roleName) {
  const normalizedRole = normalizeRole(roleName)

  const labels = {
    [ROLE_KEYS.ADMIN]: 'Admin',
    [ROLE_KEYS.MANAGER]: 'Building Manager',
    [ROLE_KEYS.STAFF]: 'Staff',
    [ROLE_KEYS.TECHNICIAN]: 'Technician',
    [ROLE_KEYS.RESIDENT]: 'Resident',
  }

  return labels[normalizedRole] || normalizedRole
}