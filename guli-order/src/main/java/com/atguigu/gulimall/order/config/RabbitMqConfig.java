package com.atguigu.gulimall.order.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.atguigu.gulimall.common.constant.MqConstant;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class RabbitMqConfig {

    @Bean
    public MessageConverter messageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public TopicExchange orderEventExchange() {
        return new TopicExchange(MqConstant.ORDER_EVENT_EXCHANGE, true, false);
    }

    @Bean
    public Queue orderDelayQueue() {
        Map<String, Object> args = new HashMap<>();
        args.put("x-dead-letter-exchange", MqConstant.ORDER_EVENT_EXCHANGE);
        args.put("x-dead-letter-routing-key", MqConstant.ORDER_RELEASE_ORDER_ROUTING);
        args.put("x-message-ttl", MqConstant.ORDER_RELEASE_TTL);
        return new Queue(MqConstant.ORDER_DELAY_QUEUE, true, false, false, args);
    }

    @Bean
    public Queue orderReleaseOrderQueue() {
        return new Queue(MqConstant.ORDER_RELEASE_ORDER_QUEUE, true, false, false);
    }

    @Bean
    public Binding orderCreateOrderBinding() {
        return BindingBuilder
                .bind(orderDelayQueue())
                .to(orderEventExchange())
                .with(MqConstant.ORDER_CREATE_ORDER_ROUTING);
    }

    @Bean
    public Binding orderReleaseOrderBinding() {
        return BindingBuilder
                .bind(orderReleaseOrderQueue())
                .to(orderEventExchange())
                .with(MqConstant.ORDER_RELEASE_ORDER_ROUTING);
    }

    //关单重试队列：消费失败重投到此，TTL 到期后死信回关单队列继续重试
    @Bean
    public Queue orderReleaseRetryQueue() {
        Map<String, Object> args = new HashMap<>();
        args.put("x-dead-letter-exchange", MqConstant.ORDER_EVENT_EXCHANGE);
        args.put("x-dead-letter-routing-key", MqConstant.ORDER_RELEASE_ORDER_ROUTING);
        args.put("x-message-ttl", MqConstant.ORDER_RELEASE_RETRY_TTL);
        return new Queue(MqConstant.ORDER_RELEASE_RETRY_QUEUE, true, false, false, args);
    }

    //关单最终死信队列：超过重试上限，留给人/补偿程序处理
    @Bean
    public Queue orderReleaseFinalQueue() {
        return new Queue(MqConstant.ORDER_RELEASE_FINAL_QUEUE, true, false, false);
    }

    @Bean
    public Binding orderReleaseRetryBinding() {
        return BindingBuilder
                .bind(orderReleaseRetryQueue())
                .to(orderEventExchange())
                .with(MqConstant.ORDER_RELEASE_RETRY_ROUTING);
    }

    @Bean
    public Binding orderReleaseFinalBinding() {
        return BindingBuilder
                .bind(orderReleaseFinalQueue())
                .to(orderEventExchange())
                .with(MqConstant.ORDER_RELEASE_FINAL_ROUTING);
    }
}
