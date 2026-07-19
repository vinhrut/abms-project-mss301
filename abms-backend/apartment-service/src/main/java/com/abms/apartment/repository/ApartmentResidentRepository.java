package com.abms.apartment.repository;

import com.abms.apartment.entity.ApartmentResident;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ApartmentResidentRepository extends JpaRepository<ApartmentResident, UUID> {

    Optional<ApartmentResident> findFirstByUserIdAndStatusOrderByCreatedAtDesc(UUID userId, String status);

    List<ApartmentResident> findByUserIdAndStatusOrderByCreatedAtDesc(UUID userId, String status);

    Optional<ApartmentResident> findFirstByUserIdOrderByCreatedAtDesc(UUID userId);

    List<ApartmentResident> findByStatusOrderByCreatedAtAsc(String status);

    boolean existsByApartmentIdAndUserIdAndStatus(UUID apartmentId, UUID userId, String status);
}