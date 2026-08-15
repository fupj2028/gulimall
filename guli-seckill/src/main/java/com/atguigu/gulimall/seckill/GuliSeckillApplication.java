package com.atguigu.gulimall.seckill;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.session.data.redis.config.annotation.web.http.EnableRedisIndexedHttpSession;

@EnableFeignClients
@EnableScheduling
@EnableRedisIndexedHttpSession
@SpringBootApplication
public class GuliSeckillApplication {

	public static void main(String[] args) {
		SpringApplication.run(GuliSeckillApplication.class, args);
	}

}
