package com.abms.finance.dto;

import java.math.BigDecimal;
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
public class InvoiceDetailResponse {

    private UUID detailId;
    private Integer serviceId;
    private String serviceName;
    private String unit;
    private BigDecimal quantity;
    private BigDecimal oldIndex;
    private BigDecimal newIndex;
    private BigDecimal unitPrice;
    private BigDecimal amount;
}
