package com.bikestore.service;

import com.bikestore.config.RabbitMQConfig;
import com.bikestore.model.Order;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;

/**
 * EmailWorker: consumidor que envía emails de confirmación
 *
 * Lógica:
 * 1. Escucha la cola de emails
 * 2. Verifica que el pago fue exitoso (status = PAID)
 * 3. Si es así, envía email de confirmación
 * 4. Registra el evento con pedidoId, timestamp y thread
 */
@Slf4j
@Service
public class EmailWorker {

    /**
     * Escucha la cola de emails y envía confirmaciones
     */
    @RabbitListener(queues = RabbitMQConfig.EMAIL_QUEUE)
    public void sendConfirmationEmail(Order order) {
        log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        log.info("[EMAIL WORKER] Procesando solicitud de email");
        log.info("  ├─ Orden ID: {}", order.getOrderId());
        log.info("  ├─ Cliente: {} ({})", order.getCustomerName(), order.getCustomerEmail());
        log.info("  ├─ Estado de pago: {}", order.getStatus());
        log.info("  ├─ Timestamp: {}", System.currentTimeMillis());
        log.info("  └─ Thread: {}", Thread.currentThread().getName());
        log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");

        // Verificar que el pago fue exitoso antes de enviar email
        if ("PAID".equals(order.getStatus())) {
            simulateSendingEmail(order);

            log.info("[EMAIL WORKER] EMAIL ENVIADO EXITOSAMENTE");
            log.info("  ├─ Orden ID: {}", order.getOrderId());
            log.info("  ├─ Destinatario: {}", order.getCustomerEmail());
            log.info("  ├─ Asunto: Pedido #{} - Confirmación de compra", order.getOrderId());
            log.info("  ├─ Contenido: Gracias por comprar {} x{}",
                    order.getBikeName(), order.getQuantity());
            log.info("  └─ Thread: {}", Thread.currentThread().getName());

        } else {
            log.warn("⚠[EMAIL WORKER] NO se envía email - Pago no completado");
            log.warn("  ├─ Orden ID: {}", order.getOrderId());
            log.warn("  ├─ Estado actual: {}", order.getStatus());
            log.warn("  └─ Se requiere pago exitoso para enviar confirmación");
        }
    }

    /**
     * Simula el envío de email (en producción sería con un proveedor real)
     */
    private void simulateSendingEmail(Order order) {
        try {
            // Simula latencia de envío
            Thread.sleep(800);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // En producción aquí irían integraciones con SendGrid, AWS SES, etc.
        log.debug("Email simulado enviado a: {}", order.getCustomerEmail());
    }
}