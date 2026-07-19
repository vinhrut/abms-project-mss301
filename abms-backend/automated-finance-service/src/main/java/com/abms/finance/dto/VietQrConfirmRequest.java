package com.abms.finance.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class VietQrConfirmRequest {

    @NotNull(message = "Invoice id is required")
    private UUID invoiceId;

    @NotNull(message = "Payer id is required")
    private UUID payerId;

    @NotNull(message = "Paid amount is required")
    @DecimalMin(value = "0.01", message = "Paid amount must be greater than 0")
    private BigDecimal paidAmount;
}
