package com.abms.finance.service.impl;

import com.abms.finance.client.ApartmentClient;
import com.abms.finance.dto.InvoiceDetailResponse;
import com.abms.finance.dto.InvoiceResponse;
import com.abms.finance.dto.MeterReadingInvoiceRequest;
import com.abms.finance.dto.MeterReadingItemRequest;
import com.abms.finance.entity.Invoice;
import com.abms.finance.entity.InvoiceDetail;
import com.abms.finance.entity.ServiceItem;
import com.abms.finance.exception.BusinessException;
import com.abms.finance.exception.ResourceNotFoundException;
import com.abms.finance.repository.InvoiceDetailRepository;
import com.abms.finance.repository.InvoiceRepository;
import com.abms.finance.repository.ServiceItemRepository;
import com.abms.finance.service.InvoiceService;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class InvoiceServiceImpl implements InvoiceService {

    public static final String STATUS_UNPAID = "UNPAID";
    public static final String STATUS_PARTIAL = "PARTIAL";
    public static final String STATUS_PAID = "PAID";
    public static final String DISPLAY_OVERDUE = "OVERDUE";

    private final InvoiceRepository invoiceRepository;
    private final InvoiceDetailRepository invoiceDetailRepository;
    private final ServiceItemRepository serviceItemRepository;
    @Autowired
    private final ApartmentClient apartmentClient;

    @Override
    @Transactional
    public InvoiceResponse createFromMeterReadings(MeterReadingInvoiceRequest request) {
        apartmentClient.getApartmentById(request.getApartmentId());

        LocalDate billingMonth = request.getBillingMonth().withDayOfMonth(1);

        invoiceRepository.findByApartmentIdAndBillingMonth(request.getApartmentId(), billingMonth)
                .ifPresent(existing -> {
                    throw new BusinessException(
                            "Invoice already exists for apartment " + request.getApartmentId()
                                    + " in billing month " + billingMonth);
                });

        Invoice invoice = Invoice.builder()
                .invoiceId(UUID.randomUUID())
                .apartmentId(request.getApartmentId())
                .invoiceCode(generateInvoiceCode(billingMonth))
                .billingMonth(billingMonth)
                .totalAmount(BigDecimal.ZERO)
                .paidAmount(BigDecimal.ZERO)
                .status(STATUS_UNPAID)
                .build();

        List<InvoiceDetail> details = new ArrayList<>();
        BigDecimal total = BigDecimal.ZERO;

        for (MeterReadingItemRequest reading : request.getReadings()) {
            if (reading.getNewIndex().compareTo(reading.getOldIndex()) < 0) {
                throw new BusinessException(
                        "New index must be >= old index for service " + reading.getServiceId());
            }

            ServiceItem service = serviceItemRepository.findById(reading.getServiceId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Service not found: " + reading.getServiceId()));

            BigDecimal quantity = reading.getNewIndex().subtract(reading.getOldIndex());
            BigDecimal amount = quantity.multiply(service.getUnitPrice()).setScale(2, RoundingMode.HALF_UP);

            details.add(InvoiceDetail.builder()
                    .detailId(UUID.randomUUID())
                    .invoice(invoice)
                    .service(service)
                    .quantity(quantity)
                    .oldIndex(reading.getOldIndex())
                    .newIndex(reading.getNewIndex())
                    .amount(amount)
                    .build());

            total = total.add(amount);
        }

        invoice.setTotalAmount(total.setScale(2, RoundingMode.HALF_UP));
        invoiceRepository.save(invoice);
        invoiceDetailRepository.saveAll(details);

        return toResponse(invoice, details);
    }

    @Override
    @Transactional(readOnly = true)
    public InvoiceResponse getInvoiceById(UUID invoiceId) {
        Invoice invoice = findInvoice(invoiceId);
        List<InvoiceDetail> details = invoiceDetailRepository.findByInvoiceInvoiceId(invoiceId);
        return toResponse(invoice, details);
    }

    @Override
    @Transactional(readOnly = true)
    public List<InvoiceResponse> getInvoices(UUID apartmentId, LocalDate billingMonth, String status) {
        List<Invoice> invoices;

        if (apartmentId != null && status != null && !status.isBlank()) {
            invoices = invoiceRepository.findByApartmentIdAndStatus(apartmentId, status.trim().toUpperCase());
        } else if (apartmentId != null) {
            invoices = invoiceRepository.findByApartmentId(apartmentId);
        } else if (status != null && !status.isBlank()) {
            invoices = invoiceRepository.findByStatus(status.trim().toUpperCase());
        } else {
            invoices = invoiceRepository.findAll();
        }

        if (billingMonth != null) {
            LocalDate monthStart = billingMonth.withDayOfMonth(1);
            invoices = invoices.stream()
                    .filter(invoice -> invoice.getBillingMonth().equals(monthStart))
                    .toList();
        }

        return invoices.stream()
                .map(invoice -> toResponse(
                        invoice,
                        invoiceDetailRepository.findByInvoiceInvoiceId(invoice.getInvoiceId())))
                .toList();
    }

    private Invoice findInvoice(UUID invoiceId) {
        return invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new ResourceNotFoundException("Invoice not found: " + invoiceId));
    }

    private String generateInvoiceCode(LocalDate billingMonth) {
        String prefix = "INV-" + billingMonth.format(DateTimeFormatter.ofPattern("yyyyMM")) + "-";
        String code;
        do {
            int suffix = ThreadLocalRandom.current().nextInt(1000, 10000);
            code = prefix + suffix;
        } while (invoiceRepository.existsByInvoiceCode(code));
        return code;
    }

    private InvoiceResponse toResponse(Invoice invoice, List<InvoiceDetail> details) {
        BigDecimal remaining = invoice.getTotalAmount().subtract(invoice.getPaidAmount());
        if (remaining.compareTo(BigDecimal.ZERO) < 0) {
            remaining = BigDecimal.ZERO;
        }

        return InvoiceResponse.builder()
                .invoiceId(invoice.getInvoiceId())
                .apartmentId(invoice.getApartmentId())
                .invoiceCode(invoice.getInvoiceCode())
                .billingMonth(invoice.getBillingMonth())
                .totalAmount(invoice.getTotalAmount())
                .paidAmount(invoice.getPaidAmount())
                .remainingAmount(remaining)
                .status(invoice.getStatus())
                .displayStatus(resolveDisplayStatus(invoice))
                .details(details.stream().map(this::toDetailResponse).toList())
                .build();
    }

    private InvoiceDetailResponse toDetailResponse(InvoiceDetail detail) {
        ServiceItem service = detail.getService();
        return InvoiceDetailResponse.builder()
                .detailId(detail.getDetailId())
                .serviceId(service.getServiceId())
                .serviceName(service.getName())
                .unit(service.getUnit())
                .quantity(detail.getQuantity())
                .oldIndex(detail.getOldIndex())
                .newIndex(detail.getNewIndex())
                .unitPrice(service.getUnitPrice())
                .amount(detail.getAmount())
                .build();
    }

    private String resolveDisplayStatus(Invoice invoice) {
        if (!STATUS_PAID.equals(invoice.getStatus())
                && invoice.getBillingMonth().isBefore(LocalDate.now().withDayOfMonth(1))) {
            return DISPLAY_OVERDUE;
        }
        return invoice.getStatus();
    }
}
