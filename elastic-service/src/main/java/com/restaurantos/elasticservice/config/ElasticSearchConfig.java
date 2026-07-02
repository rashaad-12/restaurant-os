package com.restaurantos.elasticservice.config;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.json.jackson.Jackson3JsonpMapper;
import co.elastic.clients.transport.ElasticsearchTransport;
import co.elastic.clients.transport.rest5_client.Rest5ClientTransport;
import co.elastic.clients.transport.rest5_client.low_level.Rest5Client;
import co.elastic.clients.transport.rest5_client.low_level.Rest5ClientBuilder;
import org.apache.hc.client5.http.auth.AuthScope;
import org.apache.hc.client5.http.auth.UsernamePasswordCredentials;
import org.apache.hc.client5.http.impl.auth.BasicCredentialsProvider;
import org.apache.hc.core5.http.HttpHost;
import org.apache.hc.core5.util.Timeout;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@ComponentScan(basePackages = {"com.restaurantos.restaurantservice"})
public class ElasticSearchConfig {

    @Value("${elasticsearch.scheme}")
    private String scheme;

    @Value("${elasticsearch.host}")
    private String elasticSearchHost;

    @Value("${elasticsearch.port}")
    private int elasticSearchPort;

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

        HttpHost elasticsearchServer = new HttpHost("https", elasticSearchHost, elasticSearchPort);

        BasicCredentialsProvider credentialsProvider = new BasicCredentialsProvider();

        credentialsProvider.setCredentials(
                new AuthScope(elasticsearchServer),
                new UsernamePasswordCredentials(username, password.toCharArray()));

        Rest5ClientBuilder builder = Rest5Client
                .builder(new HttpHost(scheme, elasticSearchHost, elasticSearchPort))
                .setConnectionConfigCallback(connectConfig -> connectConfig
                        .setConnectTimeout(Timeout.ofSeconds(connectionTimeout))
                        .setSocketTimeout(Timeout.ofSeconds(socketTimeout)))
                .setHttpClientConfigCallback(httpClientBuilder ->
                        httpClientBuilder.setDefaultCredentialsProvider(credentialsProvider)
                );

        Rest5Client lowLevelClient = builder.build();
        ElasticsearchTransport transport = new Rest5ClientTransport(lowLevelClient, new Jackson3JsonpMapper());
        return new ElasticsearchClient(transport);
    }
}
