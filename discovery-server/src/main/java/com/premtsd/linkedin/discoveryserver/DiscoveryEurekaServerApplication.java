package com.premtsd.linkedin.discoveryserver;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.netflix.eureka.server.EnableEurekaServer;

@SpringBootApplication
@EnableEurekaServer
@Slf4j
public class DiscoveryEurekaServerApplication {

	public static void main(String[] args) {
		log.info("Starting Discovery Server (Eureka) Application...");
		SpringApplication.run(DiscoveryEurekaServerApplication.class, args);
		log.info("Discovery Server (Eureka) Application started successfully");
	}

}
