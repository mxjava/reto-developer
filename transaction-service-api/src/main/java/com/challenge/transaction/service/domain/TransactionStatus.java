package com.challenge.transaction.service.domain;

import jakarta.persistence.EnumeratedValue;

public enum TransactionStatus {
    APROBADA("Aprobada"),
    CANCELADA("Cancelada");

    /*
     * Jakarta Persistence 3.2 utiliza este valor para persistir el enum.
     * Así la base guarda exactamente "Aprobada" y "Cancelada",
     * como solicita el reto técnico.
     */
    @EnumeratedValue private final String label;

    TransactionStatus(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }
}
