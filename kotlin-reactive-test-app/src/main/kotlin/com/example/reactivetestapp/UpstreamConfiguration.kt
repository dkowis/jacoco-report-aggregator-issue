package com.example.reactivetestapp

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "upstream")
data class UpstreamConfiguration
    (
    val url: String,
) {
}