package com.example.reactivetestapp

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.reactive.function.client.WebClient

@Configuration
class UpstreamWebClientBeans {

    @Bean
    fun upstreamClient(webClientBuilder: WebClient.Builder,
                       upstreamWebClientConfiguration: UpstreamConfiguration
    ) : WebClient {
        return webClientBuilder
            .baseUrl(upstreamWebClientConfiguration.url)
            .build()
    }
}