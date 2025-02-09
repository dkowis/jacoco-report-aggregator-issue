package com.example.servlettestapp

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.scheduling.annotation.EnableAsync

@SpringBootApplication
@EnableAsync
class ServletTestApp

fun main(args:Array<String>) {
    runApplication<ServletTestApp>(*args)
}