package com.abms.finance.repository;

import com.abms.finance.entity.Invoice;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InvoiceRepository extends JpaRepository<Invoice, UUID> {

    Optional<Invoice> findByApartmentIdAndBillingMonth(UUID apartmentId, LocalDate billingMonth);

    List<Invoice> findByApartmentId(UUID apartmentId);

    List<Invoice> findByStatus(String status);

    List<Invoice> findByApartmentIdAndStatus(UUID apartmentId, String status);

    boolean existsByInvoiceCode(String invoiceCode);
}
