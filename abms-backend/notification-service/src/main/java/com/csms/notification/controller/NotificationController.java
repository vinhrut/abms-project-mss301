package com.csms.notification.controller;

import com.csms.notification.dto.AnnouncementDTO;
import com.csms.notification.dto.NotificationDTO;
import com.csms.notification.dto.PageResponse;
import com.csms.notification.entity.NotificationType;
import com.csms.notification.entity.NotificationStatus;
import com.csms.notification.service.NotificationService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.validation.annotation.Validated;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@RestController @RequestMapping("/api/v1/notifications") @RequiredArgsConstructor @Validated
public class NotificationController {
 private final NotificationService service;
 @PostMapping("/announce")
 public ResponseEntity<NotificationDTO> create(@Valid @RequestBody AnnouncementDTO dto,
                                                @RequestHeader("X-User-Id") UUID uid,
                                                @RequestHeader("X-User-Role") String role,
                                                @RequestHeader(value = "X-Building-Id", required = false) UUID buildingId) {
  requireManager(role);
  return ResponseEntity.status(HttpStatus.CREATED).body(service.createAnnouncement(dto, uid, buildingId));
 }
 @PutMapping("/{id}/approve")
 public NotificationDTO approve(@PathVariable UUID id, @RequestHeader("X-User-Id") UUID uid,
                                @RequestHeader("X-User-Role") String role,
                                @RequestHeader(value = "X-Building-Id", required = false) UUID buildingId) {
  requireManager(role);
  return service.approve(id, uid, buildingId);
 }
 @PutMapping("/{id}/reject")
 public NotificationDTO reject(@PathVariable UUID id, @RequestParam @NotBlank @Size(max = 1000) String reason,
                               @RequestHeader("X-User-Id") UUID uid, @RequestHeader("X-User-Role") String role,
                               @RequestHeader(value = "X-Building-Id", required = false) UUID buildingId) {
  requireManager(role);
  return service.reject(id, uid, reason, buildingId);
 }
 @PutMapping("/{id}/cancel")
 public NotificationDTO cancel(@PathVariable UUID id, @RequestHeader("X-User-Id") UUID uid,
                               @RequestHeader("X-User-Role") String role,
                               @RequestHeader(value = "X-Building-Id", required = false) UUID buildingId) {
  requireManager(role);
  return service.cancel(id, uid, buildingId);
 }
 @GetMapping
 public PageResponse<NotificationDTO> list(@RequestHeader("X-User-Id") UUID uid,
                                           @RequestHeader("X-User-Role") String role,
                                           @RequestHeader(value = "X-Building-Id", required = false) UUID buildingId,
                                           @RequestParam(required = false) NotificationType type,
                                           @RequestParam(required = false) NotificationStatus status,
                                           @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                                           @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
                                           @RequestParam(required = false) String recipient,
                                           @RequestParam(defaultValue = "0") int page,
                                           @RequestParam(defaultValue = "10") int size) {
  LocalDateTime f = from == null ? null : from.atStartOfDay();
  LocalDateTime t = to == null ? null : to.plusDays(1).atStartOfDay().minusNanos(1);
  return service.list(uid, role, buildingId, type, status, f, t, recipient, page, size);
 }
 @GetMapping("/{id}")
 public NotificationDTO detail(@PathVariable UUID id, @RequestHeader("X-User-Id") UUID uid,
                               @RequestHeader("X-User-Role") String role,
                               @RequestHeader(value = "X-Building-Id", required = false) UUID buildingId) {
  return service.detail(id, uid, role, buildingId);
 }
 @PutMapping("/{id}/read")
 public NotificationDTO read(@PathVariable UUID id, @RequestHeader("X-User-Id") UUID uid,
                             @RequestHeader("X-User-Role") String role,
                             @RequestHeader(value = "X-Building-Id", required = false) UUID buildingId) {
  return service.markRead(id, uid, role, buildingId);
 }
 @PostMapping("/retry-failed")
 public java.util.Map<String, Integer> retry(@RequestHeader("X-User-Id") UUID uid,
                                             @RequestHeader("X-User-Role") String role,
                                             @RequestHeader(value = "X-Building-Id", required = false) UUID buildingId) {
  requireManager(role);
  return java.util.Map.of("retried", service.retryFailed(uid, buildingId));
 }
 private void requireManager(String role){String r=role==null?"":role.replace("ROLE_","").toUpperCase();if(!(r.equals("ADMIN")||r.equals("MANAGER")||r.equals("BUILDING_MANAGER")))throw new org.springframework.web.server.ResponseStatusException(HttpStatus.FORBIDDEN,"Notification Management permission is required");}
}
