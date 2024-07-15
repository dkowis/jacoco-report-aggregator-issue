package com.example.servlettestapp

import io.github.oshai.kotlinlogging.KotlinLogging
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import org.assertj.core.api.AbstractAssert
import org.assertj.core.api.SoftAssertions
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.actuate.observability.AutoConfigureObservability
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.springframework.test.web.reactive.server.WebTestClient
import java.nio.ByteBuffer
import java.util.*
import java.util.concurrent.TimeUnit
import java.util.stream.Stream

/**
 * Ordering doesn't matter, but parallelism matters in this test,
 * in that I need to reun only one method at a time, otherwise responses get weird from my mockwebserver
 */
@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    classes = [ServletTestApp::class]
)
@AutoConfigureWebTestClient
@ActiveProfiles("test")
@AutoConfigureObservability
class SynchronousParameterizedCorrelationIdPresenceTest {
    private val logger = KotlinLogging.logger {}

    @Autowired
    private lateinit var webTestClient: WebTestClient

    companion object {
        private lateinit var mockWebServer: MockWebServer;

        @BeforeAll
        @JvmStatic
        internal fun beforeAll() {
            mockWebServer = MockWebServer()
            mockWebServer.start();

            //let's use a dispatcher and set that up
            val dispatcher = object : Dispatcher() {
                private val logger = KotlinLogging.logger {}
                override fun dispatch(request: RecordedRequest): MockResponse {
                    logger.debug { "DISPATCHING: ${request.path}" }
                    val path = request.path!!
                    val method = path.split("/").last()

                    return MockResponse().setResponseCode(200)
                        .setHeader("x-derp-header", "LookItsADerp")
                        .setBody("""{"testMethod": "${method}"} """)
                }
            }

            mockWebServer.dispatcher = dispatcher
        }

        @AfterAll
        @JvmStatic
        internal fun afterAll() {
            mockWebServer.shutdown()
        }

        @DynamicPropertySource
        @JvmStatic
        internal fun backendProperties(registry: DynamicPropertyRegistry) {
            registry.add("upstream.url") { mockWebServer.url("/").toString() }
        }

        @JvmStatic
        fun testPaths(): Stream<Arguments> {
            return Stream.of(
                Arguments.of("test"),
                Arguments.of("async"),
                Arguments.of("executor"),
                Arguments.of("restClient")
            )
        }
    }

    @ParameterizedTest
    @MethodSource("testPaths")
    fun createsACorrelationIDandOtelTraceHeaderWhenNone(type: String) {
        val response = webTestClient.get()
            .uri("/$type/createsACorrelationIDandOtelTraceHeaderWhenNone")
            .exchange()

        val request = mockWebServer.takeRequest(1, TimeUnit.SECONDS)!!

        //Tracing Header Value
        val tracingHeaderValue = request.getHeader("traceparent")

        //The part of traceparent that is used in the correlation id isthe 32hexdigit, just the trace ID
        // 16 Byte UUID
        //Generate it using a UUID and then to byte array and then to hex
        // https://www.baeldung.com/java-byte-array-to-uuid
        val traceparentHex = tracingHeaderValue!!.split("-")[1];

        logger.debug { "Request Headers Received: \n${request.headers}\n-----" }

        val softly = SoftAssertions()

        softly.assertThat(request.headers["X-EXAMPLE-CORRELATIONID"]).isEqualTo(traceparentHex)
        softly.assertThat(request.headers["X-CORRELATION-ID"]).isEqualTo(traceparentHex)
        softly.assertThat(request.headers["CORRELATION-ID"]).isEqualTo(traceparentHex)

        response
            .expectStatus().is2xxSuccessful
            .expectBody()
            .json("""{"testMethod":  "createsACorrelationIDandOtelTraceHeaderWhenNone"}""")

        softly.assertAll()
    }

