package com.premtsd.linkedin.uploader_service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@Slf4j
public class UploaderServiceApplication {

	public static void main(String[] args) {
		log.info("Starting Uploader Service Application...");
		SpringApplication.run(UploaderServiceApplication.class, args);
		log.info("Uploader Service Application started successfully");
	}

}
