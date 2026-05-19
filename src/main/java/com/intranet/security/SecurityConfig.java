package com.intranet.security;


import static org.springframework.security.config.Customizer.withDefaults;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(
  prePostEnabled = true,
  securedEnabled = true,
  jsr250Enabled = true
)
public class SecurityConfig {

  /**
   * Comma-separated list of origins permitted by CORS.
   * Configure per environment via `app.cors.allowed-origins` in application.properties /
   * application-{env}.properties. Defaults to the Vite dev server.
   */
  @Value("${app.cors.allowed-origins:http://localhost:5173}")
  private String allowedOrigins;

  @Bean
  public CorsConfigurationSource corsConfigurationSource() {
    CorsConfiguration cfg = new CorsConfiguration();
    cfg.setAllowedOrigins(List.of(allowedOrigins.split("\\s*,\\s*")));
    cfg.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
    cfg.setAllowedHeaders(List.of("Authorization", "Content-Type", "Accept", "X-Requested-With"));
    cfg.setExposedHeaders(List.of("Authorization"));
    cfg.setAllowCredentials(true);
    cfg.setMaxAge(3600L);

    UrlBasedCorsConfigurationSource src = new UrlBasedCorsConfigurationSource();
    src.registerCorsConfiguration("/**", cfg);
    return src;
  }

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



// package com.intranet.security;

// import static org.springframework.security.config.Customizer.withDefaults;

// import java.util.stream.Collectors;

// import org.springframework.context.annotation.Bean;
// import org.springframework.context.annotation.Configuration;
// import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
// import org.springframework.security.config.annotation.web.builders.HttpSecurity;
// import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
// import org.springframework.security.core.authority.SimpleGrantedAuthority;
// import org.springframework.security.oauth2.jwt.Jwt;
// import org.springframework.security.oauth2.jwt.JwtDecoder;
// import org.springframework.security.oauth2.jwt.JwtException;
// import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
// import org.springframework.security.web.SecurityFilterChain;

// import java.time.Instant;
// import java.util.Map;

// @Configuration
// @EnableWebSecurity
// @EnableMethodSecurity(
//   prePostEnabled = true,
//   securedEnabled = true,
//   jsr250Enabled = true
// )
// public class SecurityConfig {

//   @Bean
//   public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
//     http
//       .authorizeHttpRequests(auth -> auth
//         .requestMatchers(
//             "/public/**",
//             "/swagger-ui/**",
//             "/v3/api-docs/**",
//             "/actuator/**"
//         ).permitAll()
//         .anyRequest().authenticated()
//       )
//       .oauth2ResourceServer(oauth2 -> oauth2
//         .jwt(jwt -> jwt
//           .decoder(fakeJwtDecoder())  // 👈 our custom decoder
//           .jwtAuthenticationConverter(jwtAuthenticationConverter())
//         )
//       )
//       .csrf(csrf -> csrf.disable())
//       .cors(withDefaults());

//     return http.build();
//   }

//   /**
//    * ✅ Custom JWT Decoder that skips validation
//    * but still parses the token and exposes its claims.
//    */
//   @Bean
//   public JwtDecoder fakeJwtDecoder() {
//     return token -> {
//       try {
//         // Very simple JWT decoding (Base64 decode without signature verification)
//         String[] parts = token.split("\\.");
//         if (parts.length < 2) {
//           throw new JwtException("Invalid token format");
//         }

//         String payloadJson = new String(java.util.Base64.getUrlDecoder().decode(parts[1]));
//         @SuppressWarnings("unchecked")
//         Map<String, Object> claims = new com.fasterxml.jackson.databind.ObjectMapper()
//                 .readValue(payloadJson, Map.class);

//         return new Jwt(
//             token,
//             Instant.now(),
//             Instant.now().plusSeconds(3600),
//             Map.of("alg", "none"), // no signature headers
//             claims
//         );
//       } catch (Exception e) {
//         throw new JwtException("Failed to decode token", e);
//       }
//     };
//   }

//   /**
//    * ✅ Extracts authorities (permissions) from the token
//    */
//   private JwtAuthenticationConverter jwtAuthenticationConverter() {
//     JwtAuthenticationConverter authenticationConverter = new JwtAuthenticationConverter();

//     authenticationConverter.setJwtGrantedAuthoritiesConverter(jwt -> {
//       var permissions = jwt.getClaimAsStringList("permissions");
//       if (permissions == null) return java.util.Collections.emptyList();

//       return permissions.stream()
//               .map(String::trim)
//               .map(String::toUpperCase)
//               .map(SimpleGrantedAuthority::new)
//               .collect(Collectors.toList());
//     });

//     return authenticationConverter;
//   }
// }
