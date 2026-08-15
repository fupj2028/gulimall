package com.atguigu.gulimall.cart.config;

// import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import com.atguigu.gulimall.cart.intercepter.CartIntercepter;

@Configuration
public class CartWebConfig implements WebMvcConfigurer{
    private final CartIntercepter cartIntercepter;


    CartWebConfig(CartIntercepter cartIntercepter) {
        this.cartIntercepter = cartIntercepter;
    }


    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(cartIntercepter);
    }
}
