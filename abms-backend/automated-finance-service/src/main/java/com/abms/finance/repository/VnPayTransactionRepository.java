package com.abms.finance.repository;

import com.abms.finance.entity.VnPayTransaction;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VnPayTransactionRepository extends JpaRepository<VnPayTransaction, String> {
}
