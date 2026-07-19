package com.abms.apartment.service.impl;

import com.abms.apartment.dto.ApartmentResidentResponse;
import com.abms.apartment.dto.ApartmentResponse;
import com.abms.apartment.dto.BuildingResponse;
import com.abms.apartment.dto.ResidentRegistrationRequest;
import com.abms.apartment.entity.Apartment;
import com.abms.apartment.entity.ApartmentResident;
import com.abms.apartment.entity.Building;
import com.abms.apartment.exception.ResourceNotFoundException;
import com.abms.apartment.client.AuthFeignClient;
import com.abms.apartment.dto.UserResponse;
import com.abms.apartment.repository.ApartmentRepository;
import com.abms.apartment.repository.ApartmentResidentRepository;
import com.abms.apartment.repository.BuildingRepository;
import com.abms.apartment.repository.ContractRepository;
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
    private final ContractRepository contractRepository;
    private final AuthFeignClient authFeignClient;

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
    public List<ApartmentResidentResponse> getResidentsByApartmentId(String authorizationHeader, UUID apartmentId) {
        apartmentRepository.findById(apartmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Apartment not found: " + apartmentId));

        List<ApartmentResidentResponse> residents = apartmentResidentRepository.findByApartmentIdOrderByCreatedAtAsc(apartmentId)
                .stream()
                .map(this::mapResident)
                .toList();

        enrichResidentsWithUserDetails(authorizationHeader, residents);
        return residents;
    }


    private void enrichResidentsWithUserDetails(String authorizationHeader, List<ApartmentResidentResponse> residents) {
        if (residents.isEmpty()) {
            return;
        }

        var userIds = residents.stream()
                .map(ApartmentResidentResponse::getUserId)
                .filter(java.util.Objects::nonNull)
                .distinct()
                .toList();

        for (var userId : userIds) {
            try {
                // pass string to Feign client to avoid UUID conversion issues
                UserResponse user = authFeignClient.getUserById(userId.toString(), authorizationHeader);
                if (user == null) {
                    continue;
                }

                for (ApartmentResidentResponse resident : residents) {
                    if (userId.equals(resident.getUserId())) {
                        resident.setUserFullName(user.getFullName());
                        resident.setUserEmail(user.getEmail());
                        resident.setUserPhone(user.getPhone());
                        resident.setUserIdCard(user.getIdCard());
                        resident.setUserRoleName(user.getRoleName());
                    }
                }
            } catch (RuntimeException ignored) {
                // ignore failures for individual user lookups
            }
        }
    }

    @Override
    public List<ApartmentResidentResponse> getResidentsByBuildingId(String authorizationHeader, UUID buildingId) {
        buildingRepository.findById(buildingId)
                .orElseThrow(() -> new ResourceNotFoundException("Building not found: " + buildingId));

        var apartmentIds = apartmentRepository.findByBuildingIdOrderByRoomNumberAsc(buildingId)
                .stream()
                .map(Apartment::getApartmentId)
                .toList();

        if (apartmentIds.isEmpty()) {
            return List.of();
        }

        List<ApartmentResidentResponse> residents = apartmentResidentRepository.findByApartmentIdInAndStatusOrderByCreatedAtAsc(apartmentIds, ACTIVE)
                .stream()
                .map(this::mapResident)
                .toList();

        enrichResidentsWithUserDetails(authorizationHeader, residents);
        return residents;
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
        var builder = ApartmentResidentResponse.builder()
                .residentId(apartmentResident.getResidentId())
                .apartmentId(apartmentResident.getApartmentId())
                .userId(apartmentResident.getUserId())
                .relationship(apartmentResident.getRelationship())
                .residenceType(apartmentResident.getResidenceType())
                .status(apartmentResident.getStatus())
                .createdAt(apartmentResident.getCreatedAt())
                .approvedAt(apartmentResident.getApprovedAt())
                .rejectedAt(apartmentResident.getRejectedAt());

        // attach latest contract info if available
        try {
            var contractOpt = contractRepository.findFirstByApartmentIdAndUserIdOrderByStartDateDesc(
                    apartmentResident.getApartmentId(), apartmentResident.getUserId());
            if (contractOpt.isPresent()) {
                var contract = contractOpt.get();
                builder.contractId(contract.getContractId())
                        .contractType(contract.getContractType())
                        .contractStartDate(contract.getStartDate())
                        .contractEndDate(contract.getEndDate())
                        .contractDeposit(contract.getDeposit())
                        .contractStatus(contract.getStatus());
            }
        } catch (RuntimeException ignored) {
            // ignore contract lookup failures
        }

        return builder.build();
    }

    @Override
    @Transactional
    public ApartmentResidentResponse renewResidentContract(String authorizationHeader, UUID apartmentId, UUID userId) {
        // ensure apartment exists
        apartmentRepository.findById(apartmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Apartment not found: " + apartmentId));

        // find resident
        ApartmentResident resident = apartmentResidentRepository
                .findFirstByUserIdOrderByCreatedAtDesc(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Resident not found: " + userId));

        // find active contract
        var contractOpt = contractRepository.findFirstByApartmentIdAndUserIdAndStatusOrderByStartDateDesc(apartmentId, userId, ACTIVE);
        java.time.LocalDate now = java.time.LocalDate.now();
        if (contractOpt.isPresent()) {
            var contract = contractOpt.get();
            // extend by 1 month
            var newEnd = (contract.getEndDate() == null || contract.getEndDate().isBefore(now)) ? now.plusMonths(1) : contract.getEndDate().plusMonths(1);
            contract.setEndDate(newEnd);
            contractRepository.save(contract);
            // return updated resident response
            return mapResident(resident);
        } else {
            // create a new temporary contract for 1 month
            var newContract = com.abms.apartment.entity.Contract.builder()
                    .contractId(UUID.randomUUID())
                    .apartmentId(apartmentId)
                    .userId(userId)
                    .contractType("TEMPORARY")
                    .startDate(now)
                    .endDate(now.plusMonths(1))
                    .deposit(java.math.BigDecimal.ZERO)
                    .status(ACTIVE)
                    .build();
            contractRepository.save(newContract);
            return mapResident(resident);
        }
    }

    @Override
    @Transactional
    public ApartmentResidentResponse removeResidentFromApartment(String authorizationHeader, UUID apartmentId, UUID userId) {
        // ensure apartment exists
        apartmentRepository.findById(apartmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Apartment not found: " + apartmentId));

        ApartmentResident resident = apartmentResidentRepository
                .findFirstByUserIdOrderByCreatedAtDesc(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Resident not found: " + userId));

        // soft-delete resident
        resident.setStatus("INACTIVE");
        apartmentResidentRepository.save(resident);

        // mark contract inactive if exists
        var contractOpt = contractRepository.findFirstByApartmentIdAndUserIdOrderByStartDateDesc(apartmentId, userId);
        contractOpt.ifPresent(c -> {
            c.setStatus("INACTIVE");
            contractRepository.save(c);
        });

        return mapResident(resident);
    }

    private String normalizeValue(String value, String defaultValue) {
        if (value == null || value.isBlank()) {
            return defaultValue;
        }
        return value.trim().toUpperCase();
    }
}