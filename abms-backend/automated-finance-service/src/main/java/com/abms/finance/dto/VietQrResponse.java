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
public class VietQrResponse {

    private UUID invoiceId;
    private String invoiceCode;
    private BigDecimal amount;
    private String bankBin;
    private String accountNo;
    private String accountName;
    private String transferContent;
    private String qrImageUrl;
}
