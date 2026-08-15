package com.atguigu.gulimall.cart;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Import;
import org.springframework.session.data.redis.config.annotation.web.http.EnableRedisIndexedHttpSession;

import com.atguigu.gulimall.common.config.LoginUserAdvice;

@EnableFeignClients
@EnableRedisIndexedHttpSession
@SpringBootApplication
@Import(LoginUserAdvice.class)
public class GuliCartApplication {

	public static void main(String[] args) {
		SpringApplication.run(GuliCartApplication.class, args);
	}

}
