package com.abms.vehicle.service.impl;

import com.abms.vehicle.client.ApartmentClient;
import com.abms.vehicle.constant.RoleNames;
import com.abms.vehicle.constant.VehicleStatus;
import com.abms.vehicle.constant.VehicleType;
import com.abms.vehicle.dto.ApartmentResponse;
import com.abms.vehicle.dto.VehicleRequest;
import com.abms.vehicle.dto.VehicleResponse;
import com.abms.vehicle.entity.Vehicle;
import com.abms.vehicle.entity.VehicleLimit;
import com.abms.vehicle.exception.AccessDeniedException;
import com.abms.vehicle.exception.DuplicateLicensePlateException;
import com.abms.vehicle.exception.InvalidVehicleStatusException;
import com.abms.vehicle.exception.LimitExceededException;
import com.abms.vehicle.exception.ResourceNotFoundException;
import com.abms.vehicle.repository.VehicleLimitRepository;
import com.abms.vehicle.repository.VehicleRepository;
import com.abms.vehicle.security.CurrentUser;
import com.abms.vehicle.service.VehicleService;
import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class VehicleServiceImpl implements VehicleService {

    private final VehicleRepository vehicleRepository;
    private final VehicleLimitRepository vehicleLimitRepository;
    private final ApartmentClient apartmentClient;

    @Override
    @Transactional
    public VehicleResponse registerVehicle(CurrentUser actor, VehicleRequest request) {
        ensureRole(actor, RoleNames.RESIDENT);
        String normalizedLicensePlate = normalizeLicensePlate(request.getLicensePlate());
        String normalizedType = normalizeVehicleType(request.getType());

        validateApartment(request.getApartmentId());
        validateResidentResidence(actor.getUserId(), request.getApartmentId());
        removeClosedVehicleWithSameLicensePlateOrThrow(normalizedLicensePlate);
        validateApprovedVehicleLimit(request.getApartmentId(), normalizedType);

        Vehicle vehicle = Vehicle.builder()
                .vehicleId(UUID.randomUUID())
                .apartmentId(request.getApartmentId())
                .ownerId(actor.getUserId())
                .licensePlate(normalizedLicensePlate)
                .type(normalizedType)
                .brand(normalizeOptional(request.getBrand()))
                .status(VehicleStatus.PENDING)
                .build();
        return mapToResponse(vehicleRepository.save(vehicle));
    }

    @Override
    @Transactional
    public VehicleResponse updateVehicle(CurrentUser actor, UUID vehicleId, VehicleRequest request) {
        Vehicle vehicle = getVehicleOrThrow(vehicleId);
        ensureCanAccessVehicle(actor, vehicle);
        if (VehicleStatus.APPROVED.equalsIgnoreCase(vehicle.getStatus())) {
            throw new InvalidVehicleStatusException("Approved vehicles cannot be updated. Please cancel it and create a new registration request.");
        }
        if (VehicleStatus.PENDING_CANCEL.equalsIgnoreCase(vehicle.getStatus()) || VehicleStatus.CANCELLED.equalsIgnoreCase(vehicle.getStatus()) || VehicleStatus.INACTIVE.equalsIgnoreCase(vehicle.getStatus())) {
            throw new InvalidVehicleStatusException("Cancelled or inactive vehicles cannot be updated");
        }

        UUID ownerId = RoleNames.RESIDENT.equals(actor.getRoleName()) ? actor.getUserId() : request.getOwnerId();
        if (ownerId == null) {
            throw new AccessDeniedException("Owner id is required for manager/staff updates");
        }

        String normalizedLicensePlate = normalizeLicensePlate(request.getLicensePlate());
        String normalizedType = normalizeVehicleType(request.getType());
        validateApartment(request.getApartmentId());
        validateResidentResidence(ownerId, request.getApartmentId());
        ensureCanManageApartment(actor, request.getApartmentId());
        if (vehicleRepository.existsByLicensePlateAndVehicleIdNot(normalizedLicensePlate, vehicleId)) {
            throw new DuplicateLicensePlateException("This license plate is already registered in the system: " + normalizedLicensePlate);
        }
        ensureVehicleLimitExists(request.getApartmentId(), normalizedType);

        vehicle.setApartmentId(request.getApartmentId());
        vehicle.setOwnerId(ownerId);
        vehicle.setLicensePlate(normalizedLicensePlate);
        vehicle.setType(normalizedType);
        vehicle.setBrand(normalizeOptional(request.getBrand()));
        vehicle.setStatus(VehicleStatus.PENDING);
        return mapToResponse(vehicleRepository.save(vehicle));
    }

    @Override
    public VehicleResponse getVehicleById(CurrentUser actor, UUID vehicleId) {
        Vehicle vehicle = getVehicleOrThrow(vehicleId);
        ensureCanAccessVehicle(actor, vehicle);
        return mapToResponse(vehicle);
    }

    @Override
    public List<VehicleResponse> getMyVehicles(CurrentUser actor) {
        ensureRole(actor, RoleNames.RESIDENT);
        return vehicleRepository.findByOwnerIdOrderByStatusAscLicensePlateAsc(actor.getUserId()).stream().map(this::mapToResponse).toList();
    }

    @Override
    public Page<VehicleResponse> searchVehicles(CurrentUser actor, String status, String type, String licensePlate, UUID apartmentId, UUID ownerId, Pageable pageable) {
        if (RoleNames.RESIDENT.equals(actor.getRoleName())) ownerId = actor.getUserId();
        Set<UUID> scopedApartmentIds = null;
        if (isBuildingScopedRole(actor)) {
            ensureActorHasBuilding(actor);
            scopedApartmentIds = apartmentClient.getApartmentsByBuildingId(actor.getBuildingId()).stream()
                    .map(ApartmentResponse::getApartmentId).collect(Collectors.toSet());
            if (apartmentId != null && !scopedApartmentIds.contains(apartmentId)) {
                throw new AccessDeniedException("You are not allowed to manage vehicles of this apartment");
            }
        }
        return vehicleRepository.findAll(buildVehicleSpecification(status, type, licensePlate, apartmentId, ownerId, scopedApartmentIds), pageable)
                .map(this::mapToResponse);
    }

    @Override
    @Transactional
    public VehicleResponse approveVehicle(CurrentUser actor, UUID vehicleId) {
        ensureManagerOrAdmin(actor);
        Vehicle vehicle = getVehicleOrThrow(vehicleId);
        ensureCanManageApartment(actor, vehicle.getApartmentId());
        if (VehicleStatus.PENDING_CANCEL.equalsIgnoreCase(vehicle.getStatus())) {
            vehicle.setStatus(VehicleStatus.INACTIVE);
            return mapToResponse(vehicleRepository.save(vehicle));
        }
        if (!VehicleStatus.PENDING.equalsIgnoreCase(vehicle.getStatus())) {
            throw new InvalidVehicleStatusException("Only pending vehicles or pending cancel requests can be approved");
        }
        validateApprovedVehicleLimit(vehicle.getApartmentId(), vehicle.getType());
        vehicle.setStatus(VehicleStatus.APPROVED);
        return mapToResponse(vehicleRepository.save(vehicle));
    }

    @Override
    @Transactional
    public VehicleResponse rejectVehicle(CurrentUser actor, UUID vehicleId) {
        ensureManagerOrAdmin(actor);
        Vehicle vehicle = getVehicleOrThrow(vehicleId);
        ensureCanManageApartment(actor, vehicle.getApartmentId());
        if (VehicleStatus.PENDING_CANCEL.equalsIgnoreCase(vehicle.getStatus())) {
            vehicle.setStatus(VehicleStatus.APPROVED);
            return mapToResponse(vehicleRepository.save(vehicle));
        }
        if (!VehicleStatus.PENDING.equalsIgnoreCase(vehicle.getStatus())) {
            throw new InvalidVehicleStatusException("Only pending vehicles or pending cancel requests can be rejected");
        }
        vehicle.setStatus(VehicleStatus.REJECTED);
        return mapToResponse(vehicleRepository.save(vehicle));
    }

    @Override
    @Transactional
    public VehicleResponse cancelVehicle(CurrentUser actor, UUID vehicleId) {
        ensureRole(actor, RoleNames.RESIDENT);
        Vehicle vehicle = getVehicleOrThrow(vehicleId);
        if (!actor.getUserId().equals(vehicle.getOwnerId())) {
            throw new AccessDeniedException("Residents can only cancel their own vehicle registrations");
        }
        if (VehicleStatus.REJECTED.equalsIgnoreCase(vehicle.getStatus()) || VehicleStatus.CANCELLED.equalsIgnoreCase(vehicle.getStatus()) || VehicleStatus.INACTIVE.equalsIgnoreCase(vehicle.getStatus())) {
            throw new InvalidVehicleStatusException("This vehicle registration is already closed");
        }
        VehicleResponse response = mapToResponse(vehicle);
        vehicleRepository.delete(vehicle);
        vehicleRepository.flush();
        return response;
    }

    private void removeClosedVehicleWithSameLicensePlateOrThrow(String licensePlate) {
        Optional<Vehicle> existingVehicle = vehicleRepository.findByLicensePlate(licensePlate);
        if (existingVehicle.isEmpty()) return;

        Vehicle vehicle = existingVehicle.get();
        if (VehicleStatus.PENDING.equalsIgnoreCase(vehicle.getStatus()) || VehicleStatus.APPROVED.equalsIgnoreCase(vehicle.getStatus())) {
            throw new DuplicateLicensePlateException("This license plate is already registered in the system: " + licensePlate);
        }

        vehicleRepository.delete(vehicle);
        vehicleRepository.flush();
    }

    private Vehicle getVehicleOrThrow(UUID vehicleId) {
        return vehicleRepository.findById(vehicleId).orElseThrow(() -> new ResourceNotFoundException("Vehicle not found: " + vehicleId));
    }

    private void validateApartment(UUID apartmentId) {
        String apartmentStatus = apartmentClient.getApartmentById(apartmentId).getStatus();
        if (apartmentStatus == null || VehicleStatus.INACTIVE.equalsIgnoreCase(apartmentStatus)) {
            throw new ResourceNotFoundException("Apartment is not active: " + apartmentId);
        }
    }

    private void validateResidentResidence(UUID ownerId, UUID apartmentId) {
        if (!apartmentClient.hasActiveResidence(apartmentId, ownerId)) {
            throw new ResourceNotFoundException("Resident does not belong to apartment: " + apartmentId);
        }
    }

    private void ensureCanAccessVehicle(CurrentUser actor, Vehicle vehicle) {
        if (RoleNames.ADMIN.equals(actor.getRoleName())) return;
        if (RoleNames.RESIDENT.equals(actor.getRoleName())) {
            if (!actor.getUserId().equals(vehicle.getOwnerId())) throw new AccessDeniedException("Residents can only access their own vehicles");
            return;
        }
        ensureCanManageApartment(actor, vehicle.getApartmentId());
    }

    private void ensureCanManageApartment(CurrentUser actor, UUID apartmentId) {
        if (RoleNames.ADMIN.equals(actor.getRoleName())) return;
        if (!isBuildingScopedRole(actor)) throw new AccessDeniedException("You are not allowed to manage vehicle data");
        ensureActorHasBuilding(actor);
        ApartmentResponse apartment = apartmentClient.getApartmentById(apartmentId);
        if (apartment == null || !actor.getBuildingId().equals(apartment.getBuildingId())) {
            throw new AccessDeniedException("You are not allowed to manage vehicles of another building");
        }
    }

    private boolean isBuildingScopedRole(CurrentUser actor) {
        return RoleNames.MANAGER.equals(actor.getRoleName()) || RoleNames.STAFF.equals(actor.getRoleName());
    }

    private void ensureActorHasBuilding(CurrentUser actor) {
        if (actor.getBuildingId() == null) throw new AccessDeniedException("Building scoped user does not have building context");
    }

    private void ensureManagerOrAdmin(CurrentUser actor) {
        if (!RoleNames.ADMIN.equals(actor.getRoleName()) && !RoleNames.MANAGER.equals(actor.getRoleName()) && !RoleNames.STAFF.equals(actor.getRoleName())) {
            throw new AccessDeniedException("Only admin, manager or staff can perform this action");
        }
    }

    private void ensureRole(CurrentUser actor, String roleName) {
        if (!roleName.equals(actor.getRoleName())) throw new AccessDeniedException("This action requires role " + roleName);
    }

    private String normalizeLicensePlate(String licensePlate) { return licensePlate.trim().toUpperCase(Locale.ROOT); }

    private String normalizeVehicleType(String type) {
        String normalizedType = type.trim().toUpperCase(Locale.ROOT);
        if (!VehicleType.isAllowed(normalizedType)) throw new InvalidVehicleStatusException("Vehicle type must be CAR or MOTORBIKE");
        return normalizedType;
    }

    private String normalizeOptional(String value) { return value == null || value.isBlank() ? null : value.trim(); }

    private VehicleLimit ensureVehicleLimitExists(UUID apartmentId, String type) {
        return vehicleLimitRepository.findByApartmentIdAndVehicleType(apartmentId, type)
                .orElseThrow(() -> new ResourceNotFoundException("Vehicle limit not found for apartment " + apartmentId + " and type " + type));
    }

    private void validateApprovedVehicleLimit(UUID apartmentId, String type) {
        VehicleLimit vehicleLimit = ensureVehicleLimitExists(apartmentId, type);
        long currentQuantity = vehicleRepository.countByApartmentIdAndTypeAndStatus(apartmentId, type, VehicleStatus.APPROVED);
        if (currentQuantity >= vehicleLimit.getMaxQuantity()) throw new LimitExceededException("Vehicle limit exceeded for apartment " + apartmentId + " and type " + type);
    }

    private Specification<Vehicle> buildVehicleSpecification(String status, String type, String licensePlate, UUID apartmentId, UUID ownerId, Set<UUID> scopedApartmentIds) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (status != null && !status.isBlank()) predicates.add(criteriaBuilder.equal(criteriaBuilder.upper(root.get("status")), status.trim().toUpperCase(Locale.ROOT)));
            if (type != null && !type.isBlank()) predicates.add(criteriaBuilder.equal(criteriaBuilder.upper(root.get("type")), normalizeVehicleType(type)));
            if (licensePlate != null && !licensePlate.isBlank()) predicates.add(criteriaBuilder.like(criteriaBuilder.upper(root.get("licensePlate")), "%" + licensePlate.trim().toUpperCase(Locale.ROOT) + "%"));
            if (apartmentId != null) predicates.add(criteriaBuilder.equal(root.get("apartmentId"), apartmentId));
            if (ownerId != null) predicates.add(criteriaBuilder.equal(root.get("ownerId"), ownerId));
            if (scopedApartmentIds != null) predicates.add(scopedApartmentIds.isEmpty() ? criteriaBuilder.disjunction() : root.get("apartmentId").in(scopedApartmentIds));
            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }

    private VehicleResponse mapToResponse(Vehicle vehicle) {
        return VehicleResponse.builder().vehicleId(vehicle.getVehicleId()).apartmentId(vehicle.getApartmentId()).ownerId(vehicle.getOwnerId())
                .licensePlate(vehicle.getLicensePlate()).type(vehicle.getType()).brand(vehicle.getBrand()).status(vehicle.getStatus()).build();
    }
}