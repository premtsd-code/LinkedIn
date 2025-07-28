package com.premtsd.linkedin.apigateway;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@Slf4j
public class ApiGatewayApplication {

	public static void main(String[] args) {
		log.info("Starting API Gateway Application...");
		SpringApplication.run(ApiGatewayApplication.class, args);
		log.info("API Gateway Application started successfully");
	}

}
