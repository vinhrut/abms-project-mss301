import { apartmentService } from '../../../services/apartmentService.js'
import { invoiceService } from '../../../services/invoiceService.js'
import { maintenanceService } from '../../../services/maintenanceService.js'
import { aggregateDashboardOverview, emptyDashboardOverview } from '../utils/aggregateDashboardOverview.js'

function toArray(value) {
  return Array.isArray(value) ? value : []
}

/**
 * Load dashboard by calling existing domain APIs, then aggregate on FE.
 * Optional buildingId scopes apartments/contracts (Manager); invoices/maintenance filtered by apartment set.
 */
export const dashboardOverviewService = {
  async loadOverview({ buildingId } = {}) {
    const [apartmentsResult, invoicesResult, maintenanceResult, contractsResult] = await Promise.allSettled([
      buildingId
        ? apartmentService.getApartmentsByBuildingId(buildingId)
        : apartmentService.getAllApartments(),
      invoiceService.getInvoices(),
      maintenanceService.getAllRequests(),
      apartmentService.getContracts(buildingId || undefined),
    ])

    const apartments = apartmentsResult.status === 'fulfilled' ? toArray(apartmentsResult.value) : []
    const apartmentIds = new Set(apartments.map((item) => String(item.apartmentId || '')))

    let invoices = invoicesResult.status === 'fulfilled' ? toArray(invoicesResult.value) : []
    let maintenanceRequests =
      maintenanceResult.status === 'fulfilled' ? toArray(maintenanceResult.value) : []
    let contracts = contractsResult.status === 'fulfilled' ? toArray(contractsResult.value) : []

    if (buildingId && apartmentIds.size) {
      invoices = invoices.filter((invoice) => apartmentIds.has(String(invoice.apartmentId || '')))
      maintenanceRequests = maintenanceRequests.filter((request) =>
        apartmentIds.has(String(request.apartmentId || '')),
      )
      contracts = contracts.filter((contract) => apartmentIds.has(String(contract.apartmentId || '')))
    }

    const failed = [apartmentsResult, invoicesResult, maintenanceResult, contractsResult].filter(
      (result) => result.status === 'rejected',
    )

    if (!apartments.length && !invoices.length && failed.length === 4) {
      const firstError = failed[0]?.reason
      throw firstError || new Error('Unable to load dashboard data.')
    }

    const overview = aggregateDashboardOverview({
      apartments,
      invoices,
      maintenanceRequests,
      contracts,
    })

    return overview || emptyDashboardOverview()
  },
}