    @ParameterizedTest
    @MethodSource("testPaths")
    fun createsACorrelationIDWhenOnlyOtelIsPresent(type: String) {
        // Invent my own traceparent thing, including the parent ID
        //Tracing Header Value
        val traceparent =
            "00-${convertUUIDToHexString(UUID.randomUUID())}-${createHalfUUIDHexString(UUID.randomUUID())}-00"
        logger.debug { "TRACE PARENT GENERATED HEADER: ${traceparent}" }

        //The part of traceparent that is used in the correlation id isthe 32hexdigit, just the trace ID
        // 16 Byte UUID
        //Generate it using a UUID and then to byte array and then to hex
        // https://www.baeldung.com/java-byte-array-to-uuid
        val traceparentTracingId = traceparent.split("-")[1];

        val response = webTestClient.get()
            .uri("/$type/createsACorrelationIDWhenOnlyOtelIsPresent")
            .header("traceparent", traceparent)
            .exchange()

        val request = mockWebServer.takeRequest(1, TimeUnit.SECONDS)!!

        logger.debug { "Request Headers Received: \n${request.headers}\n-----" }

        val softly = SoftAssertions()

        softly.assertThat(request.headers["X-EXAMPLE-CORRELATIONID"]).isEqualTo(traceparentTracingId)
        softly.assertThat(request.headers["X-CORRELATION-ID"]).isEqualTo(traceparentTracingId)
        softly.assertThat(request.headers["CORRELATION-ID"]).isEqualTo(traceparentTracingId)

        response
            .expectStatus().is2xxSuccessful
            .expectBody()
            .json("""{"testMethod":  "createsACorrelationIDWhenOnlyOtelIsPresent"}""")

        softly.assertAll()
    }

    @ParameterizedTest
    @MethodSource("testPaths")
    fun createsAnOtelTraceHeaderWhenOnlyOneCorrelationIdIsPresent(type: String) {
        val correlationId = UUID.randomUUID().toString()

        val response = webTestClient.get()
            .uri("/$type/createsAnOtelTraceHeaderWhenOnlyOneCorrelationIdIsPresent")
            .header("correlation-id", correlationId)
            .exchange()

        //Why is this throwing a null pointer?
        val request = mockWebServer.takeRequest(1, TimeUnit.SECONDS)!!

        logger.debug { "Request Headers Received: \n${request.headers}\n-----" }

        //All the configured correlation ID headers should always be the same, regardless of which is set
        val softly = SoftAssertions()

        softly.assertThat(request.headers["X-EXAMPLE-CORRELATIONID"]).isEqualTo(correlationId)
        softly.assertThat(request.headers["X-CORRELATION-ID"]).isEqualTo(correlationId)
        softly.assertThat(request.headers["CORRELATION-ID"]).isEqualTo(correlationId)

        //It'd be really cool if I could turn the UUID into a traceparent ID
        softly.assertThat(request.headers["traceparent"]).isNotEmpty()

        response
            .expectStatus().is2xxSuccessful
            .expectBody()
            .json("""{"testMethod":  "createsAnOtelTraceHeaderWhenOnlyOneCorrelationIdIsPresent"}""")

        softly.assertAll()
    }

    @ParameterizedTest
    @MethodSource("testPaths")
    fun persistsBothHeadersWhenPresentAndTheSame(type: String) {
        // Invent my own traceparent thing, including the parent ID
        //Tracing Header Value
        val traceparent =
            "00-${convertUUIDToHexString(UUID.randomUUID())}-${createHalfUUIDHexString(UUID.randomUUID())}-00"
        logger.debug { "persistsBothHeadersWhenPresentAndTheSame: TRACE PARENT GENERATED HEADER: ${traceparent}" }

        //The part of traceparent that is used in the correlation id isthe 32hexdigit, just the trace ID
        // 16 Byte UUID
        //Generate it using a UUID and then to byte array and then to hex
        // https://www.baeldung.com/java-byte-array-to-uuid
        val traceparentTracingId = traceparent.split("-")[1];

        val response = webTestClient.get()
            .uri("/$type/persistsBothHeadersWhenPresentAndTheSame")
            .header("traceparent", traceparent)
            .header("X-example-correlationid", traceparentTracingId)
            .header("X-correlation-id", traceparentTracingId)
            .header("correlation-id", traceparentTracingId)
            .exchange()

        val request = mockWebServer.takeRequest(1, TimeUnit.SECONDS)!!

        logger.debug { "Request Headers Received: \n${request.headers}\n-----" }

        val softly = SoftAssertions()

        softly.assertThat(request.headers["X-EXAMPLE-CORRELATIONID"]).isEqualTo(traceparentTracingId)
        softly.assertThat(request.headers["X-CORRELATION-ID"]).isEqualTo(traceparentTracingId)
        softly.assertThat(request.headers["CORRELATION-ID"]).isEqualTo(traceparentTracingId)

        //Only part of the traceparent header is gonna match, the traceID part, not the span ID part
        TraceparentHeaderAssert.assertThat(request.headers["traceparent"]).hasTraceId(traceparentTracingId)

        response
            .expectStatus().is2xxSuccessful
            .expectBody()
            .json("""{"testMethod":  "persistsBothHeadersWhenPresentAndTheSame"}""")

        softly.assertAll()

    }

