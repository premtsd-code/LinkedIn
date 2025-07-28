package com.premtsd.linkedin.connectionservice;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@Slf4j
public class ConnectionsServiceApplication {

	public static void main(String[] args) {
		log.info("Starting Connections Service Application...");
		SpringApplication.run(ConnectionsServiceApplication.class, args);
		log.info("Connections Service Application started successfully");
	}

}
