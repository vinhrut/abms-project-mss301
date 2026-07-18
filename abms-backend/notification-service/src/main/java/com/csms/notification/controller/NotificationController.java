package com.csms.notification.controller;

import com.csms.notification.dto.AnnouncementDTO;
import com.csms.notification.dto.NotificationDTO;
import com.csms.notification.dto.PageResponse;
import com.csms.notification.entity.NotificationType;
import com.csms.notification.entity.NotificationStatus;
import com.csms.notification.service.NotificationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@RestController @RequestMapping("/api/v1/notifications") @RequiredArgsConstructor
public class NotificationController {
 private final NotificationService service;
 @PostMapping("/announce") public ResponseEntity<NotificationDTO> create(@Valid @RequestBody AnnouncementDTO dto,@RequestHeader("X-User-Id") UUID uid,@RequestHeader("X-User-Role") String role){requireManager(role);return ResponseEntity.status(HttpStatus.CREATED).body(service.createAnnouncement(dto,uid));}
 @PutMapping("/{id}/approve") public NotificationDTO approve(@PathVariable UUID id,@RequestHeader("X-User-Id") UUID uid,@RequestHeader("X-User-Role") String role){requireManager(role);return service.approve(id,uid);}
 @GetMapping public PageResponse<NotificationDTO> list(@RequestHeader("X-User-Id") UUID uid,@RequestHeader("X-User-Role") String role,@RequestParam(required=false) NotificationType type,@RequestParam(required=false) NotificationStatus status,@RequestParam(required=false) @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate from,@RequestParam(required=false) @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate to,@RequestParam(required=false) String recipient,@RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="10") int size){LocalDateTime f=from==null?null:from.atStartOfDay();LocalDateTime t=to==null?null:to.plusDays(1).atStartOfDay().minusNanos(1);return service.list(uid,role,type,status,f,t,recipient,page,size);}
 @GetMapping("/{id}") public NotificationDTO detail(@PathVariable UUID id,@RequestHeader("X-User-Id") UUID uid,@RequestHeader("X-User-Role") String role){return service.detail(id,uid,role);}
 @PutMapping("/{id}/read") public NotificationDTO read(@PathVariable UUID id,@RequestHeader("X-User-Id") UUID uid,@RequestHeader("X-User-Role") String role){return service.markRead(id,uid,role);}
 @PostMapping("/retry-failed") public java.util.Map<String,Integer> retry(@RequestHeader("X-User-Id") UUID uid,@RequestHeader("X-User-Role") String role){requireManager(role);return java.util.Map.of("retried",service.retryFailed(uid));}
 private void requireManager(String role){String r=role==null?"":role.replace("ROLE_","").toUpperCase();if(!(r.equals("ADMIN")||r.equals("MANAGER")||r.equals("BUILDING_MANAGER")))throw new org.springframework.web.server.ResponseStatusException(HttpStatus.FORBIDDEN,"Notification Management permission is required");}
}
