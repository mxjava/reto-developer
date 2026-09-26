package com.challenge.transaction.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication
@EnableFeignClients
public class TransactionGatewayApplication {
    public static void main(String[] args) {
        SpringApplication.run(TransactionGatewayApplication.class, args);
    }
}
