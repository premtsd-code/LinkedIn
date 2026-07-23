package com.premtsd.linkedin.notificationservice.repository;

import com.premtsd.linkedin.notificationservice.entity.ProcessedEvent;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProcessedEventRepository extends JpaRepository<ProcessedEvent, String> {
}
