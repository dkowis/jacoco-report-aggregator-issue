package com.example.servlettestapp

import org.springframework.boot.web.client.RestTemplateBuilder
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.client.RestTemplate

@Configuration
class RestTemplateConfiguration {

    //NOTE: must use a RestTemplateBuilder to enable the auto magic.
    @Bean
    fun restTemplate(builder: RestTemplateBuilder, upstreamConfiguration: UpstreamConfiguration): RestTemplate {
        return builder
            .rootUri(upstreamConfiguration.url)
            .build()
    }
}