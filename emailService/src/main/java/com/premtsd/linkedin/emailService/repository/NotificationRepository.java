package com.premtsd.linkedin.emailService.repository;

import com.premtsd.linkedin.emailService.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificationRepository extends JpaRepository<Notification, Long> {
}
