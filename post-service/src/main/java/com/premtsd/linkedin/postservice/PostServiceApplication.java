package com.premtsd.linkedin.postservice;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication
@EnableFeignClients
@EnableCaching
@Slf4j
public class PostServiceApplication {

	public static void main(String[] args) {
		log.info("Starting Post Service Application...");
		SpringApplication.run(PostServiceApplication.class, args);
		log.info("Post Service Application started successfully");
	}

}
