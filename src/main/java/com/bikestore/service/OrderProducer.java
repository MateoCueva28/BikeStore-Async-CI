package com.bikestore.service;

import com.bikestore.config.RabbitMQConfig;
import com.bikestore.model.Order;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * OrderProducer: publica órdenes en RabbitMQ
 * Cuando un cliente confirma su orden, este servicio:
 * 1. Recibe los datos de la orden
 * 2. Crea un mensaje JSON
 * 3. Lo publica en el broker
 */
@Slf4j
@Service
public class OrderProducer {

    private final RabbitTemplate rabbitTemplate;

    public OrderProducer(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    /**
     * Publica una orden en la cola de procesamiento
     * El mensaje se enruta a través del exchange hacia PaymentWorker y EmailWorker
     */
    public void publishOrder(Order order) {
        // Asigna un ID único a la orden
        order.setOrderId(UUID.randomUUID().toString());
        order.setOrderDate(LocalDateTime.now());
        order.setStatus("PENDING");

        log.info("═══════════════════════════════════════════════════════════════");
        log.info("[ORDER PRODUCER] Publicando orden");
        log.info("  ├─ Orden ID: {}", order.getOrderId());
        log.info("  ├─ Cliente: {} ({})", order.getCustomerName(), order.getCustomerEmail());
        log.info("  ├─ Bicicleta: {} x{}", order.getBikeName(), order.getQuantity());
        log.info("  ├─ Monto: ${}", order.getTotalAmount());
        log.info("  ├─ Timestamp: {}", order.getOrderDate());
        log.info("  └─ Thread: {}", Thread.currentThread().getName());
        log.info("═══════════════════════════════════════════════════════════════");

        // Publica en el exchange order, con routing key order.created
        rabbitTemplate.convertAndSend(
                RabbitMQConfig.PAYMENT_EXCHANGE,
                RabbitMQConfig.PAYMENT_KEY,
                order
        );

        log.debug("Orden publicada exitosamente en el broker");
    }
}