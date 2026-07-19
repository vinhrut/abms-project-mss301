package com.abms.finance.dto;

import java.math.BigDecimal;
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
public class ServiceResponse {

    private Integer serviceId;
    private String name;
    private BigDecimal unitPrice;
    private String unit;
}
