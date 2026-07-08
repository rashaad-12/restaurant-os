package com.restaurantos.elasticservice.config;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import co.elastic.clients.transport.ElasticsearchTransport;
import co.elastic.clients.transport.rest5_client.Rest5ClientTransport;
import co.elastic.clients.transport.rest5_client.low_level.Rest5Client;
import co.elastic.clients.transport.rest5_client.low_level.Rest5ClientBuilder;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.apache.hc.client5.http.auth.AuthScope;
import org.apache.hc.client5.http.auth.UsernamePasswordCredentials;
import org.apache.hc.client5.http.impl.auth.BasicCredentialsProvider;
import org.apache.hc.core5.http.HttpHost;
import org.apache.hc.core5.util.Timeout;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ElasticSearchConfig {

    @Value("${elasticsearch.scheme}")
    private String scheme;

    @Value("${elasticsearch.host}")
    private String host;

    @Value("${elasticsearch.port}")
    private int port;

    @Value("${elasticsearch.username}")
    private String username;

    @Value("${elasticsearch.password}")
    private String password;

    @Value("${elasticsearch.connectionTimeout}")
    private int connectionTimeout;

    @Value("${elasticsearch.socketTimeout}")
    private int socketTimeout;

    @Bean
    public ElasticsearchClient elasticsearchClient() {
        HttpHost server = new HttpHost(scheme, host, port);

        BasicCredentialsProvider credentialsProvider = new BasicCredentialsProvider();
        credentialsProvider.setCredentials(
                new AuthScope(server),
                new UsernamePasswordCredentials(username, password.toCharArray()));

        Rest5ClientBuilder builder = Rest5Client
                .builder(server)
                .setConnectionConfigCallback(connectConfig -> connectConfig
                        .setConnectTimeout(Timeout.ofSeconds(connectionTimeout))
                        .setSocketTimeout(Timeout.ofSeconds(socketTimeout)))
                .setHttpClientConfigCallback(httpClientBuilder ->
                        httpClientBuilder.setDefaultCredentialsProvider(credentialsProvider));

        Rest5Client lowLevelClient = builder.build();
        ElasticsearchTransport transport = new Rest5ClientTransport(lowLevelClient, jsonpMapper());
        return new ElasticsearchClient(transport);
    }

    private JacksonJsonpMapper jsonpMapper() {
        ObjectMapper objectMapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        return new JacksonJsonpMapper(objectMapper);
    }
}
