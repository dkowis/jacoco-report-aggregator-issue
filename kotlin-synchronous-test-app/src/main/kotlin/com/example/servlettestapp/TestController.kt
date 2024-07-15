package com.example.servlettestapp

import io.github.oshai.kotlinlogging.KotlinLogging
import io.micrometer.observation.Observation
import io.micrometer.observation.ObservationRegistry
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.client.RestClient
import org.springframework.web.client.RestTemplate
import java.util.concurrent.Callable
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executor
import java.util.function.Supplier

@RestController
class TestController(
    private val restTemplate: RestTemplate,
    private val observationRegistry: ObservationRegistry,
    private val asyncExecutor: Executor,
    private val restClient: RestClient,
) {
    private val logger = KotlinLogging.logger { }

    @GetMapping("/test/{methodName}")
    fun echoTest(@PathVariable methodName: String): String {
        logger.info { "/test/${methodName} got called" }
        val response = restTemplate.getForEntity("/${methodName}", String::class.java)
        logger.info { "response: $response" }
        return response.body!!
    }

    //Lets try out some async goodies as per this async thingy
    // https://docs.spring.io/spring-framework/reference/web/webmvc/mvc-ann-async.html
    @GetMapping("/async/{methodName}")
    fun asyncTest(@PathVariable methodName: String): Callable<String> {
        val observation = Observation.createNotStarted("async-operation", observationRegistry)
        //You have to create an observation for the thing, so it knows that the stuff happening in another thread
        // is another span. Then everything works.
        val doer = Callable<String> {
            observation.observe(Supplier {
                logger.info { "/async/${methodName} got called" }
                val response = restTemplate.getForEntity("/${methodName}", String::class.java)
                logger.info { "response: $response" }
                response.body!!
            })
        }
        return doer
    }

    // Another Async test, but using a completable Future and an external Executor.
    @GetMapping("/executor/{methodName}")
    fun executorTest(@PathVariable methodName: String): CompletableFuture<String> {

        val observation = Observation.createNotStarted("executor-operation", observationRegistry)
        return CompletableFuture.supplyAsync({
            observation.observe(Supplier {
                logger.info { "/executor/${methodName} got called" }
                try {
                    Thread.sleep(100)
                } catch (e: InterruptedException) {
                    throw RuntimeException(e)
                }
                val response = restTemplate.getForEntity("/${methodName}", String::class.java)
                logger.info { "response: $response" }
                response.body!!
            })
        }, asyncExecutor)
    }

    @GetMapping("/restClient/{methodName}")
    fun restClientTest(@PathVariable methodName: String): String {
        logger.info { "/restClient/${methodName} got called" }
        val response = restClient.get()
            .uri("/${methodName}")
            .retrieve()
        logger.info { "response: $response" }
        return response.body(String::class.java)!!
    }
}