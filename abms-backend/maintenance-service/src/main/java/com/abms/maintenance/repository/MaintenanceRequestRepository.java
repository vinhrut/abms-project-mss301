package com.abms.maintenance.repository;

import com.abms.maintenance.entity.MaintenanceRequest;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MaintenanceRequestRepository extends JpaRepository<MaintenanceRequest, UUID> {

    List<MaintenanceRequest> findBySenderIdOrderByCreatedAtDesc(UUID senderId);

    List<MaintenanceRequest> findByTechnicianIdOrderByCreatedAtDesc(UUID technicianId);

    List<MaintenanceRequest> findAllByOrderByCreatedAtDesc();

    long countBySenderIdAndStatusIn(UUID senderId, List<String> statuses);

    Optional<MaintenanceRequest> findByRequestCode(String requestCode);
}
