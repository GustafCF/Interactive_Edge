package com.br.elohostel.config;

import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import com.br.elohostel.rsa.RsaKeyGenerator;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private final RSAPublicKey publicKey;
    private final RSAPrivateKey privateKey;

    public SecurityConfig(RsaKeyGenerator rsaKeyGenerator) {
        this.publicKey = rsaKeyGenerator.getPublicKey();
        this.privateKey = rsaKeyGenerator.getPrivateKey();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .cors(Customizer.withDefaults())
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers("/h2-console/**").permitAll() 
                        .requestMatchers("/","/login","/calendario","/room-calendar", "/room-calendar/**", "/guest","/layout","/reserve","/room", "/static/**", "/js/**", "/css/**", "/airbnb-setup", "/calendar-airbnb-setup", "/financas").permitAll()
                        .requestMatchers(HttpMethod.POST, "/auth/login").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/airbnb/connections", "/api/booking/health", "/api/booking/connections", "/connections/{propertyId}").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/airbnb/connect", "/api/airbnb/sync-now", "/api/airbnb/setup-calendar-bidirectional", "/api/booking/setup-bidirectional", "/api/booking/sync-now/{propertyId}", "/api/booking/sync-now/all", "/api/booking/setup-calendar-bidirectional", "/api/booking/test-connection").permitAll()
                        .requestMatchers(HttpMethod.DELETE, "/api/airbnb/connections/{id}", "/api/booking/connections/{propertyId}", "/api/financial/delete/{id}").permitAll()
                        .requestMatchers(HttpMethod.PUT, "/api/airbnb/connections/{id}", "/api/booking/connections/{propertyId}/deactivate", "/api/booking/connections/{propertyId}/activate", "/reserve/up/{id}").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/airbnb/setup-bidirectional").permitAll()
                        .requestMatchers("/api/airbnb/sync/**", "/api/airbnb/connection/**").permitAll()
                        .requestMatchers("/api/ical/**", "/api/booking/**").permitAll()
                        .requestMatchers("/api/airbnb/**").permitAll()
                        .requestMatchers("/api/ical/**").permitAll()
                        .requestMatchers("/api/calendar/**").permitAll()
                        .anyRequest().authenticated())
                .csrf(csrf -> csrf.disable())
                .headers(headers -> headers
                        .frameOptions(frameOptions -> frameOptions.disable()))
                .oauth2ResourceServer(oauth2 -> oauth2.jwt(Customizer.withDefaults()))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS));
        return http.build();
    }


    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowCredentials(true);
        configuration.addAllowedOriginPattern("*");
        configuration.addAllowedHeader("*");
        configuration.addAllowedMethod("*");

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    @Bean
    public JwtDecoder jwtDecoder() {
        return NimbusJwtDecoder.withPublicKey(publicKey).build();
    }

    @Bean
    public JwtEncoder jwtEncoder() {
        JWK jwk = new RSAKey.Builder(publicKey).privateKey(privateKey).build();
        var jwks = new ImmutableJWKSet<>(new JWKSet(jwk));
        return new NimbusJwtEncoder(jwks);
    }

    @Bean
    public BCryptPasswordEncoder bCryptPasswordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
