rootProject.name = "spring-boot-auto-correlation-id"

include(
    "reactive-lib",
    "synchronous-lib",
    "lib",
    "jacoco-aggregator",
    "kotlin-reactive-test-app",
    "kotlin-synchronous-test-app",
)

//Give the projects some nice names
project(":reactive-lib").name = "example-reactive-tracing-correlation-id"
project(":synchronous-lib").name = "example-synchronous-tracing-correlation-id"
project(":lib").name = "example-correlation-id-config"

pluginManagement {
    val springBootVersion: String by settings
    plugins {
        id("org.springframework.boot") version springBootVersion
    }
}


