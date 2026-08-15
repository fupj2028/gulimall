package com.atguigu.gulimall.order.config;

import jakarta.annotation.PostConstruct;
import org.springframework.amqp.core.MessagePostProcessor;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 可靠消息发送：发布确认 + 退回回调，失败自动重发（上限后放弃并告警）
 * 防止消息在"发送方"环节丢失。
 */
@Slf4j
@Component
public class ReliableRabbitTemplate {

    private static final int MAX_RETRY = 3;

    @Autowired
    private RabbitTemplate rabbitTemplate;

    private final Map<String, PendingMessage> pendingMessages = new ConcurrentHashMap<>();

    @PostConstruct
    public void init() {
        //发布确认：ack=true 表示消息已到达交换机，立即移出 map 防止内存泄漏；
        //ack=false 表示未到达交换机，需要补偿重发。
        //注意：路由失败的消息，RabbitMQ 会先发 Basic.Return（由 returns 回调处理重发）再发 ack，
        //所以这里 ack=true 时即使该消息后来被退回，退回路径也已处理过，此处无需重复处理。
        rabbitTemplate.setConfirmCallback((correlationData, ack, cause) -> {
            if (correlationData == null) {
                return;
            }
            PendingMessage pending = pendingMessages.remove(correlationData.getId());
            if (pending == null || ack) {
                return;
            }
            retryOrLog(pending, "消息未到达交换机, cause=" + cause);
        });

        //退回回调：mandatory=true 时路由失败会触发，补偿重发
        rabbitTemplate.setReturnsCallback(returned -> {
            String correlationId = returned.getMessage().getMessageProperties().getCorrelationId();
            if (correlationId == null) {
                log.error("消息被退回但无correlationId, routingKey={}, reply={}",
                        returned.getRoutingKey(), returned.getReplyText());
                return;
            }
            PendingMessage pending = pendingMessages.remove(correlationId);
            if (pending == null) {
                return;
            }
            retryOrLog(pending, "消息未路由到任何队列, reply=" + returned.getReplyText());
        });
    }

    public void convertAndSend(String exchange, String routingKey, Object message) {
        String id = UUID.randomUUID().toString();
        pendingMessages.put(id, new PendingMessage(exchange, routingKey, message, 0));
        rabbitTemplate.convertAndSend(exchange, routingKey, message, new CorrelationData(id));
    }

    public void convertAndSend(String exchange, String routingKey, Object message, MessagePostProcessor postProcessor) {
        String id = UUID.randomUUID().toString();
        pendingMessages.put(id, new PendingMessage(exchange, routingKey, message, 0, postProcessor));
        rabbitTemplate.convertAndSend(exchange, routingKey, message, postProcessor, new CorrelationData(id));
    }

    private void retryOrLog(PendingMessage pending, String reason) {
        if (pending.retryCount >= MAX_RETRY) {
            log.error("消息发送最终失败, exchange={}, routingKey={}, reason={}",
                    pending.exchange, pending.routingKey, reason);
            return;
        }
        pending.retryCount++;
        String newId = UUID.randomUUID().toString();
        pendingMessages.put(newId, pending);
        if (pending.postProcessor != null) {
            rabbitTemplate.convertAndSend(pending.exchange, pending.routingKey, pending.message,
                    pending.postProcessor, new CorrelationData(newId));
        } else {
            rabbitTemplate.convertAndSend(pending.exchange, pending.routingKey, pending.message, new CorrelationData(newId));
        }
        log.warn("消息重发第{}次, exchange={}, routingKey={}, reason={}",
                pending.retryCount, pending.exchange, pending.routingKey, reason);
    }

    private static class PendingMessage {
        String exchange;
        String routingKey;
        Object message;
        int retryCount;
        MessagePostProcessor postProcessor;

        PendingMessage(String exchange, String routingKey, Object message, int retryCount) {
            this(exchange, routingKey, message, retryCount, null);
        }

        PendingMessage(String exchange, String routingKey, Object message, int retryCount,
                       MessagePostProcessor postProcessor) {
            this.exchange = exchange;
            this.routingKey = routingKey;
            this.message = message;
            this.retryCount = retryCount;
            this.postProcessor = postProcessor;
        }
    }
}
