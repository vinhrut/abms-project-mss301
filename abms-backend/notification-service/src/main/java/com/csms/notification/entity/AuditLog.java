package com.csms.notification.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity @Table(name="notification_audit_logs") @Getter @Setter @NoArgsConstructor
public class AuditLog {
 @Id @GeneratedValue(strategy=GenerationType.UUID) private UUID id;
 @Column(name="actor_id") private UUID actorId;
 @Column(nullable=false,length=80) private String action;
 @Column(name="entity_id",length=80) private String entityId;
 @Column(nullable=false) private LocalDateTime createdAt = LocalDateTime.now();
}
