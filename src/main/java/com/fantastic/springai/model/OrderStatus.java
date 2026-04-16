package com.fantastic.springai.model;

/**
 * Valeurs alignées sur le CHECK PostgreSQL (libellés minuscules).
 */
public enum OrderStatus {
    pending,
    confirmed,
    shipped,
    delivered,
    cancelled,
    refunded
}
