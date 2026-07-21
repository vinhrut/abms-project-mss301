package com.abms.maintenance.service;

import com.abms.maintenance.dto.AssignStaffRequest;
import com.abms.maintenance.dto.MaintenanceHistoryResponse;
import com.abms.maintenance.dto.MaintenanceRequestResponse;
import com.abms.maintenance.dto.SubmitMaintenanceRequest;
import java.util.List;
import java.util.UUID;

public interface MaintenanceService {

    MaintenanceRequestResponse submitRequest(SubmitMaintenanceRequest request, UUID actorUserId, String actorEmail);

    List<MaintenanceRequestResponse> listRequests(
            String status, String priority, UUID apartmentId, UUID buildingId, String roleName);

    List<MaintenanceRequestResponse> listBySender(UUID senderId);

    List<MaintenanceRequestResponse> listByTechnician(UUID technicianId);

    MaintenanceRequestResponse getById(UUID requestId);

    List<MaintenanceHistoryResponse> getHistoryByRequestId(UUID requestId);

    MaintenanceRequestResponse assignStaff(UUID requestId, AssignStaffRequest request, UUID actorUserId);

    MaintenanceRequestResponse completeRequest(UUID requestId, UUID actorUserId);
}
