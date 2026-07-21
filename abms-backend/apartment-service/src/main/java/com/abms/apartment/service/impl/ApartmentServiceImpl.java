package com.abms.apartment.service.impl;

import com.abms.apartment.dto.ApartmentResidentResponse;
import com.abms.apartment.dto.ApartmentResponse;
import com.abms.apartment.dto.BuildingRequest;
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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

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
    @Transactional
    public BuildingResponse createBuilding(BuildingRequest request) {
        String code = normalizeCode(request.getCode());
        if (buildingRepository.existsByCodeIgnoreCase(code)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Building code already exists: " + code);
        }

        Building building = Building.builder()
                .buildingId(UUID.randomUUID())
                .name(request.getName().trim())
                .code(code)
                .address(request.getAddress().trim())
                .floors(normalizeFloors(request.getFloors()))
                .build();
        return mapBuilding(buildingRepository.save(building));
    }

    @Override
    @Transactional
    public BuildingResponse updateBuilding(UUID buildingId, BuildingRequest request) {
        Building building = buildingRepository.findById(buildingId)
                .orElseThrow(() -> new ResourceNotFoundException("Building not found: " + buildingId));

        String code = normalizeCode(request.getCode());
        if (buildingRepository.existsByCodeIgnoreCaseAndBuildingIdNot(code, buildingId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Building code already exists: " + code);
        }

        building.setName(request.getName().trim());
        building.setCode(code);
        building.setAddress(request.getAddress().trim());
        building.setFloors(normalizeFloors(request.getFloors()));
        return mapBuilding(buildingRepository.save(building));
    }

    @Override
    @Transactional
    public void deleteBuilding(UUID buildingId) {
        Building building = buildingRepository.findById(buildingId)
                .orElseThrow(() -> new ResourceNotFoundException("Building not found: " + buildingId));

        if (apartmentRepository.existsByBuildingId(buildingId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Cannot delete building that still has apartments");
        }

        buildingRepository.delete(building);
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
                .floors(building.getFloors())
                .build();
    }

    private String normalizeCode(String code) {
        return code == null ? "" : code.trim().toUpperCase(Locale.ROOT);
    }

    private Integer normalizeFloors(Integer floors) {
        if (floors == null || floors < 0) {
            return 0;
        }
        return floors;
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

    // --- contracts listing and management ---
    @Override
    public java.util.List<com.abms.apartment.dto.ContractResponse> listContracts(String authorizationHeader, UUID buildingId) {
        java.util.List<com.abms.apartment.entity.Contract> contracts;
        if (buildingId != null) {
            // collect apartment ids under building
            var apartmentIds = apartmentRepository.findByBuildingIdOrderByRoomNumberAsc(buildingId)
                    .stream()
                    .map(Apartment::getApartmentId)
                    .toList();
            if (apartmentIds.isEmpty()) return java.util.List.of();
            contracts = contractRepository.findByApartmentIdInOrderByStartDateDesc(apartmentIds);
        } else {
            contracts = contractRepository.findAll();
        }

        // map and enrich
        return contracts.stream().map(c -> mapContract(c, authorizationHeader)).toList();
    }

    @Override
    public com.abms.apartment.dto.ContractResponse getContractById(String authorizationHeader, UUID contractId) {
        var c = contractRepository.findById(contractId).orElse(null);
        if (c == null) return null;
        return mapContract(c, authorizationHeader);
    }

    @Override
    @Transactional
    public com.abms.apartment.dto.ContractResponse renewContract(String authorizationHeader, java.util.UUID contractId, com.abms.apartment.dto.RenewContractRequest request) {
        var oldOpt = contractRepository.findById(contractId);
        if (oldOpt.isEmpty()) throw new ResourceNotFoundException("Contract not found: " + contractId);
        var old = oldOpt.get();

        // expire old
        old.setStatus("EXPIRED");
        contractRepository.save(old);

        // determine start date (day after old end date) or provided
        java.time.LocalDate start = request.getStartDate();
        if (start == null) {
            start = (old.getEndDate() == null) ? java.time.LocalDate.now() : old.getEndDate().plusDays(1);
        }

        // create new contract
        var newContract = com.abms.apartment.entity.Contract.builder()
                .contractId(UUID.randomUUID())
                .apartmentId(old.getApartmentId())
                .userId(old.getUserId())
                .contractType(old.getContractType() == null ? "TEMPORARY" : old.getContractType())
                .startDate(start)
                .endDate(request.getEndDate())
                .deposit(request.getDeposit())
                .status("ACTIVE")
                .build();
        contractRepository.save(newContract);

        return mapContract(newContract, authorizationHeader);
    }

    private com.abms.apartment.dto.ContractResponse mapContract(com.abms.apartment.entity.Contract contract, String authorizationHeader) {
        var resp = com.abms.apartment.dto.ContractResponse.builder()
                .contractId(contract.getContractId())
                .apartmentId(contract.getApartmentId())
                .userId(contract.getUserId())
                .contractType(contract.getContractType())
                .startDate(contract.getStartDate())
                .endDate(contract.getEndDate())
                .deposit(contract.getDeposit())
                .status(contract.getStatus())
                .build();

        // enrich with user info (try to forward authorization if provided)
        try {
            UserResponse user = null;
            try {
                user = authFeignClient.getUserById(contract.getUserId().toString(), authorizationHeader);
            } catch (RuntimeException e) {
                // fallback: try without header
                try { user = authFeignClient.getUserById(contract.getUserId().toString(), null); } catch (RuntimeException ignored) {}
            }
            if (user != null) {
                resp.setUserFullName(user.getFullName());
                resp.setUserEmail(user.getEmail());
                resp.setUserPhone(user.getPhone());
            }
        } catch (RuntimeException ignored) {
        }

        // apartment info
        try {
            var apt = apartmentRepository.findById(contract.getApartmentId()).orElse(null);
            if (apt != null) resp.setApartmentRoomNumber(apt.getRoomNumber());
        } catch (RuntimeException ignored) {
        }

        return resp;
    }

    private String normalizeValue(String value, String defaultValue) {
        if (value == null || value.isBlank()) {
            return defaultValue;
        }
        return value.trim().toUpperCase();
    }

    @Override
    public List<ApartmentResponse> getMyApartments(UUID userId) {
        List<UUID> apartmentIds = apartmentResidentRepository.findByUserIdAndStatusOrderByCreatedAtDesc(userId, ACTIVE)
                .stream()
                .map(ApartmentResident::getApartmentId)
                .distinct()
                .toList();

        if (apartmentIds.isEmpty()) {
            return List.of();
        }

        Map<UUID, ApartmentResponse> apartmentsById = new LinkedHashMap<>();
        apartmentRepository.findAllById(apartmentIds)
                .stream()
                .map(this::mapApartment)
                .forEach(apartment -> apartmentsById.put(apartment.getApartmentId(), apartment));

        return apartmentIds.stream()
                .map(apartmentsById::get)
                .filter(java.util.Objects::nonNull)
                .toList();
    }

    @Override
    public List<ApartmentResidentResponse> getResidentsByApartmentId(String authorizationHeader, UUID apartmentId) {
        apartmentRepository.findById(apartmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Apartment not found: " + apartmentId));

        return apartmentResidentRepository.findByApartmentIdOrderByCreatedAtAsc(apartmentId)
                .stream()
                .map(this::mapResident)
                .toList();
    }

    @Override
    public List<ApartmentResidentResponse> getResidentsByBuildingId(String authorizationHeader, UUID buildingId) {
        buildingRepository.findById(buildingId)
                .orElseThrow(() -> new ResourceNotFoundException("Building not found: " + buildingId));

        List<UUID> apartmentIds = apartmentRepository.findByBuildingIdOrderByRoomNumberAsc(buildingId)
                .stream()
                .map(Apartment::getApartmentId)
                .toList();

        if (apartmentIds.isEmpty()) {
            return List.of();
        }

        return apartmentResidentRepository.findByApartmentIdInAndStatusOrderByCreatedAtAsc(apartmentIds, ACTIVE)
                .stream()
                .map(this::mapResident)
                .toList();
    }
}