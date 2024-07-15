plugins {
    base
    `jacoco-report-aggregation`
    id("org.barfuin.gradle.jacocolog") version "3.1.0"
}

repositories {
    mavenCentral()
}

dependencies {
    jacocoAggregation(project(":kotlin-synchronous-test-app"))
    jacocoAggregation(project(":kotlin-reactive-test-app"))
}

reporting {
    reports {
        val testCodeCoverageReport by creating(JacocoCoverageReport::class) {
            testType.set(TestSuiteType.UNIT_TEST)
        }
    }
}

tasks.check {
    dependsOn(tasks.named<JacocoReport>("testCodeCoverageReport"))
}

// Just skip the publishing tasks for the aggregation project, it's not a shared artifact
tasks.withType<PublishToMavenRepository>().configureEach {
    val predicate = provider {
        false
    }
    onlyIf("not publishing the aggregation project") {
        predicate.get()
    }
}
tasks.withType<PublishToMavenLocal>().configureEach {
    val predicate = provider {
        false
    }
    onlyIf("not publishing the aggregation project") {
        predicate.get()
    }
}

