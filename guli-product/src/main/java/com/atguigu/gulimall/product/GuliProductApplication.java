package com.atguigu.gulimall.product;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Import;
import org.springframework.session.data.redis.config.annotation.web.http.EnableRedisIndexedHttpSession;

import com.atguigu.gulimall.common.config.LoginUserAdvice;

@EnableRedisIndexedHttpSession
@EnableCaching
@EnableDiscoveryClient
@EnableFeignClients(basePackages = "com.atguigu.gulimall.product.feign")
@SpringBootApplication
@Import(LoginUserAdvice.class)
@MapperScan("com.atguigu.gulimall.product.dao")
public class GuliProductApplication {

	public static void main(String[] args) {
		SpringApplication.run(GuliProductApplication.class, args);
	}

}
