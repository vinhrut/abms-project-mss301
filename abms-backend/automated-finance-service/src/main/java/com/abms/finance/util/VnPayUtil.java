package com.abms.finance.util;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

public final class VnPayUtil {

    private VnPayUtil() {
    }

    /**
     * Hash and query MUST use the same encoding. Mixing raw spaces in hash with '+' in the
     * query makes VNPay reject the URL (Error.html?code=71 → JS "timer is not defined").
     */
    public static String buildHashData(Map<String, String> params) {
        return joinEncoded(params);
    }

    public static String buildQueryString(Map<String, String> params) {
        return joinEncoded(params);
    }

    private static String joinEncoded(Map<String, String> params) {
        List<String> fieldNames = new ArrayList<>(params.keySet());
        Collections.sort(fieldNames);
        StringBuilder sb = new StringBuilder();
        boolean first = true;
        for (String fieldName : fieldNames) {
            String fieldValue = params.get(fieldName);
            if (fieldValue != null && !fieldValue.isEmpty()) {
                if (!first) {
                    sb.append('&');
                }
                first = false;
                sb.append(URLEncoder.encode(fieldName, StandardCharsets.US_ASCII));
                sb.append('=');
                sb.append(URLEncoder.encode(fieldValue, StandardCharsets.US_ASCII));
            }
        }
        return sb.toString();
    }

    public static String hmacSHA512(String key, String data) {
        try {
            Mac hmac512 = Mac.getInstance("HmacSHA512");
            SecretKeySpec secretKey = new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "HmacSHA512");
            hmac512.init(secretKey);
            byte[] bytes = hmac512.doFinal(data.getBytes(StandardCharsets.UTF_8));
            StringBuilder hash = new StringBuilder(bytes.length * 2);
            for (byte b : bytes) {
                hash.append(String.format("%02x", b));
            }
            return hash.toString();
        } catch (Exception ex) {
            throw new IllegalStateException("Unable to compute VNPay HMAC SHA512", ex);
        }
    }

    public static boolean isValidSecureHash(Map<String, String> params, String secureHash, String hashSecret) {
        if (secureHash == null || secureHash.isBlank() || hashSecret == null || hashSecret.isBlank()) {
            return false;
        }
        String calculated = hmacSHA512(hashSecret.trim(), buildHashData(params));
        return calculated.equalsIgnoreCase(secureHash);
    }
}
