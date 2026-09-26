package com.challenge.transaction.service.infrastructure.bootstrap;

import com.challenge.transaction.service.application.AuthService;
import com.challenge.transaction.service.infrastructure.persistence.entity.UserEntity;
import com.challenge.transaction.service.infrastructure.persistence.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DataInitializer {

    @Bean
    CommandLineRunner initializeDefaultUser(
            UserRepository users,
            AuthService authService,
            @Value("${app.bootstrap.username:}") String username,
            @Value("${app.bootstrap.password:}") String password) {
        return args -> {
            if (username.isBlank() || password.isBlank()) {
                return;
            }
            if (password.length() < 12) {
                throw new IllegalStateException(
                        "APP_BOOTSTRAP_PASSWORD debe contener al menos 12 caracteres");
            }
            if (!users.existsByUsername(username)) {
                // La contraseña jamás se persiste en claro; solo el hash adaptativo BCrypt.
                users.save(new UserEntity(username, authService.hash(password)));
            }
        };
    }
}
