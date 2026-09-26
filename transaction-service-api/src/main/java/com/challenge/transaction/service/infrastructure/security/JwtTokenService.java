package com.challenge.transaction.service.infrastructure.security;

import com.challenge.transaction.service.api.dto.LoginResponse;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

@Service
public class JwtTokenService {

    private static final String SCOPE = "transactions.read transactions.write transactions.cancel";

    private final JwtEncoder encoder;
    private final Clock clock;
    private final String issuer;
    private final String audience;
    private final Duration lifetime;

    public JwtTokenService(
            JwtEncoder encoder,
            @Value("${security.jwt.issuer}") String issuer,
            @Value("${security.jwt.audience}") String audience,
            @Value("${security.jwt.ttl-minutes:15}") long ttlMinutes) {

        this.encoder = encoder;
        this.clock = Clock.systemUTC();
        this.issuer = issuer;
        this.audience = audience;
        this.lifetime = Duration.ofMinutes(ttlMinutes);
    }

    public LoginResponse issue(String username) {
        Instant issuedAt = clock.instant();
        Instant expiresAt = issuedAt.plus(lifetime);

        JwtClaimsSet claims =
                JwtClaimsSet.builder()
                        .issuer(issuer)
                        .subject(username)
                        .audience(List.of(audience))
                        .issuedAt(issuedAt)
                        .expiresAt(expiresAt)
                        .claim("scope", SCOPE)
                        .build();

        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).type("JWT").build();

        String token = encoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();

        return new LoginResponse(token, "Bearer", lifetime.toSeconds(), username);
    }
}
