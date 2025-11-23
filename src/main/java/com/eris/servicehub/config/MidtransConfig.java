package com.eris.servicehub.config;

import com.midtrans.Config;
import com.midtrans.service.MidtransSnapApi;
import com.midtrans.service.impl.MidtransSnapApiImpl;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MidtransConfig {

    @Value("${midtrans.server.key}")
    private String serverKey;

    @Value("${midtrans.client.key}")
    private String clientKey;

    @Value("${midtrans.is.production}")
    private boolean isProduction;

    @Bean
    public Config midtransConfiguration() {
        return Config.builder()
                .setServerKey(serverKey)
                .setClientKey(clientKey)
                .setIsProduction(isProduction)
                .build();
    }

    @Bean
    public MidtransSnapApi midtransSnapApi(Config config) {
        return new MidtransSnapApiImpl(config);
    }
}