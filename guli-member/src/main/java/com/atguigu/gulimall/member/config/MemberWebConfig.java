package com.atguigu.gulimall.member.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import com.atguigu.gulimall.member.intercepter.MemberIntercepter;

@Configuration
public class MemberWebConfig implements WebMvcConfigurer{
    private final MemberIntercepter memberIntercepter;


    MemberWebConfig(MemberIntercepter memberIntercepter) {
        this.memberIntercepter = memberIntercepter;
    }


    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(memberIntercepter)
                .addPathPatterns("/**")
                .excludePathPatterns("/login", "/static/**", "/member/**");
    }
}
