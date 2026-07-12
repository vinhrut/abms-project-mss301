package com.abms.vehicle.service.impl;

import com.abms.vehicle.client.ApartmentClient;
import com.abms.vehicle.dto.ApartmentResidentResponse;
import com.abms.vehicle.dto.VehicleRequest;
import com.abms.vehicle.dto.VehicleResponse;
import com.abms.vehicle.entity.Vehicle;
import com.abms.vehicle.entity.VehicleLimit;
import com.abms.vehicle.exception.DuplicateLicensePlateException;
import com.abms.vehicle.exception.InvalidVehicleStatusException;
import com.abms.vehicle.exception.LimitExceededException;
import com.abms.vehicle.exception.ResourceNotFoundException;
import com.abms.vehicle.repository.VehicleLimitRepository;
import com.abms.vehicle.repository.VehicleRepository;
import com.abms.vehicle.service.VehicleService;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class VehicleServiceImpl implements VehicleService {

    private static final String PENDING = "PENDING";
    private static final String APPROVED = "APPROVED";
    private static final String REJECTED = "REJECTED";
    private static final String INACTIVE = "INACTIVE";

    private final VehicleRepository vehicleRepository;
    private final VehicleLimitRepository vehicleLimitRepository;
    private final ApartmentClient apartmentClient;

    @Override
    public VehicleResponse registerVehicle(VehicleRequest request) {
        String normalizedLicensePlate = request.getLicensePlate().trim().toUpperCase(Locale.ROOT);
        String normalizedType = request.getType().trim().toUpperCase(Locale.ROOT);

        validateApartment(request.getApartmentId());
        validateResidentResidence(request.getOwnerId(), request.getApartmentId());

        if (vehicleRepository.existsByLicensePlate(normalizedLicensePlate)) {
            throw new DuplicateLicensePlateException("License plate already exists: " + normalizedLicensePlate);
        }

        VehicleLimit vehicleLimit = vehicleLimitRepository
                .findByApartmentIdAndVehicleType(request.getApartmentId(), normalizedType)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Vehicle limit not found for apartment " + request.getApartmentId() + " and type " + normalizedType));

        long currentQuantity = vehicleRepository.countByApartmentIdAndTypeAndStatus(
                request.getApartmentId(), normalizedType, APPROVED);
        if (currentQuantity >= vehicleLimit.getMaxQuantity()) {
            throw new LimitExceededException("Vehicle limit exceeded for apartment " + request.getApartmentId()
                    + " and type " + normalizedType);
        }

        Vehicle vehicle = Vehicle.builder()
                .vehicleId(UUID.randomUUID())
                .apartmentId(request.getApartmentId())
                .ownerId(request.getOwnerId())
                .licensePlate(normalizedLicensePlate)
                .type(normalizedType)
                .brand(request.getBrand().trim())
                .status(PENDING)
                .build();

        return mapToResponse(vehicleRepository.save(vehicle));
    }

    @Override
    public VehicleResponse updateVehicle(UUID vehicleId, VehicleRequest request) {
        Vehicle vehicle = getVehicleOrThrow(vehicleId);

        String normalizedLicensePlate = request.getLicensePlate().trim().toUpperCase(Locale.ROOT);
        String normalizedType = request.getType().trim().toUpperCase(Locale.ROOT);

        validateApartment(request.getApartmentId());
        validateResidentResidence(request.getOwnerId(), request.getApartmentId());

        if (vehicleRepository.existsByLicensePlateAndVehicleIdNot(normalizedLicensePlate, vehicleId)) {
            throw new DuplicateLicensePlateException("License plate already exists: " + normalizedLicensePlate);
        }

        if (!vehicle.getApartmentId().equals(request.getApartmentId())
                || !vehicle.getType().equalsIgnoreCase(normalizedType)) {
            VehicleLimit vehicleLimit = vehicleLimitRepository
                    .findByApartmentIdAndVehicleType(request.getApartmentId(), normalizedType)
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Vehicle limit not found for apartment " + request.getApartmentId() + " and type " + normalizedType));

            long approvedCount = vehicleRepository.countByApartmentIdAndTypeAndStatus(
                    request.getApartmentId(), normalizedType, APPROVED);

            if (APPROVED.equalsIgnoreCase(vehicle.getStatus()) && approvedCount >= vehicleLimit.getMaxQuantity()) {
                throw new LimitExceededException("Vehicle limit exceeded for apartment " + request.getApartmentId()
                        + " and type " + normalizedType);
            }
        }

        vehicle.setApartmentId(request.getApartmentId());
        vehicle.setOwnerId(request.getOwnerId());
        vehicle.setLicensePlate(normalizedLicensePlate);
        vehicle.setType(normalizedType);
        vehicle.setBrand(request.getBrand().trim());

        return mapToResponse(vehicleRepository.save(vehicle));
    }

    @Override
    public List<VehicleResponse> getVehiclesByApartmentId(UUID apartmentId) {
        return vehicleRepository.findByApartmentId(apartmentId)
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Override
    public List<VehicleResponse> getVehiclesByOwnerId(UUID ownerId) {
        return vehicleRepository.findByOwnerId(ownerId)
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Override
    public List<VehicleResponse> getAllVehicles() {
        return vehicleRepository.findAllByOrderByStatusAscLicensePlateAsc()
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Override
    public VehicleResponse approveVehicle(UUID vehicleId) {
        Vehicle vehicle = getVehicleOrThrow(vehicleId);
        if (!PENDING.equalsIgnoreCase(vehicle.getStatus())) {
            throw new InvalidVehicleStatusException("Only pending vehicles can be approved");
        }

        long approvedCount = vehicleRepository.countByApartmentIdAndTypeAndStatus(
                vehicle.getApartmentId(), vehicle.getType(), APPROVED);
        VehicleLimit vehicleLimit = vehicleLimitRepository
                .findByApartmentIdAndVehicleType(vehicle.getApartmentId(), vehicle.getType())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Vehicle limit not found for apartment " + vehicle.getApartmentId() + " and type " + vehicle.getType()));

        if (approvedCount >= vehicleLimit.getMaxQuantity()) {
            throw new LimitExceededException("Vehicle limit exceeded for apartment " + vehicle.getApartmentId()
                    + " and type " + vehicle.getType());
        }

        vehicle.setStatus(APPROVED);
        return mapToResponse(vehicleRepository.save(vehicle));
    }

    @Override
    public VehicleResponse rejectVehicle(UUID vehicleId) {
        Vehicle vehicle = getVehicleOrThrow(vehicleId);
        if (!PENDING.equalsIgnoreCase(vehicle.getStatus())) {
            throw new InvalidVehicleStatusException("Only pending vehicles can be rejected");
        }

        vehicle.setStatus(REJECTED);
        return mapToResponse(vehicleRepository.save(vehicle));
    }

    private Vehicle getVehicleOrThrow(UUID vehicleId) {
        return vehicleRepository.findById(vehicleId)
                .orElseThrow(() -> new ResourceNotFoundException("Vehicle not found: " + vehicleId));
    }

    private void validateApartment(UUID apartmentId) {
        String apartmentStatus = apartmentClient.getApartmentById(apartmentId).getStatus();
        if (apartmentStatus == null || INACTIVE.equalsIgnoreCase(apartmentStatus)) {
            throw new ResourceNotFoundException("Apartment is not active: " + apartmentId);
        }
    }

    private void validateResidentResidence(UUID ownerId, UUID apartmentId) {
        ApartmentResidentResponse residence = apartmentClient.getActiveResidenceByUserId(ownerId);
        if (residence == null || !apartmentId.equals(residence.getApartmentId())) {
            throw new ResourceNotFoundException("Resident does not belong to apartment: " + apartmentId);
        }
    }

    private VehicleResponse mapToResponse(Vehicle vehicle) {
        return VehicleResponse.builder()
                .vehicleId(vehicle.getVehicleId())
                .apartmentId(vehicle.getApartmentId())
                .ownerId(vehicle.getOwnerId())
                .licensePlate(vehicle.getLicensePlate())
                .type(vehicle.getType())
                .brand(vehicle.getBrand())
                .status(vehicle.getStatus())
                .build();
    }
}