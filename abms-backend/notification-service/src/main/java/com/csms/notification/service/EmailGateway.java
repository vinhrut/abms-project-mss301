package com.csms.notification.service;
import com.csms.notification.entity.Notification;
public interface EmailGateway { void send(Notification notification); }
