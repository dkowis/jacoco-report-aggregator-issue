import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    id("org.springframework.boot") apply true
    jacoco
    kotlin("jvm") version "1.9.24"
    kotlin("plugin.spring") version "1.9.24"
}

java {
  toolchain {
    languageVersion.set(JavaLanguageVersion.of(17))
  }
}

version = findProperty("version") as String
group = "com.example.ea"

repositories {
  mavenCentral() 
}

tasks.withType<KotlinCompile> {
    kotlinOptions {
        freeCompilerArgs += "-Xjsr305=strict"
        jvmTarget = "17"
    }
}

tasks.named<Test>("test") {
    useJUnitPlatform() {
    }
    systemProperty("junit.jupiter.execution.parallel.enabled", "false")
    systemProperty("junit.jupiter.execution.parallel.mode.default", "same_thread")
    testLogging {
        showStandardStreams = true
    }
}

tasks.test {
    finalizedBy(tasks.jacocoTestReport) // report is always generated after tests run
}
tasks.jacocoTestReport {
    dependsOn(tasks.test) // tests are required to run before generating the report
    reports {
        xml.required.set(false)
        csv.required.set(false)
    }
}



dependencies {
    implementation(platform(org.springframework.boot.gradle.plugin.SpringBootPlugin.BOM_COORDINATES))
    annotationProcessor(platform(org.springframework.boot.gradle.plugin.SpringBootPlugin.BOM_COORDINATES))

    implementation(project(":example-reactive-tracing-correlation-id"))

    annotationProcessor("org.springframework.boot:spring-boot-configuration-processor")
    implementation("org.springframework.boot:spring-boot-starter-actuator")

    implementation("io.micrometer:micrometer-tracing-bridge-otel")
    // I shouldn't need this, looking at the PICS stuff
    //implementation("io.opentelemetry:opentelemetry-exporter-otlp") //I think I need this?
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    implementation("org.springframework.boot:spring-boot-starter-webflux")
    implementation("org.checkerframework:checker-qual:3.45.0")

    implementation("javax.annotation:javax.annotation-api:1.3.2")
    implementation("org.slf4j:slf4j-api:2.0.13")

    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation("org.assertj:assertj-core")
    testImplementation("ch.qos.logback:logback-classic:1.5.6")

    testImplementation("com.squareup.okhttp3:mockwebserver:4.12.0")

    implementation("io.github.oshai:kotlin-logging-jvm:5.1.4")
    implementation("org.slf4j:slf4j-api:2.0.13")

}

// Just skip the publishing tasks for the aggregation project, it's not a shared artifact
tasks.withType<PublishToMavenRepository>().configureEach {
    val predicate = provider {
        false
    }
    onlyIf("not publishing the webclient example project") {
        predicate.get()
    }
}
tasks.withType<PublishToMavenLocal>().configureEach {
    val predicate = provider {
        false
    }
    onlyIf("not publishing the webclient example project") {
        predicate.get()
    }
}

