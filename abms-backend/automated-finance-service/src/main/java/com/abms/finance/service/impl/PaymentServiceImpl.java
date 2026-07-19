package com.abms.finance.service.impl;

import com.abms.finance.dto.CashPaymentRequest;
import com.abms.finance.dto.PaymentResponse;
import com.abms.finance.dto.VietQrConfirmRequest;
import com.abms.finance.dto.VietQrResponse;
import com.abms.finance.dto.VnPayCreateRequest;
import com.abms.finance.dto.VnPayCreateResponse;
import com.abms.finance.dto.VnPayProcessResult;
import com.abms.finance.entity.Invoice;
import com.abms.finance.entity.Payment;
import com.abms.finance.entity.VnPayTransaction;
import com.abms.finance.exception.BusinessException;
import com.abms.finance.exception.ResourceNotFoundException;
import com.abms.finance.repository.InvoiceRepository;
import com.abms.finance.repository.PaymentRepository;
import com.abms.finance.repository.VnPayTransactionRepository;
import com.abms.finance.service.PaymentService;
import com.abms.finance.util.VnPayUtil;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PaymentServiceImpl implements PaymentService {

    public static final String METHOD_CASH = "CASH";
    public static final String METHOD_VIETQR = "VIETQR";
    public static final String METHOD_VNPAY = "VNPAY";

    private static final ZoneId VN_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    private static final DateTimeFormatter VNP_DATE_FMT = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    private final PaymentRepository paymentRepository;
    private final InvoiceRepository invoiceRepository;
    private final VnPayTransactionRepository vnPayTransactionRepository;

    @Value("${vietqr.bank-bin}")
    private String bankBin;

    @Value("${vietqr.account-no}")
    private String accountNo;

    @Value("${vietqr.account-name}")
    private String accountName;

    @Value("${vietqr.template:compact2}")
    private String template;

    @Value("${vnpay.tmn-code}")
    private String vnpTmnCode;

    @Value("${vnpay.hash-secret}")
    private String vnpHashSecret;

    @Value("${vnpay.pay-url}")
    private String vnpPayUrl;

    @Value("${vnpay.return-url}")
    private String vnpReturnUrl;

    @Value("${vnpay.version:2.1.0}")
    private String vnpVersion;

    @Value("${vnpay.order-type:other}")
    private String vnpOrderType;

    @Value("${vnpay.expire-minutes:15}")
    private int vnpExpireMinutes;

    @Override
    @Transactional(readOnly = true)
    public VietQrResponse generateVietQr(UUID invoiceId) {
        Invoice invoice = findInvoice(invoiceId);
        ensurePayable(invoice);

        BigDecimal remaining = remainingAmount(invoice);
        String amount = remaining.setScale(0, RoundingMode.HALF_UP).toPlainString();
        String encodedName = URLEncoder.encode(accountName, StandardCharsets.UTF_8);
        String encodedContent = URLEncoder.encode(invoice.getInvoiceCode(), StandardCharsets.UTF_8);

        String qrImageUrl = String.format(
                "https://img.vietqr.io/image/%s-%s-%s.png?amount=%s&addInfo=%s&accountName=%s",
                bankBin,
                accountNo,
                template,
                amount,
                encodedContent,
                encodedName);

        return VietQrResponse.builder()
                .invoiceId(invoice.getInvoiceId())
                .invoiceCode(invoice.getInvoiceCode())
                .amount(remaining)
                .bankBin(bankBin)
                .accountNo(accountNo)
                .accountName(accountName)
                .transferContent(invoice.getInvoiceCode())
                .qrImageUrl(qrImageUrl)
                .build();
    }

    @Override
    @Transactional
    public PaymentResponse confirmVietQrPayment(VietQrConfirmRequest request) {
        Invoice invoice = findInvoice(request.getInvoiceId());
        ensurePayable(invoice);
        validatePaymentAmount(invoice, request.getPaidAmount());

        Payment payment = Payment.builder()
                .paymentId(UUID.randomUUID())
                .invoice(invoice)
                .payerId(request.getPayerId())
                .collectorId(null)
                .paidAmount(request.getPaidAmount())
                .paymentMethod(METHOD_VIETQR)
                .paymentTime(LocalDateTime.now())
                .build();

        applyPayment(invoice, request.getPaidAmount());
        paymentRepository.save(payment);
        invoiceRepository.save(invoice);

        return toResponse(payment);
    }

    @Override
    @Transactional
    public PaymentResponse recordCashPayment(CashPaymentRequest request) {
        Invoice invoice = findInvoice(request.getInvoiceId());
        ensurePayable(invoice);
        validatePaymentAmount(invoice, request.getPaidAmount());

        Payment payment = Payment.builder()
                .paymentId(UUID.randomUUID())
                .invoice(invoice)
                .payerId(request.getPayerId())
                .collectorId(request.getCollectorId())
                .paidAmount(request.getPaidAmount())
                .paymentMethod(METHOD_CASH)
                .paymentTime(LocalDateTime.now())
                .build();

        applyPayment(invoice, request.getPaidAmount());
        paymentRepository.save(payment);
        invoiceRepository.save(invoice);

        return toResponse(payment);
    }

    @Override
    @Transactional
    public VnPayCreateResponse createVnPayPayment(VnPayCreateRequest request, String clientIp) {
        String secret = vnpHashSecret == null ? "" : vnpHashSecret.trim();
        String tmn = vnpTmnCode == null ? "" : vnpTmnCode.trim();
        if (secret.isBlank() || tmn.isBlank()) {
            throw new BusinessException(
                    "VNPay chưa cấu hình. Set vnpay.tmn-code và vnpay.hash-secret "
                            + "(lấy đúng cặp từ https://sandbox.vnpayment.vn/merchantv2/). "
                            + "Error code=71 thường do secret không khớp TmnCode.");
        }

        Invoice invoice = findInvoice(request.getInvoiceId());
        ensurePayable(invoice);

        BigDecimal remaining = remainingAmount(invoice).setScale(0, RoundingMode.HALF_UP);
        if (remaining.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException("Invoice has no remaining balance: " + invoice.getInvoiceCode());
        }

        String txnRef = generateTxnRef();
        LocalDateTime now = LocalDateTime.now(VN_ZONE);

        VnPayTransaction txn = VnPayTransaction.builder()
                .txnRef(txnRef)
                .invoiceId(invoice.getInvoiceId())
                .payerId(request.getPayerId())
                .amount(remaining)
                .status(VnPayTransaction.STATUS_PENDING)
                .createdAt(now)
                .build();
        vnPayTransactionRepository.save(txn);

        long amountForVnPay = remaining.multiply(BigDecimal.valueOf(100)).longValueExact();
        String orderInfo = "Thanh toan hoa don " + sanitizeOrderInfo(invoice.getInvoiceCode());

        Map<String, String> vnpParams = new HashMap<>();
        vnpParams.put("vnp_Version", vnpVersion);
        vnpParams.put("vnp_Command", "pay");
        vnpParams.put("vnp_TmnCode", tmn);
        vnpParams.put("vnp_Amount", String.valueOf(amountForVnPay));
        vnpParams.put("vnp_CurrCode", "VND");
        vnpParams.put("vnp_TxnRef", txnRef);
        vnpParams.put("vnp_OrderInfo", orderInfo);
        vnpParams.put("vnp_OrderType", vnpOrderType);
        vnpParams.put("vnp_Locale", "vn");
        vnpParams.put("vnp_ReturnUrl", vnpReturnUrl);
        vnpParams.put("vnp_IpAddr", toVnPayIpv4(clientIp));
        vnpParams.put("vnp_CreateDate", now.format(VNP_DATE_FMT));
        vnpParams.put("vnp_ExpireDate", now.plusMinutes(vnpExpireMinutes).format(VNP_DATE_FMT));

        String hashData = VnPayUtil.buildHashData(vnpParams);
        String query = VnPayUtil.buildQueryString(vnpParams);
        String secureHash = VnPayUtil.hmacSHA512(secret, hashData);
        String paymentUrl = vnpPayUrl + "?" + query + "&vnp_SecureHash=" + secureHash;

        return VnPayCreateResponse.builder()
                .paymentUrl(paymentUrl)
                .txnRef(txnRef)
                .amount(remaining)
                .build();
    }

    @Override
    @Transactional
    public VnPayProcessResult processVnPayCallback(Map<String, String> params) {
        Map<String, String> fields = new HashMap<>(params);
        String secureHash = fields.remove("vnp_SecureHash");
        fields.remove("vnp_SecureHashType");

        if (!VnPayUtil.isValidSecureHash(fields, secureHash, vnpHashSecret)) {
            return VnPayProcessResult.builder()
                    .success(false)
                    .checksumValid(false)
                    .txnRef(fields.get("vnp_TxnRef"))
                    .message("Invalid signature")
                    .rspCode("97")
                    .build();
        }

        String txnRef = fields.get("vnp_TxnRef");
        String responseCode = fields.get("vnp_ResponseCode");
        String transactionStatus = fields.get("vnp_TransactionStatus");
        String vnpAmount = fields.get("vnp_Amount");
        String vnpTransactionNo = fields.get("vnp_TransactionNo");
        String bankCode = fields.get("vnp_BankCode");

        VnPayTransaction txn = vnPayTransactionRepository.findById(txnRef).orElse(null);
        if (txn == null) {
            return VnPayProcessResult.builder()
                    .success(false)
                    .checksumValid(true)
                    .txnRef(txnRef)
                    .message("Order not found")
                    .rspCode("01")
                    .build();
        }

        Invoice invoice = invoiceRepository.findById(txn.getInvoiceId()).orElse(null);
        String invoiceCode = invoice != null ? invoice.getInvoiceCode() : null;

        if (VnPayTransaction.STATUS_SUCCESS.equals(txn.getStatus())) {
            return VnPayProcessResult.builder()
                    .success(true)
                    .checksumValid(true)
                    .alreadyConfirmed(true)
                    .txnRef(txnRef)
                    .invoiceCode(invoiceCode)
                    .message("Order already confirmed")
                    .rspCode("02")
                    .build();
        }

        BigDecimal paidAmountFromVnPay = new BigDecimal(vnpAmount)
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        if (txn.getAmount().compareTo(paidAmountFromVnPay) != 0) {
            txn.setStatus(VnPayTransaction.STATUS_FAILED);
            txn.setResponseCode(responseCode);
            txn.setVnpTransactionNo(vnpTransactionNo);
            txn.setBankCode(bankCode);
            vnPayTransactionRepository.save(txn);

            return VnPayProcessResult.builder()
                    .success(false)
                    .checksumValid(true)
                    .txnRef(txnRef)
                    .invoiceCode(invoiceCode)
                    .message("Invalid amount")
                    .rspCode("04")
                    .build();
        }

        boolean payOk = "00".equals(responseCode)
                && (transactionStatus == null || transactionStatus.isBlank() || "00".equals(transactionStatus));

        txn.setResponseCode(responseCode);
        txn.setVnpTransactionNo(vnpTransactionNo);
        txn.setBankCode(bankCode);

        if (!payOk) {
            txn.setStatus(VnPayTransaction.STATUS_FAILED);
            vnPayTransactionRepository.save(txn);
            return VnPayProcessResult.builder()
                    .success(false)
                    .checksumValid(true)
                    .txnRef(txnRef)
                    .invoiceCode(invoiceCode)
                    .message("Payment failed")
                    .rspCode("00")
                    .build();
        }

        if (invoice == null) {
            txn.setStatus(VnPayTransaction.STATUS_FAILED);
            vnPayTransactionRepository.save(txn);
            return VnPayProcessResult.builder()
                    .success(false)
                    .checksumValid(true)
                    .txnRef(txnRef)
                    .message("Invoice not found")
                    .rspCode("01")
                    .build();
        }

        if (InvoiceServiceImpl.STATUS_PAID.equals(invoice.getStatus())) {
            txn.setStatus(VnPayTransaction.STATUS_SUCCESS);
            txn.setPaidAt(LocalDateTime.now(VN_ZONE));
            vnPayTransactionRepository.save(txn);
            return VnPayProcessResult.builder()
                    .success(true)
                    .checksumValid(true)
                    .alreadyConfirmed(true)
                    .txnRef(txnRef)
                    .invoiceCode(invoice.getInvoiceCode())
                    .message("Invoice already paid")
                    .rspCode("02")
                    .build();
        }

        validatePaymentAmount(invoice, txn.getAmount());

        Payment payment = Payment.builder()
                .paymentId(UUID.randomUUID())
                .invoice(invoice)
                .payerId(txn.getPayerId())
                .collectorId(null)
                .paidAmount(txn.getAmount())
                .paymentMethod(METHOD_VNPAY)
                .paymentTime(LocalDateTime.now())
                .build();

        applyPayment(invoice, txn.getAmount());
        paymentRepository.save(payment);
        invoiceRepository.save(invoice);

        txn.setStatus(VnPayTransaction.STATUS_SUCCESS);
        txn.setPaidAt(LocalDateTime.now(VN_ZONE));
        vnPayTransactionRepository.save(txn);

        return VnPayProcessResult.builder()
                .success(true)
                .checksumValid(true)
                .alreadyConfirmed(false)
                .txnRef(txnRef)
                .invoiceCode(invoice.getInvoiceCode())
                .message("Confirm Success")
                .rspCode("00")
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public List<PaymentResponse> getPayments(UUID apartmentId) {
        List<Payment> payments = apartmentId == null
                ? paymentRepository.findAll()
                : paymentRepository.findByInvoiceApartmentId(apartmentId);
        return payments.stream().map(this::toResponse).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<PaymentResponse> getPaymentsByInvoice(UUID invoiceId) {
        findInvoice(invoiceId);
        return paymentRepository.findByInvoiceInvoiceId(invoiceId).stream()
                .map(this::toResponse)
                .toList();
    }

    private Invoice findInvoice(UUID invoiceId) {
        return invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new ResourceNotFoundException("Invoice not found: " + invoiceId));
    }

    private void ensurePayable(Invoice invoice) {
        if (InvoiceServiceImpl.STATUS_PAID.equals(invoice.getStatus())) {
            throw new BusinessException("Invoice is already paid: " + invoice.getInvoiceCode());
        }
    }

    private void validatePaymentAmount(Invoice invoice, BigDecimal paidAmount) {
        BigDecimal remaining = remainingAmount(invoice);
        if (paidAmount.compareTo(remaining) > 0) {
            throw new BusinessException(
                    "Paid amount exceeds remaining debt. Remaining: " + remaining);
        }
    }

    private BigDecimal remainingAmount(Invoice invoice) {
        return invoice.getTotalAmount().subtract(invoice.getPaidAmount()).max(BigDecimal.ZERO);
    }

    private void applyPayment(Invoice invoice, BigDecimal paidAmount) {
        BigDecimal newPaid = invoice.getPaidAmount().add(paidAmount).setScale(2, RoundingMode.HALF_UP);
        invoice.setPaidAmount(newPaid);

        if (newPaid.compareTo(invoice.getTotalAmount()) >= 0) {
            invoice.setStatus(InvoiceServiceImpl.STATUS_PAID);
            invoice.setPaidAmount(invoice.getTotalAmount());
        } else if (newPaid.compareTo(BigDecimal.ZERO) > 0) {
            invoice.setStatus(InvoiceServiceImpl.STATUS_PARTIAL);
        } else {
            invoice.setStatus(InvoiceServiceImpl.STATUS_UNPAID);
        }
    }

    private PaymentResponse toResponse(Payment payment) {
        Invoice invoice = payment.getInvoice();
        return PaymentResponse.builder()
                .paymentId(payment.getPaymentId())
                .invoiceId(invoice.getInvoiceId())
                .invoiceCode(invoice.getInvoiceCode())
                .payerId(payment.getPayerId())
                .collectorId(payment.getCollectorId())
                .paidAmount(payment.getPaidAmount())
                .paymentMethod(payment.getPaymentMethod())
                .paymentTime(payment.getPaymentTime())
                .build();
    }

    private String generateTxnRef() {
        String raw = System.currentTimeMillis() + UUID.randomUUID().toString().replace("-", "");
        return raw.length() > 100 ? raw.substring(0, 100) : raw;
    }

    private String resolveClientIp(String clientIp) {
        if (clientIp == null || clientIp.isBlank()) {
            return "127.0.0.1";
        }
        // X-Forwarded-For may contain a list
        int comma = clientIp.indexOf(',');
        String ip = comma >= 0 ? clientIp.substring(0, comma).trim() : clientIp.trim();
        return ip.isEmpty() ? "127.0.0.1" : ip;
    }

    /** VNPay expects IPv4; localhost IPv6 from gateway breaks some sandbox checks. */
    private String toVnPayIpv4(String clientIp) {
        String ip = resolveClientIp(clientIp);
        if ("::1".equals(ip) || "0:0:0:0:0:0:0:1".equals(ip) || ip.contains(":")) {
            return "127.0.0.1";
        }
        return ip;
    }

    private String sanitizeOrderInfo(String invoiceCode) {
        if (invoiceCode == null) {
            return "ABMS";
        }
        return invoiceCode.replaceAll("[^A-Za-z0-9\\- ]", "");
    }
}
