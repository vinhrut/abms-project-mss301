package com.abms.finance.controller;

import com.abms.finance.dto.CashPaymentRequest;
import com.abms.finance.dto.PaymentResponse;
import com.abms.finance.dto.VietQrConfirmRequest;
import com.abms.finance.dto.VietQrResponse;
import com.abms.finance.dto.VnPayCreateRequest;
import com.abms.finance.dto.VnPayCreateResponse;
import com.abms.finance.dto.VnPayIpnResponse;
import com.abms.finance.dto.VnPayProcessResult;
import com.abms.finance.service.PaymentService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
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
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    @Value("${vnpay.frontend-result-url}")
    private String frontendResultUrl;

    @GetMapping("/vietqr/{invoiceId}")
    public ResponseEntity<VietQrResponse> generateVietQr(@PathVariable("invoiceId") UUID invoiceId) {
        return ResponseEntity.ok(paymentService.generateVietQr(invoiceId));
    }

    @PostMapping("/vietqr/confirm")
    public ResponseEntity<PaymentResponse> confirmVietQrPayment(
            @Valid @RequestBody VietQrConfirmRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(paymentService.confirmVietQrPayment(request));
    }

    @PostMapping("/cash")
    public ResponseEntity<PaymentResponse> recordCashPayment(
            @Valid @RequestBody CashPaymentRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(paymentService.recordCashPayment(request));
    }

    @PostMapping("/vnpay/create")
    public ResponseEntity<VnPayCreateResponse> createVnPayPayment(
            @Valid @RequestBody VnPayCreateRequest request,
            HttpServletRequest httpRequest) {
        String clientIp = resolveClientIp(httpRequest);
        return ResponseEntity.ok(paymentService.createVnPayPayment(request, clientIp));
    }

    @GetMapping("/vnpay/return")
    public ResponseEntity<Void> vnPayReturn(@RequestParam Map<String, String> params) {
        VnPayProcessResult result = paymentService.processVnPayCallback(params);
        String redirect = buildFrontendRedirect(result);
        HttpHeaders headers = new HttpHeaders();
        headers.setLocation(URI.create(redirect));
        return new ResponseEntity<>(headers, HttpStatus.FOUND);
    }

    @GetMapping("/vnpay/ipn")
    public ResponseEntity<VnPayIpnResponse> vnPayIpn(@RequestParam Map<String, String> params) {
        VnPayProcessResult result = paymentService.processVnPayCallback(params);

        String rspCode;
        String message;
        if (!result.isChecksumValid()) {
            rspCode = "97";
            message = "Invalid Checksum";
        } else if ("01".equals(result.getRspCode())) {
            rspCode = "01";
            message = "Order not Found";
        } else if ("04".equals(result.getRspCode())) {
            rspCode = "04";
            message = "Invalid Amount";
        } else if (result.isAlreadyConfirmed() || "02".equals(result.getRspCode())) {
            rspCode = "02";
            message = "Order already confirmed";
        } else if (result.isSuccess()) {
            rspCode = "00";
            message = "Confirm Success";
        } else {
            // Payment declined at gateway — acknowledge receipt so VNPay stops retrying
            rspCode = "00";
            message = "Confirm Success";
        }

        return ResponseEntity.ok(VnPayIpnResponse.builder()
                .RspCode(rspCode)
                .Message(message)
                .build());
    }

    @GetMapping
    public ResponseEntity<List<PaymentResponse>> getPayments(
            @RequestParam(value = "apartmentId", required = false) UUID apartmentId) {
        return ResponseEntity.ok(paymentService.getPayments(apartmentId));
    }

    @GetMapping("/invoice/{invoiceId}")
    public ResponseEntity<List<PaymentResponse>> getPaymentsByInvoice(
            @PathVariable("invoiceId") UUID invoiceId) {
        return ResponseEntity.ok(paymentService.getPaymentsByInvoice(invoiceId));
    }

    private String buildFrontendRedirect(VnPayProcessResult result) {
        boolean success = result.isSuccess() || result.isAlreadyConfirmed();
        StringBuilder url = new StringBuilder(frontendResultUrl);
        url.append(frontendResultUrl.contains("?") ? "&" : "?");
        url.append("success=").append(success);
        if (result.getInvoiceCode() != null) {
            url.append("&invoiceCode=").append(encode(result.getInvoiceCode()));
        }
        if (result.getTxnRef() != null) {
            url.append("&txnRef=").append(encode(result.getTxnRef()));
        }
        String message = result.getMessage() != null ? result.getMessage() : (success ? "Payment success" : "Payment failed");
        url.append("&message=").append(encode(message));
        return url.toString();
    }

    private String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private String resolveClientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded;
        }
        return request.getRemoteAddr();
    }
}
