package com.atguigu.gulimall.order.listener;

import com.atguigu.gulimall.common.constant.MqConstant;
import com.atguigu.gulimall.common.to.StockLockedTo;
import com.atguigu.gulimall.order.config.ReliableRabbitTemplate;
import com.atguigu.gulimall.order.service.OrderService;
import com.rabbitmq.client.Channel;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

import java.io.IOException;

@Slf4j
@Component
public class OrderCloseListener {

    @Autowired
    private OrderService orderService;

    @Autowired
    private ReliableRabbitTemplate rabbitTemplate;

    @RabbitListener(queues = MqConstant.ORDER_RELEASE_ORDER_QUEUE)
    public void releaseOrder(StockLockedTo to, Channel channel,
                             @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag,
                             @Header(value = MqConstant.RETRY_COUNT_HEADER, required = false) Integer retryCount)
            throws IOException {
        log.info("收到关单消息，orderSn={}, 重试次数={}", to.getOrderSn(), retryCount == null ? 0 : retryCount);
        try {
            orderService.closeOrder(to);
            //处理成功，确认消息
            channel.basicAck(deliveryTag, false);
        } catch (Exception e) {
            log.error("关单处理失败，orderSn={}", to.getOrderSn(), e);
            int count = retryCount == null ? 0 : retryCount;
            try {
                if (count >= MqConstant.ORDER_RELEASE_MAX_RETRY) {
                    //超过最大重试次数，进入最终死信队列，留给人/补偿程序处理
                    rabbitTemplate.convertAndSend(MqConstant.ORDER_EVENT_EXCHANGE,
                            MqConstant.ORDER_RELEASE_FINAL_ROUTING, to, msg -> {
                                msg.getMessageProperties().setHeader(MqConstant.RETRY_COUNT_HEADER, count + 1);
                                return msg;
                            });
                    log.error("关单消息超过最大重试次数，进入最终死信队列，orderSn={}", to.getOrderSn());
                } else {
                    //进入重试队列，TTL 到期后死信回本队列再次消费
                    rabbitTemplate.convertAndSend(MqConstant.ORDER_EVENT_EXCHANGE,
                            MqConstant.ORDER_RELEASE_RETRY_ROUTING, to, msg -> {
                                msg.getMessageProperties().setHeader(MqConstant.RETRY_COUNT_HEADER, count + 1);
                                return msg;
                            });
                    log.warn("关单消息进入重试队列，第{}次重试，orderSn={}", count + 1, to.getOrderSn());
                }
                //重发成功，确认原消息
                channel.basicAck(deliveryTag, false);
            } catch (Exception ex) {
                log.error("关单消息重发失败，重新入队，orderSn={}", to.getOrderSn(), ex);
                channel.basicNack(deliveryTag, false, true);
            }
        }
    }
}
