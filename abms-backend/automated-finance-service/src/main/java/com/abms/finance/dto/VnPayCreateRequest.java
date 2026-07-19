package com.abms.finance.dto;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class VnPayCreateRequest {

    @NotNull(message = "Invoice id is required")
    private UUID invoiceId;

    @NotNull(message = "Payer id is required")
    private UUID payerId;
}
