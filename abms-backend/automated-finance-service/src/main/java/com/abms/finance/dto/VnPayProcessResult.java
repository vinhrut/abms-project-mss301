package com.abms.finance.dto;

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
public class VnPayProcessResult {

    private boolean success;
    private boolean checksumValid;
    private boolean alreadyConfirmed;
    private String txnRef;
    private String invoiceCode;
    private String message;
    private String rspCode;
}
