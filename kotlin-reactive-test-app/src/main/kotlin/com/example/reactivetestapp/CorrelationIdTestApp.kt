package com.example.reactivetestapp

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class CorrelationIdTestApp {
}

fun main(args: Array<String>) {
    runApplication<CorrelationIdTestApp>(*args)
}