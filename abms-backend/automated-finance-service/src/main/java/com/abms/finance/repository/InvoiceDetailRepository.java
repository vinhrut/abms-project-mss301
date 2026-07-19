package com.abms.finance.repository;

import com.abms.finance.entity.InvoiceDetail;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InvoiceDetailRepository extends JpaRepository<InvoiceDetail, UUID> {

    List<InvoiceDetail> findByInvoiceInvoiceId(UUID invoiceId);
}
