package com.atguigu.gulimall.search;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Import;
import org.springframework.session.data.redis.config.annotation.web.http.EnableRedisIndexedHttpSession;

import com.atguigu.gulimall.common.config.LoginUserAdvice;

@EnableRedisIndexedHttpSession
@EnableFeignClients
@EnableDiscoveryClient
@SpringBootApplication(exclude = DataSourceAutoConfiguration.class)
@Import(LoginUserAdvice.class)
public class GuliSearchApplication {

    public static void main(String[] args) {
        SpringApplication.run(GuliSearchApplication.class, args);
    }
}
