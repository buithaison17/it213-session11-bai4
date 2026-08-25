package com.example.bai4.config;

import com.langfuse.client.LangfuseClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class LangfuseConfig {
    @Value("${langfuse.host:http://localhost:3000}")
    private String host;
    @Value("${langfuse.public-key}")
    private String publicKey;
    @Value("${langfuse.secret-key}")
    private String secretKey;

    @Bean
    public LangfuseClient langfuseClient() {
        return LangfuseClient.builder()
                .url(host)
                .credentials(publicKey, secretKey)
                .build();
    }
}