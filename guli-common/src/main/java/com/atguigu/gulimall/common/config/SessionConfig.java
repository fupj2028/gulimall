// package com.atguigu.gulimall.common.config;
// 
// import org.springframework.context.annotation.Bean;
// import org.springframework.context.annotation.Configuration;
// import org.springframework.session.data.redis.config.annotation.web.http.EnableRedisIndexedHttpSession;
// import org.springframework.session.web.http.CookieSerializer;
// import org.springframework.session.web.http.DefaultCookieSerializer;
// 
// /**
//  * Spring Session + Redis 跨子域 session 共享配置
//  * 
//  * 适用场景：登录和商城分开部署在不同子域名下（如 auth.gulimall.com、product.gulimall.com）
//  * 启用后会把 SESSION cookie 的 domain 设为 .gulimall.com，子域名间共享
//  * 
//  * 当前部署使用统一域名 124.222.125.141 经网关分发，不需要此配置。
//  * 后续切换为子域名时，取消本文件全部注释即可。
//  */
// @EnableRedisIndexedHttpSession
// @Configuration
// public class SessionConfig {
// 
//     @Bean
//     public CookieSerializer cookieSerializer() {
//         DefaultCookieSerializer serializer = new DefaultCookieSerializer();
//         serializer.setCookieName("GULISESSION");
//         serializer.setDomainName("gulimall.com");
//         serializer.setCookiePath("/");
//         serializer.setUseHttpCookie(true);
//         return serializer;
//     }
// }