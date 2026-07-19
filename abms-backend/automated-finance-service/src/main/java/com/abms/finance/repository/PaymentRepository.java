package com.abms.finance.repository;

import com.abms.finance.entity.Payment;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentRepository extends JpaRepository<Payment, UUID> {

    List<Payment> findByInvoiceInvoiceId(UUID invoiceId);

    List<Payment> findByInvoiceApartmentId(UUID apartmentId);
}
