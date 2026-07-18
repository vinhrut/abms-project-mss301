package com.csms.notification.controller;

import com.csms.notification.dto.InvoiceNotificationJobRunDTO;
import com.csms.notification.dto.PageResponse;
import com.csms.notification.service.InvoiceNotificationJobService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.YearMonth;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/notifications/invoice-jobs")
@RequiredArgsConstructor
public class InvoiceNotificationJobController {
    private final InvoiceNotificationJobService jobService;

    @GetMapping
    public PageResponse<InvoiceNotificationJobRunDTO> history(
        @RequestHeader("X-User-Role") String role,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "10") int size) {
        requireManager(role);
        return jobService.history(page, size);
    }

    @PostMapping("/run")
    public InvoiceNotificationJobRunDTO runNow(
        @RequestHeader("X-User-Id") UUID actorId,
        @RequestHeader("X-User-Role") String role,
        @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM") YearMonth period) {
        requireManager(role);
        return jobService.run(period == null ? YearMonth.now() : period, actorId, false);
    }

    @PostMapping("/{id}/retry")
    public InvoiceNotificationJobRunDTO retry(
        @PathVariable UUID id,
        @RequestHeader("X-User-Id") UUID actorId,
        @RequestHeader("X-User-Role") String role) {
        requireManager(role);
        return jobService.retry(id, actorId);
    }

    private void requireManager(String role) {
        String normalized = role == null ? "" : role.replace("ROLE_", "").toUpperCase();
        if (!(normalized.equals("ADMIN") || normalized.equals("MANAGER") || normalized.equals("BUILDING_MANAGER"))) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Notification Management permission is required");
        }
    }
}
