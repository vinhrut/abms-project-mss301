package com.abms.vehicle.service.impl;

import com.abms.vehicle.client.ApartmentClient;
import com.abms.vehicle.constant.RoleNames;
import com.abms.vehicle.constant.VehicleStatus;
import com.abms.vehicle.constant.VehicleType;
import com.abms.vehicle.dto.ApartmentResponse;
import com.abms.vehicle.dto.VehicleLimitRequest;
import com.abms.vehicle.dto.VehicleLimitResponse;
import com.abms.vehicle.entity.VehicleLimit;
import com.abms.vehicle.exception.AccessDeniedException;
import com.abms.vehicle.exception.InvalidVehicleStatusException;
import com.abms.vehicle.exception.LimitExceededException;
import com.abms.vehicle.exception.ResourceNotFoundException;
import com.abms.vehicle.repository.VehicleLimitRepository;
import com.abms.vehicle.repository.VehicleRepository;
import com.abms.vehicle.security.CurrentUser;
import com.abms.vehicle.service.VehicleLimitService;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class VehicleLimitServiceImpl implements VehicleLimitService {

    private final VehicleLimitRepository vehicleLimitRepository;
    private final VehicleRepository vehicleRepository;
    private final ApartmentClient apartmentClient;

    @Override
    @Transactional
    public VehicleLimitResponse createLimit(CurrentUser actor, VehicleLimitRequest request) {
        ensureManagerOrAdmin(actor);
        String vehicleType = normalizeVehicleType(request.getVehicleType());
        ApartmentResponse apartment = ensureCanManageApartment(actor, request.getApartmentId());

        if (vehicleLimitRepository.existsByApartmentIdAndVehicleType(request.getApartmentId(), vehicleType)) {
            throw new InvalidVehicleStatusException("Vehicle limit already exists for apartment " + request.getApartmentId() + " and type " + vehicleType);
        }

        VehicleLimit limit = VehicleLimit.builder()
                .limitId(UUID.randomUUID())
                .apartmentId(request.getApartmentId())
                .vehicleType(vehicleType)
                .maxQuantity(request.getMaxQuantity())
                .build();
        return mapToResponse(vehicleLimitRepository.save(limit), apartment);
    }

    @Override
    @Transactional
    public VehicleLimitResponse updateLimit(CurrentUser actor, UUID limitId, VehicleLimitRequest request) {
        ensureManagerOrAdmin(actor);
        VehicleLimit limit = getLimitOrThrow(limitId);
        String vehicleType = normalizeVehicleType(request.getVehicleType());
        ApartmentResponse apartment = ensureCanManageApartment(actor, request.getApartmentId());

        if (vehicleLimitRepository.existsByApartmentIdAndVehicleTypeAndLimitIdNot(request.getApartmentId(), vehicleType, limitId)) {
            throw new InvalidVehicleStatusException("Vehicle limit already exists for apartment " + request.getApartmentId() + " and type " + vehicleType);
        }

        long approvedCount = vehicleRepository.countByApartmentIdAndTypeAndStatus(request.getApartmentId(), vehicleType, VehicleStatus.APPROVED);
        if (request.getMaxQuantity() < approvedCount) {
            throw new LimitExceededException("Max quantity cannot be lower than approved vehicle count: " + approvedCount);
        }

        limit.setApartmentId(request.getApartmentId());
        limit.setVehicleType(vehicleType);
        limit.setMaxQuantity(request.getMaxQuantity());
        return mapToResponse(vehicleLimitRepository.save(limit), apartment);
    }

    @Override
    @Transactional
    public void deleteLimit(CurrentUser actor, UUID limitId) {
        ensureManagerOrAdmin(actor);
        VehicleLimit limit = getLimitOrThrow(limitId);
        ensureCanManageApartment(actor, limit.getApartmentId());
        long approvedCount = vehicleRepository.countByApartmentIdAndTypeAndStatus(limit.getApartmentId(), limit.getVehicleType(), VehicleStatus.APPROVED);
        if (approvedCount > 0) {
            throw new LimitExceededException("Cannot delete limit while approved vehicles exist: " + approvedCount);
        }
        vehicleLimitRepository.delete(limit);
    }

    @Override
    public VehicleLimitResponse getLimitById(CurrentUser actor, UUID limitId) {
        VehicleLimit limit = getLimitOrThrow(limitId);
        ApartmentResponse apartment = ensureCanManageApartment(actor, limit.getApartmentId());
        return mapToResponse(limit, apartment);
    }

    @Override
    public List<VehicleLimitResponse> getLimits(CurrentUser actor, UUID apartmentId) {
        ensureManagerOrAdmin(actor);
        if (apartmentId != null) {
            ApartmentResponse apartment = ensureCanManageApartment(actor, apartmentId);
            return vehicleLimitRepository.findByApartmentIdOrderByVehicleTypeAsc(apartmentId)
                    .stream()
                    .map(limit -> mapToResponse(limit, apartment))
                    .toList();
        }

        if (RoleNames.ADMIN.equals(actor.getRoleName())) {
            return vehicleLimitRepository.findAll()
                    .stream()
                    .map(limit -> mapToResponse(limit, apartmentClient.getApartmentById(limit.getApartmentId())))
                    .toList();
        }

        if (actor.getBuildingId() == null) {
            throw new AccessDeniedException("Building scoped user does not have building context");
        }
        List<UUID> apartmentIds = apartmentClient.getApartmentsByBuildingId(actor.getBuildingId())
                .stream()
                .map(ApartmentResponse::getApartmentId)
                .toList();
        return vehicleLimitRepository.findAll()
                .stream()
                .filter(limit -> apartmentIds.contains(limit.getApartmentId()))
                .map(limit -> mapToResponse(limit, apartmentClient.getApartmentById(limit.getApartmentId())))
                .toList();
    }

    private VehicleLimit getLimitOrThrow(UUID limitId) {
        return vehicleLimitRepository.findById(limitId)
                .orElseThrow(() -> new ResourceNotFoundException("Vehicle limit not found: " + limitId));
    }

    private ApartmentResponse ensureCanManageApartment(CurrentUser actor, UUID apartmentId) {
        ApartmentResponse apartment = apartmentClient.getApartmentById(apartmentId);
        if (apartment == null) {
            throw new ResourceNotFoundException("Apartment not found: " + apartmentId);
        }
        if (RoleNames.ADMIN.equals(actor.getRoleName())) {
            return apartment;
        }
        if (!RoleNames.MANAGER.equals(actor.getRoleName()) && !RoleNames.STAFF.equals(actor.getRoleName())) {
            throw new AccessDeniedException("Only admin, manager or staff can manage vehicle limits");
        }
        if (actor.getBuildingId() == null || !actor.getBuildingId().equals(apartment.getBuildingId())) {
            throw new AccessDeniedException("You are not allowed to manage vehicle limits of another building");
        }
        return apartment;
    }

    private void ensureManagerOrAdmin(CurrentUser actor) {
        if (!RoleNames.ADMIN.equals(actor.getRoleName()) && !RoleNames.MANAGER.equals(actor.getRoleName()) && !RoleNames.STAFF.equals(actor.getRoleName())) {
            throw new AccessDeniedException("Only admin, manager or staff can manage vehicle limits");
        }
    }

    private String normalizeVehicleType(String type) {
        String normalizedType = type.trim().toUpperCase(Locale.ROOT);
        if (!VehicleType.isAllowed(normalizedType)) {
            throw new InvalidVehicleStatusException("Vehicle type must be CAR or MOTORBIKE");
        }
        return normalizedType;
    }

    private VehicleLimitResponse mapToResponse(VehicleLimit limit, ApartmentResponse apartment) {
        long approvedCount = vehicleRepository.countByApartmentIdAndTypeAndStatus(limit.getApartmentId(), limit.getVehicleType(), VehicleStatus.APPROVED);
        return VehicleLimitResponse.builder()
                .limitId(limit.getLimitId())
                .apartmentId(limit.getApartmentId())
                .buildingId(apartment == null ? null : apartment.getBuildingId())
                .vehicleType(limit.getVehicleType())
                .maxQuantity(limit.getMaxQuantity())
                .approvedVehicleCount(approvedCount)
                .build();
    }
}