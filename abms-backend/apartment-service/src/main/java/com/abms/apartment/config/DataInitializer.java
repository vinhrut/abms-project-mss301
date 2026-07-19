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

@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private static final UUID BUILDING_A = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb");
    private static final UUID BUILDING_B = UUID.fromString("cccccccc-cccc-cccc-cccc-cccccccccccc");
    private static final UUID BUILDING_C = UUID.fromString("dddddddd-dddd-dddd-dddd-dddddddddddd");
    private static final String APARTMENT_A101 = "aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa1";
    private static final String APARTMENT_A102 = "aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa2";
    private static final String APARTMENT_B101 = "bbbbbbbb-aaaa-aaaa-aaaa-aaaaaaaaaaa1";
    private static final String RESIDENT_A101 = "00000000-0000-0000-0000-000000001101";
    private static final String RESIDENT_A102 = "00000000-0000-0000-0000-000000001102";
    private static final String RESIDENT_B101 = "00000000-0000-0000-0000-000000001201";

    private final BuildingRepository buildingRepository;
    private final ApartmentRepository apartmentRepository;
    private final ApartmentResidentRepository apartmentResidentRepository;

    @Override
    public void run(String... args) {
        List<Building> buildings = List.of(
                Building.builder().buildingId(BUILDING_A).name("Building A").code("A").address("123 Main Street").build(),
                Building.builder().buildingId(BUILDING_B).name("Building B").code("B").address("456 Second Street").build(),
                Building.builder().buildingId(BUILDING_C).name("Building C").code("C").address("789 Third Street").build());

        cleanupBuildings(buildings);
        buildings.forEach(buildingRepository::save);

        List<Apartment> apartments = List.of(
                apartment("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa1", BUILDING_A, "A-101", 1, "72.50", "OCCUPIED"),
                apartment("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa2", BUILDING_A, "A-102", 1, "68.00", "OCCUPIED"),
                apartment("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa3", BUILDING_A, "A-201", 2, "80.00", "VACANT"),
                apartment("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa4", BUILDING_A, "A-202", 2, "77.00", "OCCUPIED"),
                apartment("bbbbbbbb-aaaa-aaaa-aaaa-aaaaaaaaaaa1", BUILDING_B, "B-101", 1, "70.00", "OCCUPIED"),
                apartment("bbbbbbbb-aaaa-aaaa-aaaa-aaaaaaaaaaa2", BUILDING_B, "B-102", 1, "69.50", "VACANT"),
                apartment("bbbbbbbb-aaaa-aaaa-aaaa-aaaaaaaaaaa3", BUILDING_B, "B-201", 2, "82.00", "OCCUPIED"),
                apartment("bbbbbbbb-aaaa-aaaa-aaaa-aaaaaaaaaaa4", BUILDING_B, "B-202", 2, "75.25", "VACANT"),
                apartment("cccccccc-aaaa-aaaa-aaaa-aaaaaaaaaaa1", BUILDING_C, "C-101", 1, "66.00", "VACANT"),
                apartment("cccccccc-aaaa-aaaa-aaaa-aaaaaaaaaaa2", BUILDING_C, "C-102", 1, "71.00", "OCCUPIED"),
                apartment("cccccccc-aaaa-aaaa-aaaa-aaaaaaaaaaa3", BUILDING_C, "C-201", 2, "84.00", "OCCUPIED"),
                apartment("cccccccc-aaaa-aaaa-aaaa-aaaaaaaaaaa4", BUILDING_C, "C-202", 2, "79.00", "VACANT"));

        cleanupApartments(apartments);
        apartments.forEach(apartmentRepository::save);

        List<ApartmentResident> residents = List.of(
                resident("eeeeeeee-eeee-eeee-eeee-eeeeeeeeeee1", APARTMENT_A101, RESIDENT_A101, "OWNER", "PERMANENT", 10),
                resident("eeeeeeee-eeee-eeee-eeee-eeeeeeeeeee2", APARTMENT_A102, RESIDENT_A102, "TENANT", "TEMPORARY", 8),
                resident("eeeeeeee-eeee-eeee-eeee-eeeeeeeeeee3", APARTMENT_B101, RESIDENT_B101, "OWNER", "PERMANENT", 6));

        residents.forEach(apartmentResidentRepository::save);
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

    private ApartmentResident resident(String residentId, String apartmentId, String userId, String relationship, String residenceType, int createdDaysAgo) {
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