package com.abms.finance.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "invoices")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Invoice {

    @Id
    @Column(name = "invoice_id", nullable = false)
    private UUID invoiceId;

    @Column(name = "apartment_id", nullable = false)
    private UUID apartmentId;

    @Column(name = "invoice_code", nullable = false, unique = true)
    private String invoiceCode;

    @Column(name = "billing_month", nullable = false)
    private LocalDate billingMonth;

    @Column(name = "total_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal totalAmount;

    @Column(name = "paid_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal paidAmount;

    @Column(name = "status", nullable = false)
    private String status;
}
