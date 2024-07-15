package com.example.reactivetestapp

import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.bodyToMono
import reactor.core.publisher.Mono

@RestController
class TestingController(
    private val webClient: WebClient
) {
    private val logger = KotlinLogging.logger {}

    @GetMapping("/test/{methodName}")
    fun echoTest(@PathVariable methodName: String): Mono<String> {
        logger.info { "/test/${methodName} got called" }
        //Call the upstream server and return the body, using a dispatcher to make sure things line up, because they don't...
        return webClient.get()
            .uri("/${methodName}")
            .accept(MediaType.APPLICATION_JSON)
            .exchangeToMono { clientResponse ->
                clientResponse.bodyToMono<String>()
            }
    }
}