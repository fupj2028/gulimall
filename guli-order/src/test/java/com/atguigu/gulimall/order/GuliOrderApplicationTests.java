package com.atguigu.gulimall.order;

import com.atguigu.gulimall.order.entity.OrderEntity;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.AmqpAdmin;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.HashMap;
import java.util.Map;

@SpringBootTest
class GuliOrderApplicationTests {

    @Autowired
    private AmqpAdmin amqpAdmin;

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Test
    void contextLoads() {
    }

    @Test
    void testSendMessage() {
        rabbitTemplate.convertAndSend("hello-java-exchange", "hello.java", "Hello RabbitMQ!");
        System.out.println("消息发送成功");
    }

    @Test
    void testSendObjectMessage() {
        OrderEntity order = new OrderEntity();
        order.setId(1L);
        order.setOrderSn("20260728000001");
        order.setMemberId(1L);
        rabbitTemplate.convertAndSend("hello-java-exchange", "hello.java", order);
        System.out.println("对象消息发送成功");
    }

    @Test
    void testCreateExchange() {
        DirectExchange exchange = new DirectExchange("hello-java-exchange", true, false);
        amqpAdmin.declareExchange(exchange);
        System.out.println("交换机创建成功");
    }

    @Test
    void testCreateQueue() {
        Queue queue = new Queue("hello-java-queue", true, false, false);
        amqpAdmin.declareQueue(queue);
        System.out.println("队列创建成功");
    }

    @Test
    void testCreateBinding() {
        Binding binding = new Binding(
                "hello-java-queue",
                Binding.DestinationType.QUEUE,
                "hello-java-exchange",
                "hello.java",
                null
        );
        amqpAdmin.declareBinding(binding);
        System.out.println("绑定创建成功");
    }

}