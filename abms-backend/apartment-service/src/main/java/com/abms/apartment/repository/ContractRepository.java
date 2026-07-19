package com.abms.apartment.repository;

import com.abms.apartment.entity.Contract;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ContractRepository extends JpaRepository<Contract, UUID> {

    Optional<Contract> findFirstByApartmentIdAndUserIdOrderByStartDateDesc(UUID apartmentId, UUID userId);

    Optional<Contract> findFirstByApartmentIdAndUserIdAndStatusOrderByStartDateDesc(UUID apartmentId, UUID userId, String status);

    java.util.List<Contract> findByApartmentIdInOrderByStartDateDesc(java.util.List<UUID> apartmentIds);
}
