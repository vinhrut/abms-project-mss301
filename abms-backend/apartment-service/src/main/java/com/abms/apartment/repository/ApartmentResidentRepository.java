package com.abms.apartment.repository;

import com.abms.apartment.entity.ApartmentResident;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ApartmentResidentRepository extends JpaRepository<ApartmentResident, UUID> {

    Optional<ApartmentResident> findFirstByUserIdAndStatusOrderByCreatedAtDesc(UUID userId, String status);

    Optional<ApartmentResident> findFirstByUserIdOrderByCreatedAtDesc(UUID userId);

    List<ApartmentResident> findByStatusOrderByCreatedAtAsc(String status);

    List<ApartmentResident> findByApartmentIdInAndStatusOrderByCreatedAtAsc(List<UUID> apartmentIds, String status);

    List<ApartmentResident> findByApartmentIdOrderByCreatedAtAsc(UUID apartmentId);

    boolean existsByApartmentIdAndUserIdAndStatus(UUID apartmentId, UUID userId, String status);
}