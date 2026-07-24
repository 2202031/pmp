package com.example.pmp_back.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

// Solo usamos spring-security-crypto (el encoder), NO spring-boot-starter-security,
// así que no se activa ningún filtro de seguridad automático: los endpoints
// siguen siendo públicos como antes, solo que las contraseñas nuevas ya no
// se guardan en texto plano.
@Configuration
public class PasswordConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
