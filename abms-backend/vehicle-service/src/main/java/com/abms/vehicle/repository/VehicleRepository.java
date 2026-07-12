package com.abms.vehicle.repository;

import com.abms.vehicle.entity.Vehicle;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VehicleRepository extends JpaRepository<Vehicle, UUID> {

    long countByApartmentIdAndType(UUID apartmentId, String type);

    long countByApartmentIdAndTypeAndStatus(UUID apartmentId, String type, String status);

    boolean existsByLicensePlate(String licensePlate);

    boolean existsByLicensePlateAndVehicleIdNot(String licensePlate, UUID vehicleId);

    List<Vehicle> findByApartmentId(UUID apartmentId);

    List<Vehicle> findByOwnerId(UUID ownerId);

    List<Vehicle> findAllByOrderByStatusAscLicensePlateAsc();
}