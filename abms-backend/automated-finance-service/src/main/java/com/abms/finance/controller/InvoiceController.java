package com.abms.finance.controller;

import com.abms.finance.dto.InvoiceResponse;
import com.abms.finance.dto.MeterReadingInvoiceRequest;
import com.abms.finance.service.InvoiceService;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/invoices")
@RequiredArgsConstructor
public class InvoiceController {

    private final InvoiceService invoiceService;

    @PostMapping("/meter-readings")
    public ResponseEntity<InvoiceResponse> createFromMeterReadings(
            @Valid @RequestBody MeterReadingInvoiceRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(invoiceService.createFromMeterReadings(request));
    }

    @GetMapping("/apartment/{apartmentId}")
    public ResponseEntity<List<InvoiceResponse>> getInvoicesByApartment(
            @PathVariable("apartmentId") UUID apartmentId) {
        return ResponseEntity.ok(invoiceService.getInvoices(apartmentId, null, null));
    }

    @GetMapping("/{invoiceId}")
    public ResponseEntity<InvoiceResponse> getInvoiceById(@PathVariable("invoiceId") UUID invoiceId) {
        return ResponseEntity.ok(invoiceService.getInvoiceById(invoiceId));
    }

    @GetMapping
    public ResponseEntity<List<InvoiceResponse>> getInvoices(
            @RequestParam(value = "apartmentId", required = false) UUID apartmentId,
            @RequestParam(value = "billingMonth", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate billingMonth,
            @RequestParam(value = "status", required = false) String status) {
        return ResponseEntity.ok(invoiceService.getInvoices(apartmentId, billingMonth, status));
    }
}
