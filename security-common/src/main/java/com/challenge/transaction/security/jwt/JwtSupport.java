package com.challenge.transaction.security.jwt;

import java.util.Base64;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

public final class JwtSupport {
    private static final int MIN_HMAC_KEY_BYTES = 32;

    private JwtSupport() {
        // Clase utilitaria: no debe instanciarse.
    }

    public static SecretKey hmacKeyFromBase64(String encodedSecret) {
        final byte[] secretBytes;
        try {
            secretBytes = Base64.getDecoder().decode(encodedSecret);
        } catch (IllegalArgumentException ex) {
            throw new IllegalStateException(
                    "SECURITY_JWT_SECRET_BASE64 no contiene Base64 valido", ex);
        }
        if (secretBytes.length < MIN_HMAC_KEY_BYTES) {
            throw new IllegalStateException(
                    "SECURITY_JWT_SECRET_BASE64 debe representar al menos 32 bytes aleatorios");
        }
        return new SecretKeySpec(secretBytes, "HmacSHA256");
    }

    public static JwtEncoder encoder(SecretKey secretKey) {
        return NimbusJwtEncoder.withSecretKey(secretKey).algorithm(MacAlgorithm.HS256).build();
    }

    public static JwtDecoder decoder(SecretKey secretKey, String issuer, String audience) {
        NimbusJwtDecoder decoder =
                NimbusJwtDecoder.withSecretKey(secretKey).macAlgorithm(MacAlgorithm.HS256).build();

        OAuth2TokenValidator<Jwt> standardValidator = JwtValidators.createDefaultWithIssuer(issuer);
        OAuth2TokenValidator<Jwt> audienceValidator =
                token ->
                        token.getAudience().contains(audience)
                                ? OAuth2TokenValidatorResult.success()
                                : OAuth2TokenValidatorResult.failure(
                                        new OAuth2Error(
                                                "invalid_token", "audience invalida", null));

        // Un único punto de validación evita divergencias entre el gateway y el servicio interno.
        decoder.setJwtValidator(
                new DelegatingOAuth2TokenValidator<>(standardValidator, audienceValidator));
        return decoder;
    }
}
