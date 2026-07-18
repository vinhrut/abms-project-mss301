package com.csms.notification.service;
import java.util.UUID;
public interface AuditLogService { void log(UUID actorId, String action, String entityId); }
