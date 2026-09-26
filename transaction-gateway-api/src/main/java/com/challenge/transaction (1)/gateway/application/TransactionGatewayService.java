package com.challenge.transaction.gateway.application;

import com.challenge.transaction.gateway.api.dto.CancelTransactionRequest;
import com.challenge.transaction.gateway.api.dto.LoginRequest;
import com.challenge.transaction.gateway.api.dto.LoginResponse;
import com.challenge.transaction.gateway.api.dto.PageResponse;
import com.challenge.transaction.gateway.api.dto.ServiceTransactionRequest;
import com.challenge.transaction.gateway.api.dto.TransactionDetailResponse;
import com.challenge.transaction.gateway.api.dto.TransactionRequest;
import com.challenge.transaction.gateway.api.dto.TransactionResponse;
import com.challenge.transaction.gateway.infrastructure.client.TransactionServiceClient;
import com.challenge.transaction.gateway.infrastructure.crypto.AesCryptoService;
import org.springframework.stereotype.Service;

@Service
public class TransactionGatewayService {
    private final TransactionServiceClient client;
    private final AesCryptoService crypto;

    public TransactionGatewayService(TransactionServiceClient client, AesCryptoService crypto) {
        this.client = client;
        this.crypto = crypto;
    }

    public TransactionResponse create(TransactionRequest request) {
        String decryptedSecret = crypto.decrypt(request.secreto());
        return client.create(
                new ServiceTransactionRequest(
                        request.operacion(),
                        request.importe(),
                        request.cliente(),
                        decryptedSecret));
    }

    public TransactionResponse cancel(CancelTransactionRequest request) {
        return client.cancel(request);
    }

    public PageResponse<TransactionDetailResponse> findAll(int page, int size, String sort) {
        return client.findAll(page, size, sort);
    }

    public LoginResponse login(LoginRequest request) {
        return client.login(request);
    }
}
