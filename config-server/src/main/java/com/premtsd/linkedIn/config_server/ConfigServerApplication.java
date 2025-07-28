package com.premtsd.linkedIn.config_server;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.config.server.EnableConfigServer;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

@SpringBootApplication
@EnableConfigServer
@Slf4j
public class ConfigServerApplication {

	public static void main(String[] args) {
		log.info("Starting Config Server Application...");
		SpringApplication.run(ConfigServerApplication.class, args);
		log.info("Config Server Application started successfully");
	}

}
