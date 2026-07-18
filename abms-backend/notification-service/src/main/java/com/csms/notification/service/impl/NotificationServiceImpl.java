package com.csms.notification.service.impl;

import com.csms.notification.dto.AnnouncementDTO;
import com.csms.notification.dto.NotificationDTO;
import com.csms.notification.dto.PageResponse;
import com.csms.notification.entity.*;
import com.csms.notification.repository.NotificationRecipientRepository;
import com.csms.notification.repository.NotificationRepository;
import com.csms.notification.service.AuditLogService;
import com.csms.notification.service.EmailGateway;
import com.csms.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.*;

@Service @RequiredArgsConstructor @Transactional
public class NotificationServiceImpl implements NotificationService {
 private final NotificationRepository notificationRepository;
 private final NotificationRecipientRepository recipientRepository;
 private final AuditLogService auditLogService;
 private final EmailGateway emailGateway;

 public NotificationDTO createAnnouncement(AnnouncementDTO dto, UUID actorId) {
  Notification n=new Notification(); n.setTitle(dto.getTitle().trim()); n.setContent(dto.getContent().trim());
  n.setPriority(dto.getPriority()==null?NotificationPriority.NORMAL:dto.getPriority()); n.setRecipientGroup(dto.getRecipientGroup().trim().toUpperCase());
  n.setChannels(new HashSet<>(dto.getChannels())); n.setScheduledAt(dto.getScheduledAt()); n.setCreatedBy(actorId); n.setType(NotificationType.ANNOUNCEMENT);
  n.setStatus(NotificationStatus.PENDING); n=notificationRepository.save(n); saveRecipients(n,dto.getRecipientIds());
  auditLogService.log(actorId,"CREATE_ANNOUNCEMENT",n.getNotificationId().toString()); return toDto(n,actorId);
 }
 public NotificationDTO approve(UUID id, UUID actorId){ Notification n=get(id); if(n.getStatus()!=NotificationStatus.PENDING) throw bad("Only PENDING notification can be approved");
  n.setApprovedBy(actorId);n.setApprovedAt(LocalDateTime.now()); notificationRepository.save(n);
  if(n.getScheduledAt()==null || !n.getScheduledAt().isAfter(LocalDateTime.now())) dispatch(n);
  auditLogService.log(actorId,"APPROVE_ANNOUNCEMENT",id.toString());return toDto(n,actorId);
 }
 @Transactional(readOnly=true)
 public PageResponse<NotificationDTO> list(UUID userId,String role,NotificationType type,NotificationStatus status,LocalDateTime from,LocalDateTime to,String recipient,int page,int size){
  boolean admin=isManager(role); Pageable p=PageRequest.of(Math.max(page,0),Math.min(Math.max(size,1),100),Sort.by(Sort.Direction.DESC,"createdAt"));
  Page<Notification> result=notificationRepository.searchVisible(userId,normalizeRole(role),admin,type,status,from,to,blankToNull(recipient),p);
  return new PageResponse<>(result.map(n->toDto(n,userId)).getContent(),result.getNumber(),result.getSize(),result.getTotalElements(),result.getTotalPages());
 }
 public NotificationDTO detail(UUID id,UUID userId,String role){ Notification n=get(id); ensureVisible(n,userId,role); auditLogService.log(userId,"VIEW_NOTIFICATION",id.toString()); return toDto(n,userId); }
 public NotificationDTO markRead(UUID id,UUID userId,String role){ Notification n=get(id); ensureVisible(n,userId,role); NotificationRecipient r=recipientRepository.findByNotificationNotificationIdAndUserId(id,userId).orElseGet(()->{NotificationRecipient x=new NotificationRecipient();x.setNotification(n);x.setUserId(userId);return x;}); r.setRead(true);r.setReadAt(LocalDateTime.now());recipientRepository.save(r);auditLogService.log(userId,"MARK_NOTIFICATION_READ",id.toString());return toDto(n,userId); }
 public void dispatch(Notification n){ try { if(n.getChannels().contains(DeliveryChannel.EMAIL)) emailGateway.send(n); n.setStatus(NotificationStatus.SENT);n.setSentAt(LocalDateTime.now());n.setFailureReason(null); } catch(Exception e){n.setStatus(NotificationStatus.FAILED);n.setFailureReason(e.getMessage());} notificationRepository.save(n); }
 @Scheduled(fixedDelayString="${notification.dispatch-delay-ms:60000}") public int dispatchDue(){ List<Notification> due=notificationRepository.findByStatusAndScheduledAtLessThanEqual(NotificationStatus.PENDING,LocalDateTime.now());due.forEach(this::dispatch);return due.size(); }
 public int retryFailed(UUID actorId){ List<Notification> failed=notificationRepository.findByStatus(NotificationStatus.FAILED);failed.forEach(this::dispatch);auditLogService.log(actorId,"RETRY_FAILED_NOTIFICATIONS",String.valueOf(failed.size()));return failed.size(); }
 public NotificationDTO createInvoiceNotification(String title,String content,Set<UUID> ids,UUID actorId){ AnnouncementDTO dto=new AnnouncementDTO();dto.setTitle(title);dto.setContent(content);dto.setPriority(NotificationPriority.HIGH);dto.setRecipientGroup("RESIDENT");dto.setChannels(Set.of(DeliveryChannel.IN_APP,DeliveryChannel.EMAIL));dto.setRecipientIds(ids);NotificationDTO created=createAnnouncement(dto,actorId);Notification n=get(created.getId());n.setType(NotificationType.INVOICE);notificationRepository.save(n);dispatch(n);return toDto(n,actorId); }
 private void saveRecipients(Notification n,Set<UUID> ids){if(ids==null)return;ids.stream().filter(Objects::nonNull).distinct().forEach(id->{NotificationRecipient r=new NotificationRecipient();r.setNotification(n);r.setUserId(id);recipientRepository.save(r);});}
 private Notification get(UUID id){return notificationRepository.findById(id).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Notification not found"));}
 private void ensureVisible(Notification n,UUID userId,String role){if(isManager(role)||"ALL".equalsIgnoreCase(n.getRecipientGroup())||normalizeRole(role).equalsIgnoreCase(n.getRecipientGroup())||recipientRepository.existsByNotificationNotificationIdAndUserId(n.getNotificationId(),userId))return;throw new ResponseStatusException(HttpStatus.FORBIDDEN,"Notification is outside your access scope");}
 private NotificationDTO toDto(Notification n,UUID uid){boolean read=uid!=null&&recipientRepository.findByNotificationNotificationIdAndUserId(n.getNotificationId(),uid).map(NotificationRecipient::isRead).orElse(false);return NotificationDTO.builder().id(n.getNotificationId()).title(n.getTitle()).content(n.getContent()).type(n.getType()).priority(n.getPriority()).recipientGroup(n.getRecipientGroup()).channels(n.getChannels()).status(n.getStatus()).createdAt(n.getCreatedAt()).scheduledAt(n.getScheduledAt()).sentAt(n.getSentAt()).read(read).failureReason(n.getFailureReason()).build();}
 private boolean isManager(String r){String x=normalizeRole(r);return x.equals("ADMIN")||x.equals("MANAGER")||x.equals("BUILDING_MANAGER");}
 private String normalizeRole(String r){return r==null?"RESIDENT":r.replace("ROLE_","").toUpperCase();}
 private String blankToNull(String s){return s==null||s.isBlank()?null:s.trim();}
 private ResponseStatusException bad(String m){return new ResponseStatusException(HttpStatus.BAD_REQUEST,m);}
}
