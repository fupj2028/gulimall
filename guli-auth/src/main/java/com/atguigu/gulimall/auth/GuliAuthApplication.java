package com.atguigu.gulimall.auth;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.session.data.redis.config.annotation.web.http.EnableRedisIndexedHttpSession;

@EnableRedisIndexedHttpSession
@EnableFeignClients(basePackages = "com.atguigu.gulimall.auth.feign")
@SpringBootApplication
public class GuliAuthApplication {
	public static void main(String[] args) {
		SpringApplication.run(GuliAuthApplication.class, args);
	}

}
