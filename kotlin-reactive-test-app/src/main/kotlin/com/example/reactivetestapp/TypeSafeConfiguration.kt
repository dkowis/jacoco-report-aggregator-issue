package com.example.reactivetestapp

import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Configuration

@Configuration
@EnableConfigurationProperties(UpstreamConfiguration::class)
class TypeSafeConfiguration {
}