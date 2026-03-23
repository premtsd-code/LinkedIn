package com.premtsd.linkedin.connectionservice;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
@EnabledIfEnvironmentVariable(named = "DOCKER_AVAILABLE", matches = "true")
class ConnectionsServiceApplicationTests {

	@Test
	void contextLoads() {
	}

}
