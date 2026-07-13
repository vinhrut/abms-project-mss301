package com.abms.apartment.service.impl;

import com.abms.apartment.dto.ApartmentResidentResponse;
import com.abms.apartment.dto.ApartmentResponse;
import com.abms.apartment.dto.BuildingResponse;
import com.abms.apartment.dto.ResidentRegistrationRequest;
import com.abms.apartment.entity.Apartment;
import com.abms.apartment.entity.ApartmentResident;
import com.abms.apartment.entity.Building;
import com.abms.apartment.exception.ResourceNotFoundException;
import com.abms.apartment.repository.ApartmentRepository;
import com.abms.apartment.repository.ApartmentResidentRepository;
import com.abms.apartment.repository.BuildingRepository;
import com.abms.apartment.service.ApartmentService;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ApartmentServiceImpl implements ApartmentService {

    private static final String ACTIVE = "ACTIVE";
    private static final String PENDING_APPROVAL = "PENDING_APPROVAL";
    private static final String REJECTED = "REJECTED";

    private final BuildingRepository buildingRepository;
    private final ApartmentRepository apartmentRepository;
    private final ApartmentResidentRepository apartmentResidentRepository;

    @Override
    public List<BuildingResponse> getAllBuildings() {
        return buildingRepository.findAllByOrderByNameAsc()
                .stream()
                .map(this::mapBuilding)
                .toList();
    }

    @Override
    public BuildingResponse getBuildingById(UUID buildingId) {
        return mapBuilding(buildingRepository.findById(buildingId)
                .orElseThrow(() -> new ResourceNotFoundException("Building not found: " + buildingId)));
    }

    @Override
    public ApartmentResponse getApartmentById(UUID apartmentId) {
        return mapApartment(apartmentRepository.findById(apartmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Apartment not found: " + apartmentId)));
    }

    @Override
    public List<ApartmentResponse> getAllApartments() {
        return apartmentRepository.findAllByOrderByRoomNumberAsc()
                .stream()
                .map(this::mapApartment)
                .toList();
    }

    @Override
    public List<ApartmentResponse> getApartmentsByBuildingId(UUID buildingId) {
        buildingRepository.findById(buildingId)
                .orElseThrow(() -> new ResourceNotFoundException("Building not found: " + buildingId));

        return apartmentRepository.findByBuildingIdOrderByRoomNumberAsc(buildingId)
                .stream()
                .map(this::mapApartment)
                .toList();
    }

    @Override
    @Transactional
    public ApartmentResidentResponse createResidentRegistration(ResidentRegistrationRequest request) {
        Apartment apartment = apartmentRepository.findById(request.getApartmentId())
                .orElseThrow(() -> new ResourceNotFoundException("Apartment not found: " + request.getApartmentId()));

        ApartmentResident apartmentResident = apartmentResidentRepository
                .findFirstByUserIdOrderByCreatedAtDesc(request.getUserId())
                .orElse(ApartmentResident.builder()
                        .residentId(UUID.randomUUID())
                        .userId(request.getUserId())
                        .createdAt(LocalDateTime.now())
                        .build());

        apartmentResident.setApartmentId(apartment.getApartmentId());
        apartmentResident.setRelationship(normalizeValue(request.getRelationship(), "OWNER"));
        apartmentResident.setResidenceType(normalizeValue(request.getResidenceType(), "PERMANENT"));
        apartmentResident.setStatus(PENDING_APPROVAL);
        apartmentResident.setRejectedAt(null);
        apartmentResident.setApprovedAt(null);

        return mapResident(apartmentResidentRepository.save(apartmentResident));
    }

    @Override
    public List<ApartmentResidentResponse> getPendingResidentRegistrations() {
        return apartmentResidentRepository.findByStatusOrderByCreatedAtAsc(PENDING_APPROVAL)
                .stream()
                .map(this::mapResident)
                .toList();
    }

    @Override
    @Transactional
    public ApartmentResidentResponse approveResidentRegistration(UUID userId) {
        ApartmentResident apartmentResident = apartmentResidentRepository
                .findFirstByUserIdOrderByCreatedAtDesc(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Resident registration not found: " + userId));
        apartmentResident.setStatus(ACTIVE);
        apartmentResident.setApprovedAt(LocalDateTime.now());
        apartmentResident.setRejectedAt(null);
        return mapResident(apartmentResidentRepository.save(apartmentResident));
    }

    @Override
    @Transactional
    public ApartmentResidentResponse rejectResidentRegistration(UUID userId) {
        ApartmentResident apartmentResident = apartmentResidentRepository
                .findFirstByUserIdOrderByCreatedAtDesc(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Resident registration not found: " + userId));
        apartmentResident.setStatus(REJECTED);
        apartmentResident.setRejectedAt(LocalDateTime.now());
        apartmentResident.setApprovedAt(null);
        return mapResident(apartmentResidentRepository.save(apartmentResident));
    }

    @Override
    public ApartmentResidentResponse getActiveResidenceByUserId(UUID userId) {
        ApartmentResident apartmentResident = apartmentResidentRepository
                .findFirstByUserIdAndStatusOrderByCreatedAtDesc(userId, ACTIVE)
                .orElseThrow(() -> new ResourceNotFoundException("Active residence not found for user: " + userId));
        return mapResident(apartmentResident);
    }

    private ApartmentResponse mapApartment(Apartment apartment) {
        return ApartmentResponse.builder()
                .apartmentId(apartment.getApartmentId())
                .buildingId(apartment.getBuildingId())
                .roomNumber(apartment.getRoomNumber())
                .floor(apartment.getFloor())
                .area(apartment.getArea())
                .status(apartment.getStatus())
                .build();
    }

    private BuildingResponse mapBuilding(Building building) {
        return BuildingResponse.builder()
                .buildingId(building.getBuildingId())
                .name(building.getName())
                .code(building.getCode())
                .address(building.getAddress())
                .build();
    }

    private ApartmentResidentResponse mapResident(ApartmentResident apartmentResident) {
        return ApartmentResidentResponse.builder()
                .residentId(apartmentResident.getResidentId())
                .apartmentId(apartmentResident.getApartmentId())
                .userId(apartmentResident.getUserId())
                .relationship(apartmentResident.getRelationship())
                .residenceType(apartmentResident.getResidenceType())
                .status(apartmentResident.getStatus())
                .createdAt(apartmentResident.getCreatedAt())
                .approvedAt(apartmentResident.getApprovedAt())
                .rejectedAt(apartmentResident.getRejectedAt())
                .build();
    }

    private String normalizeValue(String value, String defaultValue) {
        if (value == null || value.isBlank()) {
            return defaultValue;
        }
        return value.trim().toUpperCase();
    }
}