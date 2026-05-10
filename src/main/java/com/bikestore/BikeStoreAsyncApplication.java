package com.bikestore;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * BikeStore Async v1.0
 *
 * Arquitectura:
 * - OrderProducer: API REST que recibe órdenes
 * - PaymentWorker: Procesa pagos (50% fallo, 3 reintentos, DLQ)
 * - EmailWorker: Envía emails si pago OK
 * - RabbitMQ: Broker de mensajes asincrónico
 *
 * Flujo:
 * 1. Cliente POST a /api/orders/create
 * 2. OrderProducer publica en RabbitMQ
 * 3. PaymentWorker consume y procesa pago
 * 4. Si OK, EmailWorker envía confirmación
 * 5. Si falla 3 veces, va a Dead-Letter Queue
 *
 * Todo registrado con pedidoId, timestamp y thread
 */
@Slf4j
@SpringBootApplication
public class BikeStoreAsyncApplication {

	public static void main(String[] args) {
		SpringApplication.run(BikeStoreAsyncApplication.class, args);
		log.info("╔════════════════════════════════════════════════════════╗");
		log.info("║                                                        ║");
		log.info("║       🚴 BikeStore Async v1.0 - INICIADO 🚴           ║");
		log.info("║                                                        ║");
		log.info("║  ✓ API REST en http://localhost:8080                 ║");
		log.info("║  ✓ RabbitMQ Console en http://localhost:15672        ║");
		log.info("║    (usuario: admin, contraseña: admin123)            ║");
		log.info("║                                                        ║");
		log.info("║  Servicios activos:                                   ║");
		log.info("║  - OrderProducer (publica órdenes)                   ║");
		log.info("║  - PaymentWorker (procesa pagos)                     ║");
		log.info("║  - EmailWorker (envía confirmaciones)                ║");
		log.info("║                                                        ║");
		log.info("╚════════════════════════════════════════════════════════╝");
	}
}