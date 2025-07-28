package com.premtsd.linkedin.userservice;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;


@SpringBootApplication(scanBasePackages = "com.premtsd.linkedin")
@Slf4j
public class UserServiceApplication {

	public static void main(String[] args) {
		log.info("Starting User Service Application...");
		SpringApplication.run(UserServiceApplication.class, args);
		log.info("User Service Application started successfully");
	}

}
