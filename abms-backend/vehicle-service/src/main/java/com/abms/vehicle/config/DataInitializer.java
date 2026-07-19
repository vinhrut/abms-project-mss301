package com.abms.vehicle.config;

import com.abms.vehicle.entity.Vehicle;
import com.abms.vehicle.entity.VehicleLimit;
import com.abms.vehicle.repository.VehicleLimitRepository;
import com.abms.vehicle.repository.VehicleRepository;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private static final UUID APARTMENT_A101 = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa1");
    private static final UUID APARTMENT_A102 = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa2");
    private static final UUID APARTMENT_B101 = UUID.fromString("bbbbbbbb-aaaa-aaaa-aaaa-aaaaaaaaaaa1");
    private static final UUID RESIDENT_A101 = UUID.fromString("00000000-0000-0000-0000-000000001101");
    private static final UUID RESIDENT_A102 = UUID.fromString("00000000-0000-0000-0000-000000001102");
    private static final UUID RESIDENT_B101 = UUID.fromString("00000000-0000-0000-0000-000000001201");

    private final VehicleLimitRepository vehicleLimitRepository;
    private final VehicleRepository vehicleRepository;

    @Override
    public void run(String... args) {
        seedVehicleLimits();
        seedVehicles();
    }

    private void seedVehicleLimits() {
        List<VehicleLimit> limits = List.of(
                limit("c1111111-1111-1111-1111-111111111111", APARTMENT_A101, "MOTORBIKE", 2),
                limit("c2222222-2222-2222-2222-222222222222", APARTMENT_A101, "CAR", 1),
                limit("c3333333-3333-3333-3333-333333333333", APARTMENT_A102, "MOTORBIKE", 2),
                limit("c4444444-4444-4444-4444-444444444444", APARTMENT_A102, "CAR", 1),
                limit("c5555555-5555-5555-5555-555555555555", APARTMENT_B101, "MOTORBIKE", 2),
                limit("c6666666-6666-6666-6666-666666666666", APARTMENT_B101, "CAR", 1));

        limits.forEach(limit -> vehicleLimitRepository
                .findByApartmentIdAndVehicleType(limit.getApartmentId(), limit.getVehicleType())
                .orElseGet(() -> vehicleLimitRepository.save(limit)));
    }

    private void seedVehicles() {
        List<Vehicle> vehicles = List.of(
                vehicle("d1111111-1111-1111-1111-111111111111", APARTMENT_A101, RESIDENT_A101, "29A-10101", "MOTORBIKE", "Honda Vision", "APPROVED"),
                vehicle("d2222222-2222-2222-2222-222222222222", APARTMENT_A101, RESIDENT_A101, "30A-10101", "CAR", "Toyota Vios", "APPROVED"),
                vehicle("d3333333-3333-3333-3333-333333333333", APARTMENT_A102, RESIDENT_A102, "29A-10201", "MOTORBIKE", "Yamaha Janus", "APPROVED"),
                vehicle("d4444444-4444-4444-4444-444444444444", APARTMENT_B101, RESIDENT_B101, "29B-10101", "MOTORBIKE", "Honda Air Blade", "PENDING"));

        vehicles.forEach(vehicle -> {
            if (!vehicleRepository.existsByLicensePlate(vehicle.getLicensePlate())) {
                vehicleRepository.save(vehicle);
            }
        });
    }

    private VehicleLimit limit(String limitId, UUID apartmentId, String vehicleType, int maxQuantity) {
        return VehicleLimit.builder()
                .limitId(UUID.fromString(limitId))
                .apartmentId(apartmentId)
                .vehicleType(vehicleType)
                .maxQuantity(maxQuantity)
                .build();
    }

    private Vehicle vehicle(String vehicleId, UUID apartmentId, UUID ownerId, String licensePlate, String type, String brand, String status) {
        return Vehicle.builder()
                .vehicleId(UUID.fromString(vehicleId))
                .apartmentId(apartmentId)
                .ownerId(ownerId)
                .licensePlate(licensePlate)
                .type(type)
                .brand(brand)
                .status(status)
                .build();
    }
}