package com.atguigu.gulimall.order.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import com.atguigu.gulimall.order.intercepter.OrderIntercepter;

@Configuration
public class OrderWebConfig implements WebMvcConfigurer{
    private final OrderIntercepter orderIntercepter;


    OrderWebConfig(OrderIntercepter orderIntercepter) {
        this.orderIntercepter = orderIntercepter;
    }


    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(orderIntercepter)
                .addPathPatterns("/**")
                .excludePathPatterns("/login", "/static/**", "/order/order/list/**", "/pay/alipay/notify/**");
    }
}
