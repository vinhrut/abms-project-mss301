package com.abms.vehicle.config;

import com.abms.vehicle.entity.Vehicle;
import com.abms.vehicle.entity.VehicleLimit;
import com.abms.vehicle.repository.VehicleLimitRepository;
import com.abms.vehicle.repository.VehicleRepository;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final VehicleLimitRepository vehicleLimitRepository;
    private final VehicleRepository vehicleRepository;

    @Override
    public void run(String... args) {
        seedVehicleLimits();
        seedVehicles();
    }

    private void seedVehicleLimits() {
        if (vehicleLimitRepository.count() > 0) {
            return;
        }

        vehicleLimitRepository.save(VehicleLimit.builder()
                .limitId(UUID.fromString("c1111111-1111-1111-1111-111111111111"))
                .apartmentId(UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa"))
                .vehicleType("MOTORBIKE")
                .maxQuantity(2)
                .build());

        vehicleLimitRepository.save(VehicleLimit.builder()
                .limitId(UUID.fromString("c2222222-2222-2222-2222-222222222222"))
                .apartmentId(UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa"))
                .vehicleType("CAR")
                .maxQuantity(1)
                .build());

        vehicleLimitRepository.save(VehicleLimit.builder()
                .limitId(UUID.fromString("c3333333-3333-3333-3333-333333333333"))
                .apartmentId(UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaab"))
                .vehicleType("MOTORBIKE")
                .maxQuantity(2)
                .build());

        vehicleLimitRepository.save(VehicleLimit.builder()
                .limitId(UUID.fromString("c4444444-4444-4444-4444-444444444444"))
                .apartmentId(UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaab"))
                .vehicleType("CAR")
                .maxQuantity(1)
                .build());
    }

    private void seedVehicles() {
        if (vehicleRepository.count() > 0) {
            return;
        }

        vehicleRepository.save(Vehicle.builder()
                .vehicleId(UUID.fromString("d1111111-1111-1111-1111-111111111111"))
                .apartmentId(UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa"))
                .ownerId(UUID.fromString("11111111-1111-1111-1111-111111111111"))
                .licensePlate("29A-99999")
                .type("MOTORBIKE")
                .brand("Honda")
                .status("APPROVED")
                .build());

        vehicleRepository.save(Vehicle.builder()
                .vehicleId(UUID.fromString("d2222222-2222-2222-2222-222222222222"))
                .apartmentId(UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa"))
                .ownerId(UUID.fromString("11111111-1111-1111-1111-111111111111"))
                .licensePlate("30B-12345")
                .type("CAR")
                .brand("Toyota")
                .status("PENDING")
                .build());

        vehicleRepository.save(Vehicle.builder()
                .vehicleId(UUID.fromString("d3333333-3333-3333-3333-333333333333"))
                .apartmentId(UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaab"))
                .ownerId(UUID.fromString("44444444-4444-4444-4444-444444444444"))
                .licensePlate("29B-67890")
                .type("MOTORBIKE")
                .brand("Yamaha")
                .status("PENDING")
                .build());
    }
}