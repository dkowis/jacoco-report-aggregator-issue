package com.example.servlettestapp

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.client.RestClient

@Configuration
class RestClientConfiguration {


    @Bean
    fun buildRestClient(
        restClientBuilder: RestClient.Builder, configuration: UpstreamConfiguration
    ): RestClient {
        return restClientBuilder.baseUrl(configuration.url).build();
    }
}