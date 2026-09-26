package com.challenge.transaction.gateway.infrastructure.client;

import feign.RequestInterceptor;
import feign.Retryer;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Configuration
public class FeignSecurityConfig {

    @Bean
    RequestInterceptor bearerTokenRelayInterceptor() {
        return template -> {
            var attributes = RequestContextHolder.getRequestAttributes();
            if (attributes instanceof ServletRequestAttributes servletAttributes) {
                HttpServletRequest request = servletAttributes.getRequest();
                String authorization = request.getHeader("Authorization");
                if (authorization != null && authorization.startsWith("Bearer ")) {
                    // API 2 vuelve a validar firma, expiración, issuer, audience y scopes.
                    template.header("Authorization", authorization);
                }
            }
        };
    }

    @Bean
    Retryer retryer() {
        // Evita reintentar automáticamente POST/PATCH y crear efectos duplicados.
        return Retryer.NEVER_RETRY;
    }
}
