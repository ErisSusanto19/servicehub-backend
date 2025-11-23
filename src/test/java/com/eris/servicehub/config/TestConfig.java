package com.eris.servicehub.config;

import com.cloudinary.Cloudinary;
import com.midtrans.service.MidtransSnapApi;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Profile;

import static org.mockito.Mockito.mock;

@TestConfiguration
@Profile("test")
public class TestConfig {

    @Bean
    public Cloudinary cloudinary() {
        return mock(Cloudinary.class);
    }

    @Bean
    public MidtransSnapApi midtransSnapApi() {
        return mock(MidtransSnapApi.class);
    }
}