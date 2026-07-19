package com.abms.finance.repository;

import com.abms.finance.entity.ServiceItem;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ServiceItemRepository extends JpaRepository<ServiceItem, Integer> {
}
