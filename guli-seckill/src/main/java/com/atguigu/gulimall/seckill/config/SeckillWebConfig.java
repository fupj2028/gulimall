package com.atguigu.gulimall.seckill.config;

import com.atguigu.gulimall.seckill.intercepter.SeckillLoginInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class SeckillWebConfig implements WebMvcConfigurer {

    private final SeckillLoginInterceptor seckillLoginInterceptor;

    SeckillWebConfig(SeckillLoginInterceptor seckillLoginInterceptor) {
        this.seckillLoginInterceptor = seckillLoginInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(seckillLoginInterceptor).addPathPatterns("/kill");
    }
}