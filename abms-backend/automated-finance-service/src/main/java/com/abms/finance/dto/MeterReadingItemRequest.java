package com.abms.finance.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class MeterReadingItemRequest {

    @NotNull(message = "Service id is required")
    private Integer serviceId;

    @NotNull(message = "Old index is required")
    @DecimalMin(value = "0.0", inclusive = true, message = "Old index must be >= 0")
    private BigDecimal oldIndex;

    @NotNull(message = "New index is required")
    @DecimalMin(value = "0.0", inclusive = true, message = "New index must be >= 0")
    private BigDecimal newIndex;
}
