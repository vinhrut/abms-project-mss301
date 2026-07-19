package com.csms.notification.service.impl;
import com.csms.notification.entity.Notification; import com.csms.notification.service.EmailGateway;
import lombok.extern.slf4j.Slf4j; import org.springframework.stereotype.Component;
@Component @Slf4j
public class LoggingEmailGateway implements EmailGateway {
 public void send(Notification n) { log.info("EMAIL notification={} group={} title={}", n.getNotificationId(), n.getRecipientGroup(), n.getTitle()); }
}