    @ParameterizedTest
    @MethodSource("testPaths")
    fun persistsBothHeadersWhenPresentAndDifferent(type:String) {
        // Invent my own traceparent thing, including the parent ID
        //Tracing Header Value
        val traceparent =
            "00-${convertUUIDToHexString(UUID.randomUUID())}-${createHalfUUIDHexString(UUID.randomUUID())}-00"
        logger.debug { "persistsBothHeadersWhenPresentAndDifferent: TRACE PARENT GENERATED HEADER: ${traceparent}" }

        val correlationId = UUID.randomUUID().toString()

        //The part of traceparent that is used in the correlation id isthe 32hexdigit, just the trace ID
        // 16 Byte UUID
        //Generate it using a UUID and then to byte array and then to hex
        // https://www.baeldung.com/java-byte-array-to-uuid
        val traceparentTracingId = traceparent.split("-")[1];

        val response = webTestClient.get()
            .uri("/$type/persistsBothHeadersWhenPresentAndDifferent")
            .header("traceparent", traceparent)
            .header("X-example-correlationid", correlationId)
            .header("X-correlation-id", correlationId)
            .header("correlation-id", correlationId)
            .exchange()

        val request = mockWebServer.takeRequest(1, TimeUnit.SECONDS)!!

        logger.debug { "Request Headers Received: \n${request.headers}\n-----" }

        val softly = SoftAssertions()

        softly.assertThat(request.headers["X-EXAMPLE-CORRELATIONID"]).isEqualTo(correlationId)
        softly.assertThat(request.headers["X-CORRELATION-ID"]).isEqualTo(correlationId)
        softly.assertThat(request.headers["CORRELATION-ID"]).isEqualTo(correlationId)

        TraceparentHeaderAssert.assertThat(request.headers["traceparent"]).hasTraceId(traceparentTracingId)

        response
            .expectStatus().is2xxSuccessful
            .expectBody()
            .json("""{"testMethod":  "persistsBothHeadersWhenPresentAndDifferent"}""")

        softly.assertAll()
    }

    class TraceparentHeaderAssert(actual: String?) :
        AbstractAssert<TraceparentHeaderAssert, String>(
            actual,
            TraceparentHeaderAssert::class.java
        ) {

        companion object {
            @JvmStatic
            fun assertThat(actual: String?): TraceparentHeaderAssert {
                return TraceparentHeaderAssert(actual)
            }
        }

        fun hasTraceId(traceId: String?) : TraceparentHeaderAssert {
            actual.isNotBlank()

            val actualTraceId = actual.split("-")[1]
            if(!Objects.equals(actualTraceId, traceId)) {
                failWithMessage("Expected traceID to be <%s> but was <%s>", traceId, actualTraceId)
            }
            return this
        }
    }


    fun convertUUIDToHexString(uuid: UUID): String {
        val bb: ByteBuffer = ByteBuffer.wrap(ByteArray(16))
        bb.putLong(uuid.mostSignificantBits)
        bb.putLong(uuid.leastSignificantBits)
        val sb = StringBuilder()
        for (b in bb.array()) {
            sb.append(String.format("%02x", b))
        }

        return sb.toString()
    }

    fun createHalfUUIDHexString(uuid: UUID): String {
        val bb: ByteBuffer = ByteBuffer.wrap(ByteArray(8))
        bb.putLong(uuid.mostSignificantBits)
        val sb = StringBuilder()
        for (b in bb.array()) {
            sb.append(String.format("%02x", b))
        }
        return sb.toString()
    }

}