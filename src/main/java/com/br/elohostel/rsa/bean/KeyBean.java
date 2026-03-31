package com.br.elohostel.rsa.bean;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.br.elohostel.rsa.RsaKeyGenerator;

@Configuration
public class KeyBean {

    @Bean
    public RsaKeyGenerator rsaKeyGenerator() {
        return new RsaKeyGenerator();
    }
}