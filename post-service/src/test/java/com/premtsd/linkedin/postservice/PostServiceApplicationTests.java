package com.premtsd.linkedin.postservice;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class PostServiceApplicationTests {

	@Autowired
	private RedisTemplate<String, String> redisTemplate;

	@Test
	void testRedisConnection() {
	}
}
