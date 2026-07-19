package com.abms.maintenance.repository;

import com.abms.maintenance.entity.MaintenanceHistory;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MaintenanceHistoryRepository extends JpaRepository<MaintenanceHistory, UUID> {

    List<MaintenanceHistory> findByRequestIdOrderByChangedAtAsc(UUID requestId);
}
