package com.csms.notification.service.impl;

import com.csms.notification.entity.AuditLog;
import com.csms.notification.repository.AuditLogRepository;
import com.csms.notification.service.AuditLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service @RequiredArgsConstructor
public class AuditLogServiceImpl implements AuditLogService {
 private final AuditLogRepository repository;
 public void log(UUID actorId,String action,String entityId){ AuditLog l=new AuditLog();l.setActorId(actorId);l.setAction(action);l.setEntityId(entityId);repository.save(l); }
}
