package com.abms.apartment.config;

import com.abms.apartment.entity.Apartment;
import com.abms.apartment.entity.ApartmentResident;
import com.abms.apartment.entity.Building;
import com.abms.apartment.repository.ApartmentRepository;
import com.abms.apartment.repository.ApartmentResidentRepository;
import com.abms.apartment.repository.BuildingRepository;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    // Keep in sync with auth-service / vehicle-service / notification-service seeds.
    private static final UUID BUILDING_A = UUID.fromString("7f3a9c2e-1b4d-4e8f-a6c0-92d5e8b1f4a3");
    private static final UUID BUILDING_B = UUID.fromString("8e4b0d3f-2c5e-4f9a-b7d1-a3e6f9c2d5b4");
    private static final UUID BUILDING_C = UUID.fromString("9f5c1e4a-3d6f-4a0b-c8e2-b4f7a0d3e6c5");

    private static final String APARTMENT_A101 = "a101b202-c303-4d04-8e05-f606a707b808";
    private static final String APARTMENT_A102 = "a102b203-c304-4d05-8e06-f607a708b809";
    private static final String APARTMENT_A201 = "a201b302-c403-4d04-8e05-f606a707b808";
    private static final String APARTMENT_A202 = "a202b303-c404-4d05-8e06-f607a708b809";
    private static final String APARTMENT_B101 = "b101c202-d303-4e04-8f05-a606b707c808";
    private static final String APARTMENT_B102 = "b102c203-d304-4e05-8f06-a607b708c809";
    private static final String APARTMENT_B201 = "b201c302-d403-4e04-8f05-a606b707c808";
    private static final String APARTMENT_B202 = "b202c303-d404-4e05-8f06-a607b708c809";
    private static final String APARTMENT_C101 = "c101d202-e303-4f04-8a05-b606c707d808";
    private static final String APARTMENT_C102 = "c102d203-e304-4f05-8a06-b607c708d809";
    private static final String APARTMENT_C201 = "c201d302-e403-4f04-8a05-b606c707d808";
    private static final String APARTMENT_C202 = "c202d303-e404-4f05-8a06-b607c708d809";

    private static final String RESIDENT_A101 = "4d5e6f70-8192-4abc-d345-e6f7890a1b2c";
    private static final String RESIDENT_A102 = "5e6f7081-92a3-4bcd-e456-f7890a1b2c3d";
    private static final String RESIDENT_B101 = "6f708192-a3b4-4cde-f567-890a1b2c3d4e";

    private static final String LINK_A101 = "e1f2a3b4-c5d6-4e7f-8a9b-0c1d2e3f4a01";
    private static final String LINK_A102 = "e1f2a3b4-c5d6-4e7f-8a9b-0c1d2e3f4a02";
    private static final String LINK_B101 = "e1f2a3b4-c5d6-4e7f-8a9b-0c1d2e3f4a03";

    private final BuildingRepository buildingRepository;
    private final ApartmentRepository apartmentRepository;
    private final ApartmentResidentRepository apartmentResidentRepository;

    @Override
    @Transactional
    public void run(String... args) {
        List<Building> buildings = List.of(
                Building.builder().buildingId(BUILDING_A).name("Building A").code("A").address("123 Main Street").floors(2).build(),
                Building.builder().buildingId(BUILDING_B).name("Building B").code("B").address("456 Second Street").floors(2).build(),
                Building.builder().buildingId(BUILDING_C).name("Building C").code("C").address("789 Third Street").floors(2).build());

        cleanupBuildings(buildings);
        buildings.forEach(buildingRepository::save);

        List<Apartment> apartments = List.of(
                apartment(APARTMENT_A101, BUILDING_A, "A-101", 1, "72.50", "OCCUPIED"),
                apartment(APARTMENT_A102, BUILDING_A, "A-102", 1, "68.00", "OCCUPIED"),
                apartment(APARTMENT_A201, BUILDING_A, "A-201", 2, "80.00", "VACANT"),
                apartment(APARTMENT_A202, BUILDING_A, "A-202", 2, "77.00", "OCCUPIED"),
                apartment(APARTMENT_B101, BUILDING_B, "B-101", 1, "70.00", "OCCUPIED"),
                apartment(APARTMENT_B102, BUILDING_B, "B-102", 1, "69.50", "VACANT"),
                apartment(APARTMENT_B201, BUILDING_B, "B-201", 2, "82.00", "OCCUPIED"),
                apartment(APARTMENT_B202, BUILDING_B, "B-202", 2, "75.25", "VACANT"),
                apartment(APARTMENT_C101, BUILDING_C, "C-101", 1, "66.00", "VACANT"),
                apartment(APARTMENT_C102, BUILDING_C, "C-102", 1, "71.00", "OCCUPIED"),
                apartment(APARTMENT_C201, BUILDING_C, "C-201", 2, "84.00", "OCCUPIED"),
                apartment(APARTMENT_C202, BUILDING_C, "C-202", 2, "79.00", "VACANT"));

        cleanupApartments(apartments);
        apartments.forEach(apartmentRepository::save);

        List<ApartmentResident> residents = List.of(
                resident(LINK_A101, APARTMENT_A101, RESIDENT_A101, "OWNER", "PERMANENT", 10),
                resident(LINK_A102, APARTMENT_A102, RESIDENT_A102, "TENANT", "TEMPORARY", 8),
                resident(LINK_B101, APARTMENT_B101, RESIDENT_B101, "OWNER", "PERMANENT", 6));

        cleanupResidents(residents);
        residents.forEach(this::upsertResident);
    }

    private void cleanupBuildings(List<Building> seedBuildings) {
        Set<UUID> allowedBuildingIds = new HashSet<>();
        seedBuildings.stream().map(Building::getBuildingId).forEach(allowedBuildingIds::add);

        buildingRepository.findAll().stream()
                .filter(existingBuilding -> !allowedBuildingIds.contains(existingBuilding.getBuildingId()))
                .forEach(buildingRepository::delete);
    }

    private void cleanupApartments(List<Apartment> seedApartments) {
        Set<UUID> allowedApartmentIds = new HashSet<>();
        seedApartments.stream().map(Apartment::getApartmentId).forEach(allowedApartmentIds::add);

        apartmentRepository.findAll().stream()
                .filter(existingApartment -> !allowedApartmentIds.contains(existingApartment.getApartmentId()))
                .forEach(apartmentRepository::delete);
    }

    private void cleanupResidents(List<ApartmentResident> seedResidents) {
        Set<UUID> allowedResidentIds = new HashSet<>();
        Set<UUID> seedUserIds = new HashSet<>();
        seedResidents.forEach(seed -> {
            allowedResidentIds.add(seed.getResidentId());
            seedUserIds.add(seed.getUserId());
        });

        apartmentResidentRepository.findAll().stream()
                .filter(existing -> !allowedResidentIds.contains(existing.getResidentId())
                        || (seedUserIds.contains(existing.getUserId())
                                && !allowedResidentIds.contains(existing.getResidentId())))
                .forEach(apartmentResidentRepository::delete);

        apartmentResidentRepository.findAll().stream()
                .filter(existing -> seedUserIds.contains(existing.getUserId())
                        && !allowedResidentIds.contains(existing.getResidentId()))
                .forEach(apartmentResidentRepository::delete);
    }

    private void upsertResident(ApartmentResident seed) {
        apartmentResidentRepository.findById(seed.getResidentId()).ifPresentOrElse(existing -> {
            existing.setApartmentId(seed.getApartmentId());
            existing.setUserId(seed.getUserId());
            existing.setRelationship(seed.getRelationship());
            existing.setResidenceType(seed.getResidenceType());
            existing.setStatus(seed.getStatus());
            existing.setApprovedAt(seed.getApprovedAt());
            existing.setRejectedAt(null);
            apartmentResidentRepository.save(existing);
        }, () -> apartmentResidentRepository.save(seed));
    }

    private Apartment apartment(String apartmentId, UUID buildingId, String roomNumber, int floor, String area, String status) {
        return Apartment.builder()
                .apartmentId(UUID.fromString(apartmentId))
                .buildingId(buildingId)
                .roomNumber(roomNumber)
                .floor(floor)
                .area(new BigDecimal(area))
                .status(status)
                .build();
    }

    private ApartmentResident resident(
            String residentId,
            String apartmentId,
            String userId,
            String relationship,
            String residenceType,
            int createdDaysAgo) {
        LocalDateTime createdAt = LocalDateTime.now().minusDays(createdDaysAgo);
        return ApartmentResident.builder()
                .residentId(UUID.fromString(residentId))
                .apartmentId(UUID.fromString(apartmentId))
                .userId(UUID.fromString(userId))
                .relationship(relationship)
                .residenceType(residenceType)
                .status("ACTIVE")
                .createdAt(createdAt)
                .approvedAt(createdAt.plusHours(12))
                .rejectedAt(null)
                .build();
    }
}
