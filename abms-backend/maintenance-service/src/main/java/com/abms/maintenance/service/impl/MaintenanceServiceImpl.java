package com.abms.maintenance.service.impl;

import com.abms.maintenance.client.ApartmentClient;
import com.abms.maintenance.constant.MaintenancePriority;
import com.abms.maintenance.constant.MaintenanceStatus;
import com.abms.maintenance.dto.ApartmentResidentResponse;
import com.abms.maintenance.dto.ApartmentResponse;
import com.abms.maintenance.dto.AssignStaffRequest;
import com.abms.maintenance.dto.MaintenanceHistoryResponse;
import com.abms.maintenance.dto.MaintenanceRequestResponse;
import com.abms.maintenance.dto.SubmitMaintenanceRequest;
import com.abms.maintenance.entity.MaintenanceHistory;
import com.abms.maintenance.entity.MaintenanceRequest;
import com.abms.maintenance.exception.BusinessRuleException;
import com.abms.maintenance.exception.ResourceNotFoundException;
import com.abms.maintenance.repository.MaintenanceHistoryRepository;
import com.abms.maintenance.repository.MaintenanceRequestRepository;
import com.abms.maintenance.service.MaintenanceService;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class MaintenanceServiceImpl implements MaintenanceService {

    private static final int MAX_OPEN_REQUESTS = 5;
    private static final Set<String> ALLOWED_CATEGORIES = Set.of(
            "PLUMBING", "ELECTRICAL", "HVAC", "CIVIL", "OTHER");
    private static final Set<String> ALLOWED_PRIORITIES = Set.of(
            MaintenancePriority.NORMAL, MaintenancePriority.EMERGENCY);
    private static final List<String> OPEN_STATUSES = List.of(
            MaintenanceStatus.OPEN, MaintenanceStatus.IN_PROGRESS);

    /**
     * Maps seed resident emails to apartment-service seed userIds.
     * Used only when auth userId and apartment residence userId are out of sync.
     */
    private static final Map<String, UUID> SEED_RESIDENT_USER_IDS_BY_EMAIL = Map.of(
            "resident.a101@abms.local", UUID.fromString("4d5e6f70-8192-4abc-d345-e6f7890a1b2c"),
            "resident.a102@abms.local", UUID.fromString("5e6f7081-92a3-4bcd-e456-f7890a1b2c3d"),
            "resident.b101@abms.local", UUID.fromString("6f708192-a3b4-4cde-f567-890a1b2c3d4e"));

    private final MaintenanceRequestRepository maintenanceRequestRepository;
    private final MaintenanceHistoryRepository maintenanceHistoryRepository;
    private final ApartmentClient apartmentClient;

    @Override
    @Transactional
    public MaintenanceRequestResponse submitRequest(
            SubmitMaintenanceRequest request, UUID actorUserId, String actorEmail) {
        UUID senderId = actorUserId != null ? actorUserId : request.getSenderId();
        if (senderId == null) {
            throw new BusinessRuleException("senderId is required");
        }

        String category = normalize(request.getCategory());
        String priority = normalize(request.getPriority());
        validateCategory(category);
        validatePriority(priority);

        UUID apartmentId = resolveApartmentIdForResident(senderId, actorEmail);

        long openCount = maintenanceRequestRepository.countBySenderIdAndStatusIn(senderId, OPEN_STATUSES);
        if (openCount >= MAX_OPEN_REQUESTS) {
            throw new BusinessRuleException(
                    "You have reached the maximum of 5 open maintenance requests. Please wait for existing requests to be resolved.");
        }

        LocalDateTime now = LocalDateTime.now();
        MaintenanceRequest entity = MaintenanceRequest.builder()
                .requestId(UUID.randomUUID())
                .requestCode(generateRequestCode())
                .apartmentId(apartmentId)
                .senderId(senderId)
                .category(category)
                .priority(priority)
                .title(request.getTitle().trim())
                .description(request.getDescription() == null ? null : request.getDescription().trim())
                .status(MaintenanceStatus.OPEN)
                .createdAt(now)
                .build();

        MaintenanceRequest saved = maintenanceRequestRepository.save(entity);
        appendHistory(saved.getRequestId(), null, MaintenanceStatus.OPEN, senderId, "Request submitted");
        return mapToResponse(saved);
    }

    /**
     * Prefer residence by login userId. If auth/apartment userId are out of sync
     * (common with seeded apartments), fall back to residence lookup by known seed userId for email.
     */
    private UUID resolveApartmentIdForResident(UUID senderId, String actorEmail) {
        return apartmentClient.findActiveResidenceByUserId(senderId)
                .map(ApartmentResidentResponse::getApartmentId)
                .or(() -> resolveApartmentIdBySeedEmail(actorEmail))
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Active residence not found for user: " + senderId));
    }

    private java.util.Optional<UUID> resolveApartmentIdBySeedEmail(String actorEmail) {
        if (actorEmail == null || actorEmail.isBlank()) {
            return java.util.Optional.empty();
        }

        UUID seedUserId = SEED_RESIDENT_USER_IDS_BY_EMAIL.get(actorEmail.trim().toLowerCase(Locale.ROOT));
        if (seedUserId == null) {
            return java.util.Optional.empty();
        }

        return apartmentClient.findActiveResidenceByUserId(seedUserId)
                .map(ApartmentResidentResponse::getApartmentId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<MaintenanceRequestResponse> listRequests(
            String status, String priority, UUID apartmentId, UUID buildingId, String roleName) {
        Set<UUID> allowedApartmentIds = resolveAllowedApartmentIds(buildingId, roleName);

        return maintenanceRequestRepository.findAllByOrderByCreatedAtDesc().stream()
                .filter(item -> status == null || status.isBlank()
                        || item.getStatus().equalsIgnoreCase(status.trim()))
                .filter(item -> priority == null || priority.isBlank()
                        || item.getPriority().equalsIgnoreCase(priority.trim()))
                .filter(item -> apartmentId == null || item.getApartmentId().equals(apartmentId))
                .filter(item -> allowedApartmentIds == null || allowedApartmentIds.contains(item.getApartmentId()))
                .map(this::mapToResponse)
                .toList();
    }

    /**
     * Admin sees all buildings. Manager/Staff are scoped to apartments under X-Building-Id.
     * Returns null when no building filter should apply.
     */
    private Set<UUID> resolveAllowedApartmentIds(UUID buildingId, String roleName) {
        String role = roleName == null ? "" : roleName.trim().toUpperCase(Locale.ROOT);

        if ("ADMIN".equals(role)) {
            return null;
        }

        if ("MANAGER".equals(role) || "STAFF".equals(role)) {
            if (buildingId == null) {
                return Set.of();
            }
            return apartmentClient.getApartmentsByBuildingId(buildingId).stream()
                    .map(ApartmentResponse::getApartmentId)
                    .collect(Collectors.toSet());
        }

        if (buildingId != null) {
            return apartmentClient.getApartmentsByBuildingId(buildingId).stream()
                    .map(ApartmentResponse::getApartmentId)
                    .collect(Collectors.toSet());
        }

        return null;
    }

    @Override
    @Transactional(readOnly = true)
    public List<MaintenanceRequestResponse> listBySender(UUID senderId) {
        return maintenanceRequestRepository.findBySenderIdOrderByCreatedAtDesc(senderId).stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<MaintenanceRequestResponse> listByTechnician(UUID technicianId) {
        return maintenanceRequestRepository.findByTechnicianIdOrderByCreatedAtDesc(technicianId).stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public MaintenanceRequestResponse getById(UUID requestId) {
        return mapToResponse(getOrThrow(requestId));
    }

    @Override
    @Transactional(readOnly = true)
    public List<MaintenanceHistoryResponse> getHistoryByRequestId(UUID requestId) {
        getOrThrow(requestId);
        return maintenanceHistoryRepository.findByRequestIdOrderByChangedAtDesc(requestId).stream()
                .map(this::mapHistoryToResponse)
                .toList();
    }

    @Override
    @Transactional
    public MaintenanceRequestResponse assignStaff(UUID requestId, AssignStaffRequest request, UUID actorUserId) {
        MaintenanceRequest entity = getOrThrow(requestId);

        if (MaintenanceStatus.CANCELLED.equalsIgnoreCase(entity.getStatus())
                || MaintenanceStatus.RESOLVED.equalsIgnoreCase(entity.getStatus())) {
            throw new BusinessRuleException("Cannot assign staff to a " + entity.getStatus() + " request");
        }

        String fromStatus = entity.getStatus();
        entity.setTechnicianId(request.getTechnicianId());
        entity.setStatus(MaintenanceStatus.IN_PROGRESS);
        entity.setAssignedAt(LocalDateTime.now());

        MaintenanceRequest saved = maintenanceRequestRepository.save(entity);
        appendHistory(
                saved.getRequestId(),
                fromStatus,
                MaintenanceStatus.IN_PROGRESS,
                actorUserId,
                "Assigned technician " + request.getTechnicianId());
        return mapToResponse(saved);
    }

    @Override
    @Transactional
    public MaintenanceRequestResponse completeRequest(UUID requestId, UUID actorUserId) {
        MaintenanceRequest entity = getOrThrow(requestId);

        if (!MaintenanceStatus.IN_PROGRESS.equalsIgnoreCase(entity.getStatus())) {
            throw new BusinessRuleException("Only IN_PROGRESS requests can be marked as resolved");
        }

        if (actorUserId != null && entity.getTechnicianId() != null
                && !entity.getTechnicianId().equals(actorUserId)) {
            throw new BusinessRuleException("Only the assigned technician can complete this request");
        }

        String fromStatus = entity.getStatus();
        entity.setStatus(MaintenanceStatus.RESOLVED);
        entity.setResolvedAt(LocalDateTime.now());

        MaintenanceRequest saved = maintenanceRequestRepository.save(entity);
        appendHistory(saved.getRequestId(), fromStatus, MaintenanceStatus.RESOLVED, actorUserId, "Marked as resolved");
        return mapToResponse(saved);
    }

    private MaintenanceRequest getOrThrow(UUID requestId) {
        return maintenanceRequestRepository.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("Maintenance request not found: " + requestId));
    }

    private void appendHistory(UUID requestId, String fromStatus, String toStatus, UUID changedBy, String note) {
        maintenanceHistoryRepository.save(MaintenanceHistory.builder()
                .historyId(UUID.randomUUID())
                .requestId(requestId)
                .fromStatus(fromStatus)
                .toStatus(toStatus)
                .changedBy(changedBy)
                .note(note)
                .changedAt(LocalDateTime.now())
                .build());
    }

    private String generateRequestCode() {
        String datePart = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        int suffix = ThreadLocalRandom.current().nextInt(1000, 10000);
        return "MR-" + datePart + "-" + suffix;
    }

    private void validateCategory(String category) {
        if (!ALLOWED_CATEGORIES.contains(category)) {
            throw new BusinessRuleException(
                    "Invalid category. Allowed: PLUMBING, ELECTRICAL, HVAC, CIVIL, OTHER");
        }
    }

    private void validatePriority(String priority) {
        if (!ALLOWED_PRIORITIES.contains(priority)) {
            throw new BusinessRuleException("Invalid priority. Allowed: NORMAL, EMERGENCY");
        }
    }

    private String normalize(String value) {
        return value == null ? null : value.trim().toUpperCase(Locale.ROOT);
    }

    private MaintenanceRequestResponse mapToResponse(MaintenanceRequest entity) {
        return MaintenanceRequestResponse.builder()
                .requestId(entity.getRequestId())
                .requestCode(entity.getRequestCode())
                .apartmentId(entity.getApartmentId())
                .senderId(entity.getSenderId())
                .technicianId(entity.getTechnicianId())
                .category(entity.getCategory())
                .priority(entity.getPriority())
                .title(entity.getTitle())
                .description(entity.getDescription())
                .status(entity.getStatus())
                .createdAt(entity.getCreatedAt())
                .assignedAt(entity.getAssignedAt())
                .resolvedAt(entity.getResolvedAt())
                .build();
    }

    private MaintenanceHistoryResponse mapHistoryToResponse(MaintenanceHistory entity) {
        return MaintenanceHistoryResponse.builder()
                .historyId(entity.getHistoryId())
                .requestId(entity.getRequestId())
                .fromStatus(entity.getFromStatus())
                .toStatus(entity.getToStatus())
                .changedBy(entity.getChangedBy())
                .note(entity.getNote())
                .changedAt(entity.getChangedAt())
                .build();
    }
}
