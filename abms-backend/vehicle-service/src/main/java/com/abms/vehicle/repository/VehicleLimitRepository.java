package com.abms.vehicle.repository;

import com.abms.vehicle.entity.VehicleLimit;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VehicleLimitRepository extends JpaRepository<VehicleLimit, UUID> {

    Optional<VehicleLimit> findByApartmentIdAndVehicleType(UUID apartmentId, String vehicleType);

    boolean existsByApartmentIdAndVehicleType(UUID apartmentId, String vehicleType);

    boolean existsByApartmentIdAndVehicleTypeAndLimitIdNot(UUID apartmentId, String vehicleType, UUID limitId);

    List<VehicleLimit> findByApartmentIdOrderByVehicleTypeAsc(UUID apartmentId);
}