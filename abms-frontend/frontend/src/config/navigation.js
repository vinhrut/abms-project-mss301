import { ROLE_KEYS, normalizeRole } from './roles.js'

export const APP_ROUTES = {
  home: '/',
  login: '/login',
  register: '/register',
  dashboard: '/app/dashboard',
  users: '/app/users',
  profile: '/app/profile',
  changePassword: '/app/change-password',
  buildings: '/app/buildings',
  apartments: '/app/apartments',
  residents: '/app/residents',
  residentApprovals: '/app/residents/approvals',
  contracts: '/app/contracts',
  invoices: '/app/invoices',
  payments: '/app/payments',
  maintenance: '/app/maintenance',
  maintenanceSubmit: '/app/maintenance/submit',
  maintenanceTasks: '/app/maintenance/my-tasks',
  vehicles: '/app/vehicles',
  vehicleRegister: '/app/vehicles/register',
  notifications: '/app/notifications',
  reports: '/app/reports',
  jobs: '/app/system/jobs',
}

export function getDefaultPrivateRoute(roleName) {
  const normalizedRole = normalizeRole(roleName)

  if (normalizedRole === ROLE_KEYS.TECHNICIAN) {
    return APP_ROUTES.maintenanceTasks
  }

  return APP_ROUTES.dashboard
}

export const navigationSections = [
  {
    id: 'overview',
    title: 'Overview',
    items: [
      { label: 'Dashboard', to: APP_ROUTES.dashboard, roles: [ROLE_KEYS.ADMIN, ROLE_KEYS.MANAGER, ROLE_KEYS.STAFF, ROLE_KEYS.TECHNICIAN, ROLE_KEYS.RESIDENT] },
    ],
  },
  {
    id: 'property',
    title: 'Building & Residence',
    items: [
      { label: 'Buildings', to: APP_ROUTES.buildings, roles: [ROLE_KEYS.ADMIN, ROLE_KEYS.MANAGER] },
      { label: 'User Management', to: APP_ROUTES.users, roles: [ROLE_KEYS.ADMIN, ROLE_KEYS.MANAGER] },
      { label: 'Apartments', to: APP_ROUTES.apartments, roles: [ROLE_KEYS.ADMIN, ROLE_KEYS.MANAGER, ROLE_KEYS.STAFF] },
      { label: 'Residents', to: APP_ROUTES.residents, roles: [ROLE_KEYS.ADMIN, ROLE_KEYS.MANAGER, ROLE_KEYS.STAFF] },
      { label: 'Resident Approvals', to: APP_ROUTES.residentApprovals, roles: [ROLE_KEYS.ADMIN, ROLE_KEYS.MANAGER, ROLE_KEYS.STAFF] },
      { label: 'Contracts', to: APP_ROUTES.contracts, roles: [ROLE_KEYS.ADMIN, ROLE_KEYS.MANAGER, ROLE_KEYS.STAFF] },
    ],
  },
  {
    id: 'operation',
    title: 'Operations',
    items: [
      { label: 'Invoices', to: APP_ROUTES.invoices, roles: [ROLE_KEYS.ADMIN, ROLE_KEYS.MANAGER, ROLE_KEYS.STAFF, ROLE_KEYS.RESIDENT] },
      { label: 'Payments', to: APP_ROUTES.payments, roles: [ROLE_KEYS.ADMIN, ROLE_KEYS.MANAGER, ROLE_KEYS.STAFF, ROLE_KEYS.RESIDENT] },
      { label: 'Maintenance', to: APP_ROUTES.maintenance, roles: [ROLE_KEYS.ADMIN, ROLE_KEYS.MANAGER, ROLE_KEYS.STAFF, ROLE_KEYS.TECHNICIAN, ROLE_KEYS.RESIDENT] },
      { label: 'Submit Request', to: APP_ROUTES.maintenanceSubmit, roles: [ROLE_KEYS.ADMIN, ROLE_KEYS.MANAGER, ROLE_KEYS.STAFF, ROLE_KEYS.RESIDENT] },
      { label: 'My Tasks', to: APP_ROUTES.maintenanceTasks, roles: [ROLE_KEYS.TECHNICIAN] },
      { label: 'Vehicles', to: APP_ROUTES.vehicles, roles: [ROLE_KEYS.ADMIN, ROLE_KEYS.MANAGER, ROLE_KEYS.STAFF, ROLE_KEYS.RESIDENT] },
      { label: 'Register Vehicle', to: APP_ROUTES.vehicleRegister, roles: [ROLE_KEYS.ADMIN, ROLE_KEYS.MANAGER, ROLE_KEYS.STAFF, ROLE_KEYS.RESIDENT] },
    ],
  },
  {
    id: 'insight',
    title: 'Reports & Communications',
    items: [
      { label: 'Reports', to: APP_ROUTES.reports, roles: [ROLE_KEYS.ADMIN, ROLE_KEYS.MANAGER] },
      { label: 'Notifications', to: APP_ROUTES.notifications, roles: [ROLE_KEYS.ADMIN, ROLE_KEYS.MANAGER, ROLE_KEYS.STAFF, ROLE_KEYS.RESIDENT] },
      { label: 'System Jobs', to: APP_ROUTES.jobs, roles: [ROLE_KEYS.ADMIN] },
    ],
  },
  {
    id: 'account',
    title: 'Account',
    items: [
      { label: 'Profile', to: APP_ROUTES.profile, roles: [ROLE_KEYS.ADMIN, ROLE_KEYS.MANAGER, ROLE_KEYS.STAFF, ROLE_KEYS.TECHNICIAN, ROLE_KEYS.RESIDENT] },
      { label: 'Change Password', to: APP_ROUTES.changePassword, roles: [ROLE_KEYS.ADMIN, ROLE_KEYS.MANAGER, ROLE_KEYS.STAFF, ROLE_KEYS.TECHNICIAN, ROLE_KEYS.RESIDENT] },
    ],
  },
]

export function getNavigationForRole(roleName) {
  const normalizedRole = normalizeRole(roleName)

  return navigationSections
    .map((section) => ({
      ...section,
      items: section.items.filter((item) => item.roles.includes(normalizedRole)),
    }))
    .filter((section) => section.items.length > 0)
}