package com.abms.vehicle.repository;

import com.abms.vehicle.entity.VehicleLimit;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VehicleLimitRepository extends JpaRepository<VehicleLimit, UUID> {

    Optional<VehicleLimit> findByApartmentIdAndVehicleType(UUID apartmentId, String vehicleType);
}