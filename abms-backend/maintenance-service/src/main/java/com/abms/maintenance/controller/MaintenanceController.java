package com.abms.maintenance.controller;

import com.abms.maintenance.dto.AssignStaffRequest;
import com.abms.maintenance.dto.MaintenanceHistoryResponse;
import com.abms.maintenance.dto.MaintenanceRequestResponse;
import com.abms.maintenance.dto.SubmitMaintenanceRequest;
import com.abms.maintenance.service.MaintenanceService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/maintenance-requests")
@RequiredArgsConstructor
public class MaintenanceController {

    private final MaintenanceService maintenanceService;

    @PostMapping
    public ResponseEntity<MaintenanceRequestResponse> submitRequest(
            @Valid @RequestBody SubmitMaintenanceRequest request,
            @RequestHeader(value = "X-User-Id", required = false) String userIdHeader,
            @RequestHeader(value = "X-User-Email", required = false) String emailHeader) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(maintenanceService.submitRequest(request, parseUuid(userIdHeader), emailHeader));
    }

    @GetMapping
    public ResponseEntity<List<MaintenanceRequestResponse>> listRequests(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String priority,
            @RequestParam(required = false) UUID apartmentId,
            @RequestParam(required = false) UUID senderId,
            @RequestParam(required = false) UUID technicianId) {
        if (senderId != null) {
            return ResponseEntity.ok(maintenanceService.listBySender(senderId));
        }
        if (technicianId != null) {
            return ResponseEntity.ok(maintenanceService.listByTechnician(technicianId));
        }
        return ResponseEntity.ok(maintenanceService.listRequests(status, priority, apartmentId));
    }

    @GetMapping("/mine")
    public ResponseEntity<List<MaintenanceRequestResponse>> listMine(
            @RequestHeader(value = "X-User-Id", required = false) String userIdHeader,
            @RequestParam(required = false) UUID senderId) {
        UUID resolvedSenderId = parseUuid(userIdHeader);
        if (resolvedSenderId == null) {
            resolvedSenderId = senderId;
        }
        if (resolvedSenderId == null) {
            return ResponseEntity.badRequest().build();
        }
        return ResponseEntity.ok(maintenanceService.listBySender(resolvedSenderId));
    }

    @GetMapping("/my-tasks")
    public ResponseEntity<List<MaintenanceRequestResponse>> listMyTasks(
            @RequestHeader(value = "X-User-Id", required = false) String userIdHeader,
            @RequestParam(required = false) UUID technicianId) {
        UUID resolvedTechnicianId = parseUuid(userIdHeader);
        if (resolvedTechnicianId == null) {
            resolvedTechnicianId = technicianId;
        }
        if (resolvedTechnicianId == null) {
            return ResponseEntity.badRequest().build();
        }
        return ResponseEntity.ok(maintenanceService.listByTechnician(resolvedTechnicianId));
    }

    @GetMapping("/{requestId}")
    public ResponseEntity<MaintenanceRequestResponse> getById(@PathVariable UUID requestId) {
        return ResponseEntity.ok(maintenanceService.getById(requestId));
    }

    @GetMapping("/{requestId}/history")
    public ResponseEntity<List<MaintenanceHistoryResponse>> getHistory(@PathVariable UUID requestId) {
        return ResponseEntity.ok(maintenanceService.getHistoryByRequestId(requestId));
    }

    @PostMapping("/{requestId}/assign")
    public ResponseEntity<MaintenanceRequestResponse> assignStaff(
            @PathVariable UUID requestId,
            @Valid @RequestBody AssignStaffRequest request,
            @RequestHeader(value = "X-User-Id", required = false) String userIdHeader) {
        return ResponseEntity.ok(maintenanceService.assignStaff(requestId, request, parseUuid(userIdHeader)));
    }

    @PostMapping("/{requestId}/complete")
    public ResponseEntity<MaintenanceRequestResponse> completeRequest(
            @PathVariable UUID requestId,
            @RequestHeader(value = "X-User-Id", required = false) String userIdHeader) {
        return ResponseEntity.ok(maintenanceService.completeRequest(requestId, parseUuid(userIdHeader)));
    }

    private UUID parseUuid(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return UUID.fromString(value.trim());
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }
}
