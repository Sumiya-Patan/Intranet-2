package com.intranet;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Configuration
public class LocalJwtDecoderConfig {

    @Bean
    public JwtDecoder jwtDecoder() {
        return token -> {
            Map<String, Object> claims = new HashMap<>();
            claims.put("user_id", "123");
            claims.put("name", "Swagger Test User");
            claims.put("email", "swagger@example.com");
            claims.put("roles", List.of("General User", "Admin")); // matches your jwtAuthenticationConverter

            Instant now = Instant.now();
            return Jwt.withTokenValue(token)
                    .header("alg", "none")
                    .claims(c -> c.putAll(claims))
                    .issuedAt(now)
                    .expiresAt(now.plusSeconds(3600))
                    .build();
        };
    }
}
