package com.abms.finance.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class MeterReadingInvoiceRequest {

    @NotNull(message = "Apartment id is required")
    private UUID apartmentId;

    @NotNull(message = "Billing month is required")
    private LocalDate billingMonth;

    @NotEmpty(message = "At least one meter reading is required")
    @Valid
    private List<MeterReadingItemRequest> readings;
}
