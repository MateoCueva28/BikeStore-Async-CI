package com.bikestore.config;

import org.springframework.amqp.core.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

/**
 * Configuración de RabbitMQ:
 * - Colas para órdenes, pagos, emails
 * - Dead-Letter Queue para reintentos fallidos
 * - Exchanges y bindings
 */
@Configuration
public class RabbitMQConfig {

    // ============ QUEUES ============
    public static final String ORDER_QUEUE = "bikestore.order.queue";
    public static final String PAYMENT_QUEUE = "bikestore.payment.queue";
    public static final String EMAIL_QUEUE = "bikestore.email.queue";
    public static final String DLQ_QUEUE = "bikestore.dlq.queue";

    // ============ EXCHANGES ============
    public static final String ORDER_EXCHANGE = "bikestore.order.exchange";
    public static final String PAYMENT_EXCHANGE = "bikestore.payment.exchange";
    public static final String EMAIL_EXCHANGE = "bikestore.email.exchange";
    public static final String DLQ_EXCHANGE = "bikestore.dlq.exchange";

    // ============ ROUTING KEYS ============
    public static final String ORDER_KEY = "order.created";
    public static final String PAYMENT_KEY = "payment.process";
    public static final String EMAIL_KEY = "email.send";
    public static final String DLQ_KEY = "dlq.failed";

    // ============ EXCHANGES ============
    @Bean
    public TopicExchange orderExchange() {
        return new TopicExchange(ORDER_EXCHANGE, true, false);
    }

    @Bean
    public TopicExchange paymentExchange() {
        return new TopicExchange(PAYMENT_EXCHANGE, true, false);
    }

    @Bean
    public TopicExchange emailExchange() {
        return new TopicExchange(EMAIL_EXCHANGE, true, false);
    }

    @Bean
    public TopicExchange dlqExchange() {
        return new TopicExchange(DLQ_EXCHANGE, true, false);
    }

    // ============ QUEUES ============
    @Bean
    public Queue orderQueue() {
        return QueueBuilder.durable(ORDER_QUEUE)
                .build();
    }

    @Bean
    public Queue paymentQueue() {
        return QueueBuilder.durable(PAYMENT_QUEUE)
                .build();
    }

    @Bean
    public Queue emailQueue() {
        return QueueBuilder.durable(EMAIL_QUEUE)
                .build();
    }

    @Bean
    public Queue dlqQueue() {
        return QueueBuilder.durable(DLQ_QUEUE)
                .build();
    }

    // ============ BINDINGS ============
    @Bean
    public Binding orderBinding(Queue orderQueue, TopicExchange orderExchange) {
        return BindingBuilder.bind(orderQueue)
                .to(orderExchange)
                .with(ORDER_KEY);
    }

    @Bean
    public Binding paymentBinding(Queue paymentQueue, TopicExchange paymentExchange) {
        return BindingBuilder.bind(paymentQueue)
                .to(paymentExchange)
                .with(PAYMENT_KEY);
    }

    @Bean
    public Binding emailBinding(Queue emailQueue, TopicExchange emailExchange) {
        return BindingBuilder.bind(emailQueue)
                .to(emailExchange)
                .with(EMAIL_KEY);
    }

    @Bean
    public Binding dlqBinding(Queue dlqQueue, TopicExchange dlqExchange) {
        return BindingBuilder.bind(dlqQueue)
                .to(dlqExchange)
                .with(DLQ_KEY);
    }

    @Bean
    public Jackson2JsonMessageConverter messageConverter() {

        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());

        return new Jackson2JsonMessageConverter(objectMapper);
    }
}