package com.abms.finance.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentResponse {

    private UUID paymentId;
    private UUID invoiceId;
    private String invoiceCode;
    private UUID payerId;
    private UUID collectorId;
    private BigDecimal paidAmount;
    private String paymentMethod;
    private LocalDateTime paymentTime;
}
