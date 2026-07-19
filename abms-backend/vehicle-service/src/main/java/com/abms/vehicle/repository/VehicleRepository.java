package com.abms.vehicle.repository;

import com.abms.vehicle.entity.Vehicle;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface VehicleRepository extends JpaRepository<Vehicle, UUID>, JpaSpecificationExecutor<Vehicle> {

    long countByApartmentIdAndType(UUID apartmentId, String type);

    long countByApartmentIdAndTypeAndStatus(UUID apartmentId, String type, String status);

    boolean existsByLicensePlate(String licensePlate);

    Optional<Vehicle> findByLicensePlate(String licensePlate);

    boolean existsByLicensePlateAndVehicleIdNot(String licensePlate, UUID vehicleId);

    List<Vehicle> findByApartmentId(UUID apartmentId);

    List<Vehicle> findByOwnerId(UUID ownerId);

    List<Vehicle> findByOwnerIdOrderByStatusAscLicensePlateAsc(UUID ownerId);

    List<Vehicle> findAllByOrderByStatusAscLicensePlateAsc();

    Page<Vehicle> findAllByOrderByStatusAscLicensePlateAsc(Pageable pageable);
}