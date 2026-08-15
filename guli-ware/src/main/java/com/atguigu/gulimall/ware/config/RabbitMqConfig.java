package com.atguigu.gulimall.ware.config;

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
    public TopicExchange stockEventExchange() {
        return new TopicExchange(MqConstant.STOCK_EVENT_EXCHANGE, true, false);
    }

    @Bean
    public Queue stockReleaseStockQueue() {
        return new Queue(MqConstant.STOCK_RELEASE_STOCK_QUEUE, true, false, false);
    }

    @Bean
    public Binding stockReleaseBinding() {
        return BindingBuilder
                .bind(stockReleaseStockQueue())
                .to(stockEventExchange())
                .with(MqConstant.STOCK_RELEASE_ROUTING);
    }

    @Bean
    public Queue stockDelayQueue() {
        Map<String, Object> args = new HashMap<>();
        args.put("x-dead-letter-exchange", MqConstant.STOCK_EVENT_EXCHANGE);
        args.put("x-dead-letter-routing-key", MqConstant.STOCK_RELEASE_ROUTING);
        args.put("x-message-ttl", MqConstant.ORDER_RELEASE_TTL);
        return new Queue(MqConstant.STOCK_DELAY_QUEUE, true, false, false, args);
    }

    @Bean
    public Binding stockLockDelayBinding() {
        return BindingBuilder
                .bind(stockDelayQueue())
                .to(stockEventExchange())
                .with(MqConstant.STOCK_LOCK_DELAY_ROUTING);
    }

    //库存解锁重试队列：消费失败重投到此，TTL 到期后死信回库存解锁队列继续重试
    @Bean
    public Queue stockReleaseRetryQueue() {
        Map<String, Object> args = new HashMap<>();
        args.put("x-dead-letter-exchange", MqConstant.STOCK_EVENT_EXCHANGE);
        args.put("x-dead-letter-routing-key", MqConstant.STOCK_RELEASE_ROUTING);
        args.put("x-message-ttl", MqConstant.STOCK_RELEASE_RETRY_TTL);
        return new Queue(MqConstant.STOCK_RELEASE_RETRY_QUEUE, true, false, false, args);
    }

    //库存解锁最终死信队列：超过重试上限，留给人/补偿程序处理
    @Bean
    public Queue stockReleaseFinalQueue() {
        return new Queue(MqConstant.STOCK_RELEASE_FINAL_QUEUE, true, false, false);
    }

    @Bean
    public Binding stockReleaseRetryBinding() {
        return BindingBuilder
                .bind(stockReleaseRetryQueue())
                .to(stockEventExchange())
                .with(MqConstant.STOCK_RELEASE_RETRY_ROUTING);
    }

    @Bean
    public Binding stockReleaseFinalBinding() {
        return BindingBuilder
                .bind(stockReleaseFinalQueue())
                .to(stockEventExchange())
                .with(MqConstant.STOCK_RELEASE_FINAL_ROUTING);
    }

    //库存扣减队列：支付成功后将锁定库存转为实销
    @Bean
    public Queue stockDeductStockQueue() {
        return new Queue(MqConstant.STOCK_DEDUCT_STOCK_QUEUE, true, false, false);
    }

    @Bean
    public Binding stockDeductBinding() {
        return BindingBuilder
                .bind(stockDeductStockQueue())
                .to(stockEventExchange())
                .with(MqConstant.STOCK_DEDUCT_ROUTING);
    }

    //库存扣减重试队列：消费失败重投到此，TTL 到期后死信回库存扣减队列继续重试
    @Bean
    public Queue stockDeductRetryQueue() {
        Map<String, Object> args = new HashMap<>();
        args.put("x-dead-letter-exchange", MqConstant.STOCK_EVENT_EXCHANGE);
        args.put("x-dead-letter-routing-key", MqConstant.STOCK_DEDUCT_ROUTING);
        args.put("x-message-ttl", MqConstant.STOCK_DEDUCT_RETRY_TTL);
        return new Queue(MqConstant.STOCK_DEDUCT_RETRY_QUEUE, true, false, false, args);
    }

    //库存扣减最终死信队列：超过重试上限，留给人/补偿程序处理
    @Bean
    public Queue stockDeductFinalQueue() {
        return new Queue(MqConstant.STOCK_DEDUCT_FINAL_QUEUE, true, false, false);
    }

    @Bean
    public Binding stockDeductRetryBinding() {
        return BindingBuilder
                .bind(stockDeductRetryQueue())
                .to(stockEventExchange())
                .with(MqConstant.STOCK_DEDUCT_RETRY_ROUTING);
    }

    @Bean
    public Binding stockDeductFinalBinding() {
        return BindingBuilder
                .bind(stockDeductFinalQueue())
                .to(stockEventExchange())
                .with(MqConstant.STOCK_DEDUCT_FINAL_ROUTING);
    }
}
