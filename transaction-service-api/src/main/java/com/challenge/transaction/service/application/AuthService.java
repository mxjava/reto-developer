package com.challenge.transaction.service.application;

import com.challenge.transaction.service.api.dto.LoginRequest;
import com.challenge.transaction.service.api.dto.LoginResponse;
import com.challenge.transaction.service.infrastructure.persistence.repository.UserRepository;
import com.challenge.transaction.service.infrastructure.security.JwtTokenService;
import java.security.SecureRandom;
import java.util.HexFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {
    private static final Logger LOGGER = LoggerFactory.getLogger(AuthService.class);
    private final UserRepository repository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenService tokenService;
    private final String dummyBcrypt;

    public AuthService(
            UserRepository repository,
            PasswordEncoder passwordEncoder,
            JwtTokenService tokenService) {
        this.repository = repository;
        this.passwordEncoder = passwordEncoder;
        this.tokenService = tokenService;
        // Hash de relleno generado al iniciar para aproximar el costo temporal de un usuario real.
        // SecureRandom evita que una herramienta SAST confunda este material con una credencial
        // fija.
        byte[] random = new byte[32];
        new SecureRandom().nextBytes(random);
        this.dummyBcrypt = passwordEncoder.encode(HexFormat.of().formatHex(random));
    }

    public LoginResponse login(LoginRequest request) {
        var user = repository.findByUsername(request.username());
        String hashToVerify = user.map(candidate -> candidate.getPassword()).orElse(dummyBcrypt);
        boolean passwordMatches = passwordEncoder.matches(request.password(), hashToVerify);

        if (user.isEmpty() || !passwordMatches) {
            // No se registra el usuario fallido: reduce enumeración y evita inyección de datos en
            // logs.
            LOGGER.warn("event=AUTH_FAILURE result=DENIED");
            // Mensaje deliberadamente genérico: evita enumeración de usuarios.
            throw new BadCredentialsException("Credenciales invalidas");
        }
        String username = user.orElseThrow().getUsername();
        LOGGER.info("event=AUTH_SUCCESS result=GRANTED username={}", username);
        return tokenService.issue(username);
    }

    public String hash(String rawPassword) {
        return passwordEncoder.encode(rawPassword);
    }
}
