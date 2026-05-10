package com.bikestore.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Representa una orden de compra de bicicletas
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Order implements Serializable {

    private static final long serialVersionUID = 1L;

    private String orderId;
    private String customerId;
    private String customerEmail;
    private String customerName;
    private BigDecimal totalAmount;
    private String bikeName;
    private Integer quantity;
    private LocalDateTime orderDate;
    private String cardToken;  // Token de tarjeta (no PAN real)
    private String status;     // PENDING, PAID, FAILED
}
