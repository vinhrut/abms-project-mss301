package com.abms.finance.service;

import com.abms.finance.dto.CashPaymentRequest;
import com.abms.finance.dto.PaymentResponse;
import com.abms.finance.dto.VietQrConfirmRequest;
import com.abms.finance.dto.VietQrResponse;
import com.abms.finance.dto.VnPayCreateRequest;
import com.abms.finance.dto.VnPayCreateResponse;
import com.abms.finance.dto.VnPayProcessResult;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public interface PaymentService {

    VietQrResponse generateVietQr(UUID invoiceId);

    PaymentResponse confirmVietQrPayment(VietQrConfirmRequest request);

    PaymentResponse recordCashPayment(CashPaymentRequest request);

    VnPayCreateResponse createVnPayPayment(VnPayCreateRequest request, String clientIp);

    VnPayProcessResult processVnPayCallback(Map<String, String> params);

    List<PaymentResponse> getPayments(UUID apartmentId);

    List<PaymentResponse> getPaymentsByInvoice(UUID invoiceId);
}
