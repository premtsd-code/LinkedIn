package com.premtsd.linkedin.notificationservice.repository;

import com.premtsd.linkedin.notificationservice.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificationRepository extends JpaRepository<Notification, Long> {
}
