package com.restaurantos.elasticsyncservice.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.time.Duration;

@Configuration
public class RestClientConfig {

    @Value("${integration.identity-service.base-url}")
    private String identityServiceBaseUrl;

    @Value("${integration.http.connect-timeout-ms:2000}")
    private int connectTimeoutMs;

    @Value("${integration.http.read-timeout-ms:5000}")
    private int readTimeoutMs;

    /** Fixed base-url client for the single token issuer. */
    @Bean
    public RestClient identityServiceRestClient() {
        return build(identityServiceBaseUrl);
    }

    /** Base-url-less client for enrichment: each source's absolute URL comes from config at call time. */
    @Bean
    public RestClient enrichmentRestClient() {
        return build(null);
    }

    private RestClient build(String baseUrl) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofMillis(connectTimeoutMs));
        factory.setReadTimeout(Duration.ofMillis(readTimeoutMs));

        RestClient.Builder builder = RestClient.builder().requestFactory(factory);
        if (baseUrl != null) {
            builder.baseUrl(baseUrl);
        }
        return builder.build();
    }
}
