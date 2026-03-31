package com.br.elohostel.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import com.mercadopago.MercadoPagoConfig;

import jakarta.annotation.PostConstruct;

@Configuration
public class MercadoPagoSdkConfig {

    private static final Logger log = LoggerFactory.getLogger(MercadoPagoSdkConfig.class);

    @Value("${mercado-pago.access-token}")
    private String accessToken;

    @PostConstruct
    public void init() {
        MercadoPagoConfig.setAccessToken(accessToken);
        log.info("Test 1: {}", accessToken);
        log.info("Test 2: "+ accessToken.toString());
    }
}
