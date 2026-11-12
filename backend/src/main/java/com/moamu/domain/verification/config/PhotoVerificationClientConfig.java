package com.moamu.domain.verification.config;

import java.net.http.HttpClient;
import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration
public class PhotoVerificationClientConfig {

    @Bean
    public RestClient photoVerificationRestClient(
            RestClient.Builder builder,
            @Value("${app.ai.connect-timeout}") Duration connectTimeout,
            @Value("${app.ai.read-timeout}") Duration readTimeout) {
        HttpClient httpClient = HttpClient.newBuilder()
                // AI 서버의 HTTP/2 cleartext(h2c) 업그레이드 처리 오류 방지
                .version(HttpClient.Version.HTTP_1_1)
                .connectTimeout(connectTimeout)
                .build();
        JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(httpClient);
        factory.setReadTimeout(readTimeout);
        return builder.requestFactory(factory).build();
    }
}
