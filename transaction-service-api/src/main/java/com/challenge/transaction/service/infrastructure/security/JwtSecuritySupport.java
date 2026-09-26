package com.challenge.transaction.service.infrastructure.security;

import com.challenge.transaction.security.jwt.JwtSupport;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;

@Configuration
public class JwtSecuritySupport {

    @Bean
    SecretKey jwtSecretKey(@Value("${security.jwt.secret-base64}") String encodedSecret) {
        return JwtSupport.hmacKeyFromBase64(encodedSecret);
    }

    @Bean
    JwtEncoder jwtEncoder(SecretKey secretKey) {
        return JwtSupport.encoder(secretKey);
    }

    @Bean
    JwtDecoder jwtDecoder(
            SecretKey secretKey,
            @Value("${security.jwt.issuer}") String issuer,
            @Value("${security.jwt.audience}") String audience) {
        return JwtSupport.decoder(secretKey, issuer, audience);
    }
}
