package com.abms.finance.service;

import com.abms.finance.dto.InvoiceResponse;
import com.abms.finance.dto.MeterReadingInvoiceRequest;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface InvoiceService {

    InvoiceResponse createFromMeterReadings(MeterReadingInvoiceRequest request);

    InvoiceResponse getInvoiceById(UUID invoiceId);

    List<InvoiceResponse> getInvoices(UUID apartmentId, LocalDate billingMonth, String status);
}
