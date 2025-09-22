package com.intranet.security;


import static org.springframework.security.config.Customizer.withDefaults;

import java.util.stream.Collectors;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(
  prePostEnabled = true,
  securedEnabled = true,
  jsr250Enabled = true
)
public class SecurityConfig {

  @Bean
  public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
    http
      .authorizeHttpRequests(auth -> auth
        .requestMatchers("/public/**",
        "/swagger-ui/**",
        "/v3/api-docs/**",
        "/actuator/**"
        ).permitAll()
        .anyRequest().authenticated()
      )
      .oauth2ResourceServer(oauth2 -> oauth2
        .jwt(jwt -> jwt
          .jwtAuthenticationConverter(jwtAuthenticationConverter())
        )
      )
      .csrf(csrf -> csrf.disable()) // Disable CSRF for simplicity, enable if needed
      .cors(withDefaults()); // Enable CORS


    return http.build();
  }

  private JwtAuthenticationConverter jwtAuthenticationConverter() {
    JwtAuthenticationConverter authenticationConverter = new JwtAuthenticationConverter();

    authenticationConverter.setJwtGrantedAuthoritiesConverter(jwt -> {
        // Extract and normalize permissions
        return jwt.getClaimAsStringList("permissions").stream()
                .map(permission -> permission.trim().toUpperCase())  // normalize
                .map(SimpleGrantedAuthority::new)                    // convert to GrantedAuthority
                .collect(Collectors.toList());
    });

    return authenticationConverter;
  }
}
