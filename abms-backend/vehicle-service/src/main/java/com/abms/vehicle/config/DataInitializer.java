package com.abms.vehicle.config;

import com.abms.vehicle.entity.Vehicle;
import com.abms.vehicle.entity.VehicleLimit;
import com.abms.vehicle.repository.VehicleLimitRepository;
import com.abms.vehicle.repository.VehicleRepository;
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

    // Keep in sync with apartment-service / auth-service seeds.
    private static final UUID APARTMENT_A101 = UUID.fromString("a101b202-c303-4d04-8e05-f606a707b808");
    private static final UUID APARTMENT_A102 = UUID.fromString("a102b203-c304-4d05-8e06-f607a708b809");
    private static final UUID APARTMENT_B101 = UUID.fromString("b101c202-d303-4e04-8f05-a606b707c808");
    private static final UUID RESIDENT_A101 = UUID.fromString("4d5e6f70-8192-4abc-d345-e6f7890a1b2c");
    private static final UUID RESIDENT_A102 = UUID.fromString("5e6f7081-92a3-4bcd-e456-f7890a1b2c3d");
    private static final UUID RESIDENT_B101 = UUID.fromString("6f708192-a3b4-4cde-f567-890a1b2c3d4e");

    private final VehicleLimitRepository vehicleLimitRepository;
    private final VehicleRepository vehicleRepository;

    @Override
    @Transactional
    public void run(String... args) {
        seedVehicleLimits();
        seedVehicles();
    }

    private void seedVehicleLimits() {
        List<VehicleLimit> limits = List.of(
                limit("c1a2b3c4-d5e6-4f70-8192-a3b4c5d6e701", APARTMENT_A101, "MOTORBIKE", 2),
                limit("c1a2b3c4-d5e6-4f70-8192-a3b4c5d6e702", APARTMENT_A101, "CAR", 1),
                limit("c1a2b3c4-d5e6-4f70-8192-a3b4c5d6e703", APARTMENT_A102, "MOTORBIKE", 2),
                limit("c1a2b3c4-d5e6-4f70-8192-a3b4c5d6e704", APARTMENT_A102, "CAR", 1),
                limit("c1a2b3c4-d5e6-4f70-8192-a3b4c5d6e705", APARTMENT_B101, "MOTORBIKE", 2),
                limit("c1a2b3c4-d5e6-4f70-8192-a3b4c5d6e706", APARTMENT_B101, "CAR", 1));

        Set<UUID> allowedIds = new HashSet<>();
        limits.forEach(limit -> allowedIds.add(limit.getLimitId()));

        vehicleLimitRepository.findAll().stream()
                .filter(existing -> !allowedIds.contains(existing.getLimitId()))
                .forEach(vehicleLimitRepository::delete);

        limits.forEach(limit -> vehicleLimitRepository.findById(limit.getLimitId()).ifPresentOrElse(existing -> {
            existing.setApartmentId(limit.getApartmentId());
            existing.setVehicleType(limit.getVehicleType());
            existing.setMaxQuantity(limit.getMaxQuantity());
            vehicleLimitRepository.save(existing);
        }, () -> vehicleLimitRepository.save(limit)));
    }

    private void seedVehicles() {
        List<Vehicle> vehicles = List.of(
                vehicle("d1a2b3c4-e5f6-4071-8293-b4c5d6e7f801", APARTMENT_A101, RESIDENT_A101, "29A-10101", "MOTORBIKE", "Honda Vision", "APPROVED"),
                vehicle("d1a2b3c4-e5f6-4071-8293-b4c5d6e7f802", APARTMENT_A101, RESIDENT_A101, "30A-10101", "CAR", "Toyota Vios", "APPROVED"),
                vehicle("d1a2b3c4-e5f6-4071-8293-b4c5d6e7f803", APARTMENT_A102, RESIDENT_A102, "29A-10201", "MOTORBIKE", "Yamaha Janus", "APPROVED"),
                vehicle("d1a2b3c4-e5f6-4071-8293-b4c5d6e7f804", APARTMENT_B101, RESIDENT_B101, "29B-10101", "MOTORBIKE", "Honda Air Blade", "PENDING"));

        Set<UUID> allowedIds = new HashSet<>();
        vehicles.forEach(vehicle -> allowedIds.add(vehicle.getVehicleId()));

        vehicleRepository.findAll().stream()
                .filter(existing -> !allowedIds.contains(existing.getVehicleId()))
                .forEach(vehicleRepository::delete);

        vehicles.forEach(seed -> {
            vehicleRepository.findByLicensePlate(seed.getLicensePlate()).ifPresent(existingByPlate -> {
                if (!existingByPlate.getVehicleId().equals(seed.getVehicleId())) {
                    vehicleRepository.delete(existingByPlate);
                    vehicleRepository.flush();
                }
            });

            vehicleRepository.findById(seed.getVehicleId()).ifPresentOrElse(existing -> {
                existing.setApartmentId(seed.getApartmentId());
                existing.setOwnerId(seed.getOwnerId());
                existing.setLicensePlate(seed.getLicensePlate());
                existing.setType(seed.getType());
                existing.setBrand(seed.getBrand());
                existing.setStatus(seed.getStatus());
                vehicleRepository.save(existing);
            }, () -> vehicleRepository.save(seed));
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

    private Vehicle vehicle(
            String vehicleId,
            UUID apartmentId,
            UUID ownerId,
            String licensePlate,
            String type,
            String brand,
            String status) {
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
