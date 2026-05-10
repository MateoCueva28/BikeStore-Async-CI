package com.bikestore.controller;

import com.bikestore.model.Order;
import com.bikestore.service.OrderProducer;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * API REST para recibir órdenes desde el frontend (web/mobile)
 *
 * El flujo es:
 * 1. Cliente confirma compra en la app/web
 * 2. Frontend hace POST a /api/orders/create
 * 3. Este controlador recibe la orden
 * 4. Invoca OrderProducer para publicarla en RabbitMQ
 * 5. Retorna respuesta al cliente inmediatamente (sin esperar procesamiento)
 */
@Slf4j
@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderProducer orderProducer;

    public OrderController(OrderProducer orderProducer) {
        this.orderProducer = orderProducer;
    }

    /**
     * Endpoint para crear una nueva orden
     * Recibe los datos del cliente y la bicicleta seleccionada
     */
    @PostMapping("/create")
    public ResponseEntity<Map<String, Object>> createOrder(@RequestBody OrderRequest request) {
        log.info("[REST API] Nueva solicitud de orden recibida");
        log.info("  └─ Cliente: {}", request.getCustomerEmail());

        try {
            // Construir la orden a partir de la solicitud
            Order order = new Order();
            order.setCustomerId(request.getCustomerId());
            order.setCustomerName(request.getCustomerName());
            order.setCustomerEmail(request.getCustomerEmail());
            order.setBikeName(request.getBikeName());
            order.setQuantity(request.getQuantity());
            order.setTotalAmount(request.getTotalAmount());
            order.setCardToken(request.getCardToken());  // Token (no PAN real)

            // Publicar en RabbitMQ
            orderProducer.publishOrder(order);

            // Responder inmediatamente al cliente (procesamiento es asincrónico)
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Orden recibida. Se procesará en breve.");
            response.put("orderId", order.getOrderId());
            response.put("status", "PENDING");

            log.info("[REST API] Respuesta enviada al cliente - Orden pendiente de procesar");
            return ResponseEntity.status(HttpStatus.ACCEPTED).body(response);

        } catch (Exception e) {
            log.error("[REST API] Error al crear orden", e);
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", "Error al procesar la orden");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    /**
     * Endpoint de health check
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        Map<String, String> response = new HashMap<>();
        response.put("status", "UP");
        response.put("service", "BikeStore Order Service");
        return ResponseEntity.ok(response);
    }

    /**
     * DTO para recibir datos de la orden desde el frontend
     */
    @Data
    public static class OrderRequest {
        private String customerId;
        private String customerName;
        private String customerEmail;
        private String bikeName;
        private Integer quantity;
        private BigDecimal totalAmount;
        private String cardToken;  // Token de tarjeta (tokenizado en frontend)
    }
}