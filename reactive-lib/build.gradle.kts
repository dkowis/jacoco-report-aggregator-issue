
plugins {
    `java-library`
    id("org.springframework.boot") apply false
    jacoco
    `gitlab-repo`
}

java {
  toolchain {
    languageVersion.set(JavaLanguageVersion.of(17))
  }
}

version = findProperty("version") as String
group = "com.example.ea.correlationid"

repositories {
  mavenCentral() 
}

tasks.named<Test>("test") {
    useJUnitPlatform() {
    }
    systemProperty("junit.jupiter.execution.parallel.enabled", "true")
    systemProperty("junit.jupiter.execution.parallel.mode.default", "concurrent")
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

    annotationProcessor("org.springframework.boot:spring-boot-configuration-processor")
    implementation(project(":example-correlation-id-config"))
    implementation("org.springframework.boot:spring-boot-autoconfigure")
    implementation("org.checkerframework:checker-qual:3.45.0")
    implementation("org.projectlombok:lombok")
    annotationProcessor("org.projectlombok:lombok")

    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("io.micrometer:micrometer-tracing-bridge-otel")
    implementation("org.springframework.boot:spring-boot-starter-webflux")

    implementation("javax.annotation:javax.annotation-api:1.3.2")
    implementation("org.slf4j:slf4j-api:2.0.13")

    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation("org.assertj:assertj-core")
    testImplementation("ch.qos.logback:logback-classic:1.5.6")

}
