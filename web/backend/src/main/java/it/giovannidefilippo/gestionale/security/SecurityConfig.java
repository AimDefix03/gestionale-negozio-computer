package it.giovannidefilippo.gestionale.security;

import it.giovannidefilippo.gestionale.common.ApiErrorCode;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableMethodSecurity
class SecurityConfig {
    private final SessionAuthenticationFilter sessionAuthenticationFilter;
    private final SecurityErrorWriter errorWriter;

    SecurityConfig(SessionAuthenticationFilter sessionAuthenticationFilter, SecurityErrorWriter errorWriter) {
        this.sessionAuthenticationFilter = sessionAuthenticationFilter;
        this.errorWriter = errorWriter;
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }

    @Bean
    UserDetailsService userDetailsService() {
        return username -> {
            throw new UsernameNotFoundException("Autenticazione gestita tramite token di sessione.");
        };
    }

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
                .csrf(AbstractHttpConfigurer::disable)
                .headers(headers -> headers.frameOptions(frameOptions -> frameOptions.sameOrigin()))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint((request, response, exception) -> errorWriter.write(request, response, HttpStatus.UNAUTHORIZED, ApiErrorCode.AUTH_UNAUTHORIZED, "Sessione mancante o non valida."))
                        .accessDeniedHandler((request, response, exception) -> errorWriter.write(request, response, HttpStatus.FORBIDDEN, ApiErrorCode.AUTH_FORBIDDEN, "Permessi insufficienti per questa operazione."))
                )
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.POST, "/api/accounts/login", "/api/accounts/register", "/api/accounts/password-strength").permitAll()
                        .requestMatchers("/h2-console/**").permitAll()
                        .requestMatchers("/actuator/health/liveness", "/actuator/health/readiness").permitAll()
                        .requestMatchers("/actuator/prometheus").permitAll()
                        .requestMatchers("/actuator/**").denyAll()
                        .requestMatchers(HttpMethod.POST, "/api/accounts/logout").authenticated()
                        .requestMatchers(HttpMethod.POST, "/api/accounts/session/renew").authenticated()
                        .requestMatchers(HttpMethod.GET, "/api/products/**").hasAuthority("VIEW_CATALOG")
                        .requestMatchers(HttpMethod.POST, "/api/products/**").hasAuthority("MANAGE_PRODUCTS")
                        .requestMatchers(HttpMethod.PUT, "/api/products/**").hasAuthority("MANAGE_PRODUCTS")
                        .requestMatchers(HttpMethod.DELETE, "/api/products/**").hasAuthority("MANAGE_PRODUCTS")
                        .requestMatchers(HttpMethod.GET, "/api/partners/**").hasAuthority("VIEW_PARTNERS")
                        .requestMatchers(HttpMethod.POST, "/api/partners/**").hasAuthority("MANAGE_PARTNERS")
                        .requestMatchers(HttpMethod.PUT, "/api/partners/**").hasAuthority("MANAGE_PARTNERS")
                        .requestMatchers(HttpMethod.DELETE, "/api/partners/**").hasAuthority("MANAGE_PARTNERS")
                        .requestMatchers("/api/inventory/**").hasAuthority("MANAGE_INVENTORY")
                        .requestMatchers(HttpMethod.GET, "/api/orders/**").hasAuthority("VIEW_ORDERS")
                        .requestMatchers(HttpMethod.POST, "/api/orders/*/confirm").hasAuthority("CONFIRM_ORDERS")
                        .requestMatchers(HttpMethod.POST, "/api/orders/*/fulfill").hasAuthority("FULFILL_ORDERS")
                        .requestMatchers(HttpMethod.POST, "/api/orders/*/cancel").hasAuthority("CANCEL_ORDERS")
                        .requestMatchers(HttpMethod.POST, "/api/orders").hasAuthority("CREATE_ORDERS")
                        .requestMatchers("/api/documents/**").hasAuthority("MANAGE_DOCUMENTS")
                        .requestMatchers(HttpMethod.GET, "/api/reports/**").hasAuthority("VIEW_REPORTS")
                        .requestMatchers("/api/accounts/**").hasAuthority("MANAGE_ACCOUNTS")
                        .requestMatchers("/api/audit/**").hasAuthority("VIEW_AUDIT")
                        .anyRequest().authenticated()
                )
                .addFilterBefore(sessionAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .build();
    }
}
