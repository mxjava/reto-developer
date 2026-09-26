package com.challenge.transaction.service.application;

import com.challenge.transaction.service.api.dto.CancelTransactionRequest;
import com.challenge.transaction.service.api.dto.CreateTransactionRequest;
import com.challenge.transaction.service.api.dto.PageResponse;
import com.challenge.transaction.service.api.dto.TransactionDetailResponse;
import com.challenge.transaction.service.api.dto.TransactionResponse;
import com.challenge.transaction.service.domain.TransactionStatus;
import com.challenge.transaction.service.domain.exception.InvalidOperationException;
import com.challenge.transaction.service.domain.exception.NotFoundException;
import com.challenge.transaction.service.infrastructure.persistence.entity.TransactionEntity;
import com.challenge.transaction.service.infrastructure.persistence.repository.TransactionRepository;
import java.security.SecureRandom;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TransactionApplicationService {
    private static final Logger LOGGER =
            LoggerFactory.getLogger(TransactionApplicationService.class);
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final int REFERENCE_MIN = 100_000;
    private static final int REFERENCE_RANGE = 900_000;
    private final TransactionRepository repository;

    public TransactionApplicationService(TransactionRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public TransactionResponse create(CreateTransactionRequest request) {
        TransactionEntity entity = new TransactionEntity();
        entity.setOperation(request.operacion().trim());
        entity.setAmount(request.importe());
        entity.setCustomer(request.cliente().trim());
        // El PDF exige persistir este valor descifrado. No debe utilizarse para PAN, CVV, PIN ni
        // credenciales reales.
        entity.setSecret(request.secreto());
        entity.setStatus(TransactionStatus.APROBADA);
        entity.setReference(nextReference());
        TransactionEntity saved = repository.save(entity);
        // Auditoría mínima sin secreto, cliente ni importe. La plataforma de logs debe añadir
        // timestamp/host.
        LOGGER.info(
                "event=TRANSACTION_CREATED id={} reference={} status={}",
                saved.getId(),
                saved.getReference(),
                saved.getStatus().label());
        return new TransactionResponse(
                saved.getId(),
                saved.getStatus().label(),
                saved.getReference(),
                saved.getOperation());
    }

    @Transactional
    public TransactionResponse cancel(CancelTransactionRequest request) {
        int affected =
                repository.updateStatus(
                        request.id(),
                        request.referencia(),
                        TransactionStatus.CANCELADA,
                        TransactionStatus.APROBADA);
        if (affected == 0) {
            if (!repository.existsById(request.id())) {
                throw new NotFoundException("Transaccion no encontrada");
            }
            throw new InvalidOperationException(
                    "La transaccion no coincide con la referencia o ya fue cancelada");
        }
        TransactionEntity entity =
                repository
                        .findById(request.id())
                        .orElseThrow(() -> new NotFoundException("Transaccion no encontrada"));
        LOGGER.info(
                "event=TRANSACTION_CANCELLED id={} reference={} status={}",
                entity.getId(),
                entity.getReference(),
                entity.getStatus().label());
        return new TransactionResponse(
                entity.getId(),
                entity.getStatus().label(),
                entity.getReference(),
                entity.getOperation());
    }

    @Transactional(readOnly = true)
    public PageResponse<TransactionDetailResponse> findAll(int page, int size, String sort) {
        if (page < 0) {
            throw new InvalidOperationException("page debe ser >= 0");
        }
        if (size < 1 || size > 100) {
            throw new InvalidOperationException("size debe estar entre 1 y 100");
        }
        Page<TransactionEntity> result =
                repository.findAll(PageRequest.of(page, size, parseSort(sort)));
        var content = result.stream().map(this::toDetail).toList();
        return new PageResponse<>(
                content,
                result.getNumber(),
                result.getSize(),
                result.getTotalElements(),
                result.getTotalPages(),
                result.isFirst(),
                result.isLast());
    }

    private String nextReference() {
        // SecureRandom evita referencias predecibles; el índice UNIQUE actúa como última barrera de
        // integridad.
        for (int attempt = 0; attempt < 50; attempt++) {
            String reference = Integer.toString(REFERENCE_MIN + RANDOM.nextInt(REFERENCE_RANGE));
            if (!repository.existsByReference(reference)) {
                return reference;
            }
        }
        throw new IllegalStateException("No fue posible generar una referencia unica");
    }

    private Sort parseSort(String sort) {
        String value = (sort == null || sort.isBlank()) ? "id,desc" : sort;
        String[] parts = value.split(",", 2);
        String property =
                switch (parts[0]) {
                    case "id" -> "id";
                    case "operacion" -> "operation";
                    case "importe" -> "amount";
                    case "cliente" -> "customer";
                    case "referencia" -> "reference";
                    case "estatus" -> "status";
                    default ->
                            throw new InvalidOperationException(
                                    "Campo de ordenamiento no permitido: " + parts[0]);
                };
        Sort.Direction direction =
                parts.length > 1 && "asc".equalsIgnoreCase(parts[1])
                        ? Sort.Direction.ASC
                        : Sort.Direction.DESC;
        return Sort.by(direction, property);
    }

    private TransactionDetailResponse toDetail(TransactionEntity entity) {
        // El secreto se excluye deliberadamente de consultas/listados para reducir exposición de
        // datos.
        return new TransactionDetailResponse(
                entity.getId(),
                entity.getOperation(),
                entity.getAmount(),
                entity.getCustomer(),
                entity.getReference(),
                entity.getStatus().label());
    }
}
