package com.challenge.transaction.service.infrastructure.persistence.entity;

import com.challenge.transaction.service.domain.TransactionStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(
        name = "transactions",
        indexes =
                @Index(
                        name = "idx_transactions_referencia",
                        columnList = "referencia",
                        unique = true))
public class TransactionEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "operacion", nullable = false, length = 30)
    private String operation;

    @Column(name = "importe", nullable = false, precision = 14, scale = 2)
    private BigDecimal amount;

    @Column(name = "cliente", nullable = false, length = 100)
    private String customer;

    @Column(name = "referencia", nullable = false, unique = true, length = 6)
    private String reference;

    /*
     * Jakarta Persistence usa el valor definido con @EnumeratedValue en TransactionStatus,
     * manteniendo exactamente "Aprobada" y "Cancelada" en la base de datos.
     */
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "estatus", nullable = false, length = 20)
    private TransactionStatus status;

    @Column(name = "secreto", nullable = false, length = 500)
    private String secret;

    public Long getId() {
        return id;
    }

    public String getOperation() {
        return operation;
    }

    public void setOperation(String operation) {
        this.operation = operation;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public String getCustomer() {
        return customer;
    }

    public void setCustomer(String customer) {
        this.customer = customer;
    }

    public String getReference() {
        return reference;
    }

    public void setReference(String reference) {
        this.reference = reference;
    }

    public TransactionStatus getStatus() {
        return status;
    }

    public void setStatus(TransactionStatus status) {
        this.status = status;
    }

    public String getSecret() {
        return secret;
    }

    public void setSecret(String secret) {
        this.secret = secret;
    }
}
