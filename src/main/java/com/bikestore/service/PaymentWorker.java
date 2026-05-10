package com.bikestore.service;

import com.bikestore.config.RabbitMQConfig;
import com.bikestore.model.Order;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

import java.util.Random;

/**
 * PaymentWorker: consumidor que procesa pagos
 *
 * Lógica:
 * 1. Recibe una orden del broker
 * 2. Intenta procesar el pago (50% probabilidad de fallo)
 * 3. Si falla, reintenta automáticamente (hasta 3 reintentos)
 * 4. Si agota reintentos, envía a Dead-Letter Queue
 * 5. Si paga OK, publica evento para que EmailWorker envíe confirmación
 */
@Slf4j
@Service
public class PaymentWorker {

    private final RabbitTemplate rabbitTemplate;
    private final Random random = new Random();
    private static final int MAX_RETRIES = 3;

    public PaymentWorker(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    /**
     * Escucha la cola de pagos y procesa cada orden
     */
    @RabbitListener(queues = RabbitMQConfig.PAYMENT_QUEUE)
    public void processPayment(Order order, Message message) {
        log.info("───────────────────────────────────────────────────────────────");
        log.info("[PAYMENT WORKER] Recibida orden para procesar pago");
        log.info("  ├─ Orden ID: {}", order.getOrderId());
        log.info("  ├─ Monto: ${}", order.getTotalAmount());
        log.info("  ├─ Timestamp: {}", System.currentTimeMillis());
        log.info("  └─ Thread: {}", Thread.currentThread().getName());
        log.info("───────────────────────────────────────────────────────────────");

        // Obtener el número de reintentos del header del mensaje
        int retryCount = getRetryCount(message);

        // Simular procesamiento de pago (50% de fallo)
        boolean paymentSuccess = simulatePaymentGateway();

        if (paymentSuccess) {
            // ÉXITO: marcar orden como pagada y publicar evento para email
            order.setStatus("PAID");

            log.info("[PAYMENT WORKER] PAGO EXITOSO");
            log.info("  ├─ Orden ID: {}", order.getOrderId());
            log.info("  ├─ Estado: {}", order.getStatus());
            log.info("  └─ Thread: {}", Thread.currentThread().getName());

            // Publica evento de pago completado para que EmailWorker lo procese
            rabbitTemplate.convertAndSend(
                    RabbitMQConfig.EMAIL_EXCHANGE,
                    RabbitMQConfig.EMAIL_KEY,
                    order
            );
            log.info("Evento de pago exitoso publicado para email");

        } else {
            // FALLO: reintentar si no hemos superado máximo
            if (retryCount < MAX_RETRIES) {
                log.warn("️ [PAYMENT WORKER] PAGO FALLIDO - Reintentando ({}/{}) ",
                        retryCount + 1, MAX_RETRIES);
                log.warn("  ├─ Orden ID: {}", order.getOrderId());
                log.warn("  └─ Thread: {}", Thread.currentThread().getName());

                // Re-publicar en la misma cola para reintentar
                int newRetryCount = retryCount + 1;
                rabbitTemplate.convertAndSend(
                        RabbitMQConfig.PAYMENT_EXCHANGE,
                        RabbitMQConfig.PAYMENT_KEY,
                        order,
                        message1 -> {
                            message1.getMessageProperties()
                                    .setHeader("x-retry-count", newRetryCount);
                            return message1;
                        }
                );

            } else {
                // FRACASO DEFINITIVO: enviar a Dead-Letter Queue
                order.setStatus("FAILED");

                log.error(" [PAYMENT WORKER] PAGO FALLIDO DESPUÉS DE {} REINTENTOS", MAX_RETRIES);
                log.error("  ├─ Orden ID: {}", order.getOrderId());
                log.error("  ├─ Cliente: {}", order.getCustomerEmail());
                log.error("  ├─ Monto: ${}", order.getTotalAmount());
                log.error("  └─ Se envía a Dead-Letter Queue para análisis");

                // Enviar a DLQ para análisis offline
                rabbitTemplate.convertAndSend(
                        RabbitMQConfig.DLQ_EXCHANGE,
                        RabbitMQConfig.DLQ_KEY,
                        order
                );
            }
        }
    }

    /**
     * Simula el gateway de pagos con 50% de probabilidad de fallo
     */
    private boolean simulatePaymentGateway() {
        // Simula latencia de red
        try {
            Thread.sleep(500 + random.nextInt(1000));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // 50% de éxito, 50% de fallo
        return random.nextDouble() > 0.5;
    }

    /**
     * Extrae el número de reintentos del header del mensaje
     */
    private int getRetryCount(Message message) {
        Integer retryCount = (Integer) message.getMessageProperties()
                .getHeader("x-retry-count");
        return retryCount != null ? retryCount : 0;
    }
}