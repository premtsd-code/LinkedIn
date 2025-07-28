package com.premtsd.linkedin.notificationservice;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication
@EnableFeignClients
@Slf4j
public class EmailServiceApplication {

    public static void main(String[] args) {
        log.info("Starting Notification Service Application...");
        SpringApplication.run(EmailServiceApplication.class, args);
        log.info("Notification Service Application started successfully");
    }

}
